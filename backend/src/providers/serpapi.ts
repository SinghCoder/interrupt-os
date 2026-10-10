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
  const productId = rawResults.find((item) => item.product_id)?.product_id ?? null;
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
  if (!plan.originAirport || !plan.destinationAirport || !plan.departureDate) {
    return { items: [], prices: [], alternatives: 0, provider: "serpapi/google_flights" };
  }
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
