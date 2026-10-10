import type { EvidenceItem, EvidencePlan } from "../types.js";

export interface SearchEvidence {
  items: EvidenceItem[];
  prices: number[];
  alternatives: number;
  provider: string;
}

function numericPrice(value: unknown): number | null {
  if (typeof value === "number") return value;
  if (typeof value !== "string") return null;
  const number = Number(value.replace(/[^0-9.]/g, ""));
  return Number.isFinite(number) && number > 0 ? number : null;
}

async function exactProductId(
  searchQuery: string,
  candidates: Array<{ title?: string; product_id?: string }>,
): Promise<string | null> {
  const usable = candidates.filter((candidate) => candidate.product_id && candidate.title).slice(0, 12);
  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey || !usable.length) return null;
  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    signal: AbortSignal.timeout(5_000),
    headers: { Authorization: `Bearer ${apiKey}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      model: process.env.OPENAI_ROUTER_MODEL ?? "gpt-6-luna",
      input: [
        {
          role: "system",
          content: "Select the candidate that is the same exact product and model as the query. Variants such as color are acceptable; accessories, earlier models, related products, and bundles with a materially different model are not. Return -1 when no candidate is a reliable exact match.",
        },
        { role: "user", content: JSON.stringify({ searchQuery, candidates: usable.map((candidate, index) => ({ index, title: candidate.title })) }) },
      ],
      text: { format: { type: "json_schema", name: "product_match", strict: true, schema: {
        type: "object", additionalProperties: false,
        properties: { selectedIndex: { type: "integer", minimum: -1, maximum: usable.length - 1 } },
        required: ["selectedIndex"],
      } } },
    }),
  });
  if (!response.ok) return null;
  const payload = (await response.json()) as { output?: Array<{ content?: Array<{ type?: string; text?: string }> }> };
  const text = payload.output?.flatMap((item) => item.content ?? []).find((item) => item.type === "output_text")?.text;
  if (!text) return null;
  const selectedIndex = (JSON.parse(text) as { selectedIndex: number }).selectedIndex;
  return selectedIndex >= 0 ? usable[selectedIndex]?.product_id ?? null : null;
}

export async function collectEvidence(plan: EvidencePlan): Promise<SearchEvidence> {
  const apiKey = process.env.SERPAPI_KEY;
  if (!apiKey) throw new Error("SERPAPI_KEY is not configured");
  if (plan.domain === "commerce") return shoppingEvidence(plan, apiKey);
  return flightEvidence(plan, apiKey);
}

async function shoppingEvidence(plan: EvidencePlan, apiKey: string): Promise<SearchEvidence> {
  const url = new URL("https://serpapi.com/search.json");
  url.search = new URLSearchParams({
    engine: "google_shopping",
    q: plan.searchQuery,
    gl: "in",
    hl: "en",
    api_key: apiKey,
  }).toString();
  const response = await fetch(url, { signal: AbortSignal.timeout(12_000) });
  if (!response.ok) throw new Error(`SerpApi Shopping returned ${response.status}`);
  const body = (await response.json()) as {
    shopping_results?: Array<{ title?: string; price?: string; extracted_price?: number; source?: string; product_link?: string; product_id?: string }>;
  };
  const rawResults = (body.shopping_results ?? []).slice(0, 40);
  const productId = await exactProductId(plan.searchQuery, rawResults);
  let results = productId ? rawResults.filter((item) => item.product_id === productId) : [];
  let verifiedTitle: string | null = null;
  if (productId) {
    try {
      const productUrl = new URL("https://serpapi.com/search.json");
      productUrl.search = new URLSearchParams({
        engine: "google_product",
        product_id: productId,
        offer_view: "true",
        gl: "in",
        hl: "en",
        api_key: apiKey,
      }).toString();
      const productResponse = await fetch(productUrl, { signal: AbortSignal.timeout(7_000) });
      if (productResponse.ok) {
        const productBody = (await productResponse.json()) as {
          product_results?: { title?: string; stores?: Array<{ name?: string; title?: string; price?: string; extracted_price?: number; link?: string }> };
        };
        const stores = productBody.product_results?.stores ?? [];
        verifiedTitle = productBody.product_results?.title ?? null;
        if (stores.length) {
          results = stores.map((store) => ({
            title: store.title ?? plan.searchQuery,
            ...(store.price !== undefined ? { price: store.price } : {}),
            ...(store.extracted_price !== undefined ? { extracted_price: store.extracted_price } : {}),
            ...(store.name !== undefined ? { source: store.name } : {}),
            ...(store.link !== undefined ? { product_link: store.link } : {}),
            product_id: productId,
          }));
        }
      }
    } catch {
      // The exact Shopping matches remain valid if the richer offer view times out.
    }
  }
  const prices = results.map((item) => numericPrice(item.extracted_price ?? item.price)).filter((price): price is number => price !== null);
  const sorted = [...prices].sort((a, b) => a - b);
  const lowest = sorted[0];
  const median = sorted.length ? sorted[Math.floor(sorted.length / 2)] : undefined;
  const merchants = new Set(results.map((item) => item.source).filter(Boolean)).size;
  const items: EvidenceItem[] = [];
  if (verifiedTitle) items.push({ label: "Verified product", detail: verifiedTitle, source: "Google Product" });
  if (lowest !== undefined) items.push({ label: "Lowest current price", detail: `₹${Math.round(lowest).toLocaleString("en-IN")} across ${merchants} merchants`, source: "Google Shopping" });
  if (median !== undefined) items.push({ label: "Market midpoint", detail: `₹${Math.round(median).toLocaleString("en-IN")} from current listings`, source: "Google Shopping" });
  if (!productId) items.push({ label: "Product match", detail: "No exact product could be verified from the current results.", source: "Google Shopping" });
  return { items, prices, alternatives: results.length, provider: productId ? "serpapi/google_product" : "serpapi/google_shopping" };
}

async function flightEvidence(plan: EvidencePlan, apiKey: string): Promise<SearchEvidence> {
  if (!plan.originAirport || !plan.destinationAirport) {
    return { items: [], prices: [], alternatives: 0, provider: "serpapi/google_flights" };
  }
  if (!plan.departureDate) return flightDealEvidence(plan, apiKey);
  const url = new URL("https://serpapi.com/search.json");
  url.search = new URLSearchParams({
    engine: "google_flights",
    departure_id: plan.originAirport,
    arrival_id: plan.destinationAirport,
    outbound_date: plan.departureDate,
    type: "2",
    currency: "INR",
    hl: "en",
    api_key: apiKey,
  }).toString();
  const statusUrl = new URL("https://serpapi.com/search.json");
  statusUrl.search = new URLSearchParams({
    engine: "google",
    q: `${plan.flightNumber ?? `${plan.originAirport} ${plan.destinationAirport}`} flight status ${plan.departureDate}`,
    gl: "in",
    hl: "en",
    api_key: apiKey,
  }).toString();
  const [response, statusResponse] = await Promise.all([
    fetch(url, { signal: AbortSignal.timeout(12_000) }),
    fetch(statusUrl, { signal: AbortSignal.timeout(12_000) }),
  ]);
  if (!response.ok) throw new Error(`SerpApi Flights returned ${response.status}`);
  const body = (await response.json()) as {
    best_flights?: Array<{ price?: number; flights?: Array<{ departure_airport?: { time?: string }; airline?: string }> }>;
    other_flights?: Array<{ price?: number; flights?: Array<{ departure_airport?: { time?: string }; airline?: string }> }>;
    price_insights?: { lowest_price?: number; price_level?: string; typical_price_range?: number[] };
  };
  const statusBody = statusResponse.ok ? await statusResponse.json() as {
    organic_results?: Array<{ title?: string; snippet?: string; source?: string; link?: string; date?: string }>;
  } : {};
  const flights = [...(body.best_flights ?? []), ...(body.other_flights ?? [])].slice(0, 20);
  const prices = flights.map((flight) => numericPrice(flight.price)).filter((price): price is number => price !== null);
  const first = flights[0];
  const items: EvidenceItem[] = [];
  if (first) {
    const leg = first.flights?.[0];
    items.push({
      label: "Current alternatives",
      detail: `${flights.length} options found${leg?.departure_airport?.time ? `; next listed departure ${leg.departure_airport.time}` : ""}`,
      source: "Google Flights",
    });
  }
  if (prices.length) items.push({ label: "Lowest listed fare", detail: `₹${Math.round(Math.min(...prices)).toLocaleString("en-IN")}`, source: "Google Flights" });
  if (body.price_insights?.typical_price_range?.length === 2
    && body.price_insights.typical_price_range[0] !== undefined
    && body.price_insights.typical_price_range[1] !== undefined) {
    const [low, high] = body.price_insights.typical_price_range;
    items.push({
      label: "Typical fare range",
      detail: `₹${Math.round(low).toLocaleString("en-IN")}–₹${Math.round(high).toLocaleString("en-IN")}`,
      source: "Google Flights",
    });
  }
  if (body.price_insights?.price_level) {
    items.push({
      label: "Current price level",
      detail: body.price_insights.price_level,
      source: "Google Flights",
    });
  }
  for (const result of (statusBody.organic_results ?? []).filter((item) => item.snippet).slice(0, 3)) {
    items.push({
      label: "Current status result",
      detail: `${result.title ?? "Flight status"}: ${result.snippet}`,
      source: result.source ?? "Google Search",
      ...(result.link ? { url: result.link } : {}),
    });
  }
  return { items, prices, alternatives: flights.length, provider: "serpapi/google_flights+google_search" };
}

async function flightDealEvidence(plan: EvidencePlan, apiKey: string): Promise<SearchEvidence> {
  const url = new URL("https://serpapi.com/search.json");
  url.search = new URLSearchParams({
    engine: "google_flights_deals",
    departure_id: plan.originAirport!,
    type: "1",
    travel_duration: "2",
    stops: "1",
    currency: "INR",
    gl: "in",
    hl: "en",
    api_key: apiKey,
  }).toString();
  const response = await fetch(url, { signal: AbortSignal.timeout(12_000) });
  if (!response.ok) throw new Error(`SerpApi Flight Deals returned ${response.status}`);
  const body = (await response.json()) as {
    deals?: Array<{
      name?: string;
      price?: number;
      average_price?: number;
      discount_percentage?: number;
      arrival_airport_code?: string;
      airline?: string;
      start_date?: string;
      end_date?: string;
    }>;
  };
  const target = (body.deals ?? []).find((deal) => deal.arrival_airport_code === plan.destinationAirport);
  if (!target) {
    return {
      items: [{ label: "Current deal", detail: "No matching live deal was found.", source: "Google Flights Deals" }],
      prices: [],
      alternatives: body.deals?.length ?? 0,
      provider: "serpapi/google_flights_deals",
    };
  }
  const items: EvidenceItem[] = [];
  if (target.price !== undefined) {
    items.push({ label: "Current deal fare", detail: `₹${Math.round(target.price).toLocaleString("en-IN")}`, source: "Google Flights Deals" });
  }
  if (target.average_price !== undefined) {
    items.push({ label: "Average fare", detail: `₹${Math.round(target.average_price).toLocaleString("en-IN")}`, source: "Google Flights Deals" });
  }
  if (target.discount_percentage !== undefined) {
    items.push({ label: "Current saving", detail: `${Math.round(target.discount_percentage)}% below average`, source: "Google Flights Deals" });
  }
  if (target.airline) items.push({ label: "Airline", detail: target.airline, source: "Google Flights Deals" });
  return {
    items,
    prices: [target.price, target.average_price].filter((price): price is number => typeof price === "number"),
    alternatives: body.deals?.length ?? 0,
    provider: "serpapi/google_flights_deals",
  };
}
