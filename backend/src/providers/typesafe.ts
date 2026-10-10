import type { NotificationEvent, RouteDecision } from "../types.js";

interface TypeSafeAnswer {
  type: string;
  choice?: string;
  confidence?: number;
  noul?: number;
}

export async function routeWithTypeSafe(event: NotificationEvent): Promise<RouteDecision> {
  const apiKey = process.env.TYPESAFE_API_KEY;
  if (!apiKey) throw new Error("TYPESAFE_API_KEY is not configured");
  const response = await fetch("https://api.typesafe.ai/v1/systemone", {
    method: "POST",
    signal: AbortSignal.timeout(2_000),
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model: "jev-latest",
      state: {
        source: event.sourceLabel,
        title: event.title,
        body: event.body,
        category: event.category,
      },
      questions: {
        domain: {
          type: "choice",
          instructions: "Which notification domain best describes this state?",
          criteria: {
            commerce: "A product, merchant, price, promotion, or purchase",
            travel: "A flight, hotel, route, booking, delay, or trip",
            critical: "An OTP, alarm, emergency, security event, or direct conversation",
            other: "None of the other options",
          },
        },
        claims_time_pressure: {
          type: "noul",
          instructions: "The notification explicitly claims that the user has little time to act.",
        },
        verification_is_useful: {
          type: "noul",
          instructions: "Current public search results could materially help evaluate the notification's claim.",
        },
      },
    }),
  });
  if (!response.ok) throw new Error(`TypeSafe returned ${response.status}`);
  const body = (await response.json()) as { model: string; answers: Record<string, TypeSafeAnswer> };
  const domain = body.answers.domain;
  const domainChoice = domain?.choice;
  if (!domainChoice || !["commerce", "travel", "critical", "other"].includes(domainChoice)) {
    throw new Error("TypeSafe returned an invalid domain");
  }
  return {
    domain: domainChoice as RouteDecision["domain"],
    domainConfidence: domain.confidence ?? 0,
    claimsTimePressure: body.answers.claims_time_pressure?.noul ?? 0,
    verificationUseful: body.answers.verification_is_useful?.noul ?? 0,
    provider: body.model,
  };
}

