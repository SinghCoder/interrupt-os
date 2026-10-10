import { mkdir, readFile, writeFile } from "node:fs/promises";
import { spawnSync } from "node:child_process";

const scenario = process.argv.slice(2).find((argument) => !argument.startsWith("--")) ?? "commerce-drop";
const shouldPost = process.argv.includes("--post") || process.argv.includes("--post-latest");
const postLatest = process.argv.includes("--post-latest");
const apiKey = process.env.SERPAPI_KEY;
const openaiKey = process.env.OPENAI_API_KEY;
if (!apiKey) throw new Error("SERPAPI_KEY is required");
if (!openaiKey) throw new Error("OPENAI_API_KEY is required");

const inr = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 });

async function serp(params) {
  const url = new URL("https://serpapi.com/search.json");
  url.search = new URLSearchParams({ ...params, api_key: apiKey }).toString();
  const response = await fetch(url, { signal: AbortSignal.timeout(30_000) });
  if (!response.ok) throw new Error(`SerpApi ${params.engine} returned ${response.status}`);
  const body = await response.json();
  if (body.error) throw new Error(body.error);
  return body;
}

function outputText(payload) {
  return payload.output?.flatMap((item) => item.content ?? []).find((item) => item.type === "output_text")?.text;
}

async function modelJson(name, instructions, input, schema) {
  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    signal: AbortSignal.timeout(15_000),
    headers: { Authorization: `Bearer ${openaiKey}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      model: "gpt-6-luna",
      input: [{ role: "system", content: instructions }, { role: "user", content: JSON.stringify(input) }],
      text: { format: { type: "json_schema", name, strict: true, schema } },
    }),
  });
  if (!response.ok) throw new Error(`OpenAI returned ${response.status}`);
  const text = outputText(await response.json());
  if (!text) throw new Error("OpenAI returned no structured output");
  return JSON.parse(text);
}

async function commerce(kind) {
  const seed = await modelJson(
    "commerce_seed",
    "Choose one specific, widely sold consumer-product model in India that is likely to have multiple current merchants and Google price history. Vary the category and model across runs. Return only a precise shopping query, not a category or accessory.",
    { currentDate: new Date().toISOString().slice(0, 10) },
    { type: "object", additionalProperties: false, properties: { query: { type: "string" } }, required: ["query"] },
  );
  const shopping = await serp({ engine: "google_shopping", q: seed.query, gl: "in", hl: "en" });
  const candidates = [...new Map((shopping.shopping_results ?? []).filter((item) => item.product_id).map((item) => [item.product_id, item])).values()].slice(0, 6);
  let canonical = null;
  let details = null;
  for (const candidate of candidates) {
    try {
      const product = await serp({ engine: "google_product", product_id: candidate.product_id, offer_view: "true", gl: "in", hl: "en" });
      const candidateDetails = product.product_results ?? {};
      if ((candidateDetails.stores ?? []).length >= 2 && (candidateDetails.price_insights?.stores ?? []).length) {
        canonical = candidate;
        details = candidateDetails;
        break;
      }
    } catch {
      // Live catalogs change between Shopping and Product requests; try the next candidate.
    }
  }
  if (!canonical || !details) throw new Error("No live product with enough merchant and history evidence was found");
  const stores = (details.stores ?? []).filter((store) => store.extracted_price);
  if (stores.length < 2) throw new Error("Not enough live merchant offers were found");
  const histories = new Map((details.price_insights?.stores ?? []).map((store) => [store.name.toLowerCase(), store.prices ?? []]));
  const enriched = stores.map((store) => {
    const history = histories.get(store.name.toLowerCase()) ?? [];
    const prior = history.length > 1 ? history.at(-2)?.price : null;
    const current = store.extracted_price;
    const historicalPrices = history.map((point) => point.price).filter(Boolean).sort((a, b) => a - b);
    const historicalMedian = historicalPrices.length ? historicalPrices[Math.floor(historicalPrices.length / 2)] : null;
    const decrease = historicalMedian && current < historicalMedian ? (historicalMedian - current) / historicalMedian : 0;
    return { ...store, prior, historicalMedian, decrease };
  });
  const sorted = [...enriched].sort((a, b) => a.extracted_price - b.extracted_price);
  const marketMedian = sorted[Math.floor(sorted.length / 2)].extracted_price;
  const historicalWinner = [...enriched].sort((a, b) => b.decrease - a.decrease)[0];
  const offer = kind === "drop"
    ? (historicalWinner.decrease > 0 ? historicalWinner : sorted[0])
    : sorted[Math.floor(sorted.length / 2)];
  const historyClaim = offer.historicalMedian && offer.decrease > 0
    ? `${Math.round(offer.decrease * 100)}% below its recorded historical midpoint`
    : `the lowest of ${sorted.length} current verified merchant offers`;
  const title = kind === "drop"
    ? `Price watch: ${details.title} at ${inr.format(offer.extracted_price)}`
    : `${details.title} at ${inr.format(offer.extracted_price)}`;
  const body = kind === "drop"
    ? `Your saved item at ${offer.name} is ${historyClaim}. Offer available now.`
    : `${offer.name}: currently listed at ${inr.format(offer.extracted_price)}.`;
  return {
    scenario: kind === "drop" && offer.decrease > 0 ? "commerce-historical-advantage" : `commerce-${kind === "drop" ? "current-advantage" : "ordinary"}`,
    fixturePackage: "chat.harpinderjots.fixture.market",
    receiver: "chat.harpinderjots.fixture.market/.FixtureReceiver",
    title,
    body,
    liveBasis: {
      fetchedAt: new Date().toISOString(),
      product: details.title ?? canonical.title,
      merchant: offer.name,
      currentPrice: offer.extracted_price,
      priorRecordedPrice: offer.prior,
      historicalMedianPrice: offer.historicalMedian,
      currentMarketPrices: sorted.map((item) => item.extracted_price),
      productId: canonical.product_id,
      source: "SerpApi Google Shopping + Google Product",
    },
  };
}

async function travel(kind) {
  const today = new Date();
  const earliest = new Date(today.getTime() + 2 * 86_400_000).toISOString().slice(0, 10);
  const latest = new Date(today.getTime() + 10 * 86_400_000).toISOString().slice(0, 10);
  let route;
  let verifiedChange = null;
  if (kind === "urgent") {
    const currentDate = today.toISOString().slice(0, 10);
    const statusSearch = await serp({ engine: "google", q: `India flight cancelled delayed today live status ${currentDate}`, gl: "in", hl: "en" });
    const results = (statusSearch.organic_results ?? []).slice(0, 10).map((item, index) => ({
      index, title: item.title, snippet: item.snippet, source: item.source, date: item.date,
    }));
    verifiedChange = await modelJson(
      "verified_travel_change",
      "Extract one concrete current Indian flight disruption only when a supplied search result explicitly supports the flight number, route, and cancelled or delayed status. Do not infer or invent missing facts. Set usable false when the evidence is insufficient. When usable, write concise native-notification copy and cite the supporting result index.",
      { currentDate, results },
      { type: "object", additionalProperties: false, properties: {
        usable: { type: "boolean" }, flightNumber: { type: "string" }, airline: { type: "string" },
        from: { type: "string" }, to: { type: "string" }, fromName: { type: "string" }, toName: { type: "string" },
        departureDate: { type: "string" }, title: { type: "string" }, body: { type: "string" },
        changeSummary: { type: "string" }, sourceIndex: { type: "integer" },
      }, required: ["usable", "flightNumber", "airline", "from", "to", "fromName", "toName", "departureDate", "title", "body", "changeSummary", "sourceIndex"] },
    );
    if (!verifiedChange.usable || !results[verifiedChange.sourceIndex]) throw new Error("No independently supported current flight disruption was found");
    route = verifiedChange;
    verifiedChange.sourceEvidence = results[verifiedChange.sourceIndex];
  } else {
    route = await modelJson(
      "travel_seed",
      "Choose a plausible Indian domestic nonstop flight route and a date within the supplied range. Vary the route across runs. Use valid IATA codes and normal city names.",
      { earliest, latest },
      { type: "object", additionalProperties: false, properties: {
        from: { type: "string" }, to: { type: "string" }, fromName: { type: "string" }, toName: { type: "string" }, departureDate: { type: "string" },
      }, required: ["from", "to", "fromName", "toName", "departureDate"] },
    );
  }
  const result = await serp({
    engine: "google_flights", departure_id: route.from, arrival_id: route.to,
    outbound_date: route.departureDate, type: "2", currency: "INR", hl: "en",
  });
  const options = [...(result.best_flights ?? []), ...(result.other_flights ?? [])];
  const itinerary = options.find((option) => option.flights?.[0]?.departure_airport?.time) ?? options[0];
  if (!itinerary) throw new Error("No current flight itinerary was found");
  const leg = itinerary.flights[0];
  const change = verifiedChange ?? await modelJson(
    "travel_change",
    "Create a realistic fixture notification for the supplied real itinerary with a low-consequence operational change. Do not decide what Interrupt OS should do. Keep title and body concise, and include the exact route date so an independent system can search it.",
    { route, itinerary, currentAlternativeCount: options.length },
    { type: "object", additionalProperties: false, properties: {
      title: { type: "string" }, body: { type: "string" }, changeSummary: { type: "string" },
    }, required: ["title", "body", "changeSummary"] },
  );
  return {
    scenario: `travel-${kind}`,
    fixturePackage: "chat.harpinderjots.fixture.air",
    receiver: "chat.harpinderjots.fixture.air/.FixtureReceiver",
    title: change.title,
    body: change.body,
    liveBasis: {
      fetchedAt: new Date().toISOString(),
      route: `${route.from}-${route.to}`,
      departureDate: route.departureDate,
      airline: leg.airline,
      flightNumber: leg.flight_number,
      scheduledDeparture: leg.departure_airport.time,
      currentAlternativeCount: options.length,
      currentFares: options.map((option) => option.price).filter(Boolean),
      changeSummary: change.changeSummary,
      ...(verifiedChange ? { statusEvidence: verifiedChange.sourceEvidence } : {}),
      source: "SerpApi Google Flights",
    },
  };
}

async function travelDeal(kind) {
  if (kind === "exceptional") {
    const seed = await modelJson(
      "flight_deal_origin",
      "Choose one major Indian departure airport for a weekend flight-deal search. Vary the city across runs. Return a valid IATA code and normal city name.",
      { currentDate: new Date().toISOString().slice(0, 10) },
      { type: "object", additionalProperties: false, properties: { origin: { type: "string" }, city: { type: "string" } }, required: ["origin", "city"] },
    );
    const deals = await serp({ engine: "google_flights_deals", departure_id: seed.origin, type: "1", travel_duration: "2", stops: "1", currency: "INR", gl: "in", hl: "en" });
    const candidates = (deals.deals ?? []).filter((deal) => deal.price && deal.average_price && deal.discount_percentage && deal.arrival_airport_code);
    if (!candidates.length) throw new Error("No current flight deal with comparative evidence was found");
    const deal = [...candidates].sort((a, b) => b.discount_percentage - a.discount_percentage)[0];
    return {
      scenario: "travel-deal-exceptional",
      fixturePackage: "chat.harpinderjots.fixture.air",
      receiver: "chat.harpinderjots.fixture.air/.FixtureReceiver",
      title: `Price watch: ${deal.name} from ${inr.format(deal.price)}`,
      body: `Your saved ${seed.city} (${seed.origin}) to ${deal.name} (${deal.arrival_airport_code}) trip is now ${Math.round(deal.discount_percentage)}% below the current average of ${inr.format(deal.average_price)}.`,
      liveBasis: {
        fetchedAt: new Date().toISOString(), origin: seed.origin, destination: deal.arrival_airport_code,
        destinationName: deal.name, currentPrice: deal.price, averagePrice: deal.average_price,
        discountPercentage: deal.discount_percentage, airline: deal.airline,
        source: "SerpApi Google Flights Deals",
      },
    };
  }

  const today = new Date();
  const earliest = new Date(today.getTime() + 10 * 86_400_000).toISOString().slice(0, 10);
  const latest = new Date(today.getTime() + 24 * 86_400_000).toISOString().slice(0, 10);
  const seeds = await modelJson(
    "high_fare_candidates",
    "Return five varied Indian domestic nonstop route candidates and dates inside the supplied range. Use major and mid-size airports, valid IATA codes, normal city names, and different routes. Do not predict which fare will be high.",
    { earliest, latest },
    { type: "object", additionalProperties: false, properties: { routes: { type: "array", minItems: 5, maxItems: 5, items: { type: "object", additionalProperties: false, properties: { from: { type: "string" }, to: { type: "string" }, fromName: { type: "string" }, toName: { type: "string" }, departureDate: { type: "string" } }, required: ["from", "to", "fromName", "toName", "departureDate"] } } }, required: ["routes"] },
  );
  const inflated = [];
  for (const route of seeds.routes) {
    const result = await serp({ engine: "google_flights", departure_id: route.from, arrival_id: route.to, outbound_date: route.departureDate, type: "2", currency: "INR", hl: "en" });
    if (result.price_insights?.price_level !== "high" || !result.price_insights.lowest_price) continue;
    const itinerary = [...(result.best_flights ?? []), ...(result.other_flights ?? [])].find((option) => option.price);
    if (!itinerary) continue;
    const typical = result.price_insights.typical_price_range ?? [];
    inflated.push({ route, result, itinerary, typical, ratio: itinerary.price / (typical[1] ?? result.price_insights.lowest_price) });
  }
  const winner = inflated.sort((a, b) => b.ratio - a.ratio)[0];
  if (!winner) throw new Error("No current route with a high fare was found");
  const { route, result, itinerary, typical } = winner;
  return {
    scenario: "travel-fare-inflated",
    fixturePackage: "chat.harpinderjots.fixture.air",
    receiver: "chat.harpinderjots.fixture.air/.FixtureReceiver",
    title: `${route.fromName} to ${route.toName} at ${inr.format(itinerary.price)}`,
    body: `Book now for ${route.departureDate}. Limited seats on ${itinerary.flights?.[0]?.airline ?? "this route"}.`,
    liveBasis: {
      fetchedAt: new Date().toISOString(), route: `${route.from}-${route.to}`, departureDate: route.departureDate,
      currentPrice: itinerary.price, priceLevel: result.price_insights.price_level,
      typicalPriceRange: typical, alternativeCount: [...(result.best_flights ?? []), ...(result.other_flights ?? [])].length,
      source: "SerpApi Google Flights",
    },
  };
}

const payload = postLatest
  ? JSON.parse(await readFile("qa/output/latest-scenario.json", "utf8"))
  : scenario === "commerce-drop" ? await commerce("drop")
    : scenario === "commerce-ordinary" ? await commerce("ordinary")
      : scenario === "travel-deal-exceptional" ? await travelDeal("exceptional")
        : scenario === "travel-fare-inflated" ? await travelDeal("inflated")
      : scenario === "travel-urgent" ? await travel("urgent")
        : scenario === "travel-routine" ? await travel("routine")
          : (() => { throw new Error("Scenario must be commerce-drop, commerce-ordinary, travel-deal-exceptional, travel-fare-inflated, travel-urgent, or travel-routine"); })();

await mkdir("qa/output", { recursive: true });
await writeFile("qa/output/latest-scenario.json", `${JSON.stringify(payload, null, 2)}\n`);

if (shouldPost) {
  const quote = (value) => `'${String(value).replaceAll("'", `'\\''`)}'`;
  const command = [
    "am broadcast", "-n", quote(payload.receiver),
    "-a", quote("chat.harpinderjots.fixture.POST"),
    "--es", "title", quote(payload.title),
    "--es", "body", quote(payload.body),
  ].join(" ");
  const result = spawnSync("adb", ["shell", command], { encoding: "utf8" });
  if (result.status !== 0) throw new Error(result.stderr || result.stdout || "adb broadcast failed");
  payload.posted = true;
  payload.adbResult = result.stdout.trim();
}

console.log(JSON.stringify(payload, null, 2));
