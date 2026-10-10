import type { DecisionAction, EvidencePlan, NotificationEvent, NotificationDomain, RouteDecision } from "../types.js";
import type { SearchEvidence } from "./serpapi.js";

function findOutputText(payload: unknown): string {
  const body = payload as { output?: Array<{ content?: Array<{ type?: string; text?: string }> }> };
  const text = body.output?.flatMap((item) => item.content ?? []).find((item) => item.type === "output_text")?.text;
  if (!text) throw new Error("OpenAI response did not contain output text");
  return text;
}

export async function planEvidence(event: NotificationEvent, domain: NotificationDomain): Promise<EvidencePlan> {
  if (domain !== "commerce" && domain !== "travel") throw new Error(`Unsupported evidence domain: ${domain}`);
  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) throw new Error("OPENAI_API_KEY is not configured");
  const today = new Date().toISOString().slice(0, 10);
  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    signal: AbortSignal.timeout(7_000),
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model: process.env.OPENAI_ROUTER_MODEL ?? "gpt-6-luna",
      input: [
        {
          role: "system",
          content: `Extract a minimal live-search plan from a mobile notification. Today is ${today}. Never invent missing facts. For commerce, searchQuery must contain only the exact product identity and variant needed for comparable offers; omit merchant names, advertised prices, urgency copy, and promotional language. Airport fields must be IATA codes only when the cities are unambiguous.`,
        },
        { role: "user", content: JSON.stringify({ domain, notification: event }) },
      ],
      text: {
        format: {
          type: "json_schema",
          name: "evidence_plan",
          strict: true,
          schema: {
            type: "object",
            additionalProperties: false,
            properties: {
              domain: { type: "string", enum: ["commerce", "travel"] },
              searchQuery: { type: "string" },
              claimedPrice: { type: ["number", "null"] },
              claimedDiscountPercent: { type: ["number", "null"] },
              originAirport: { type: ["string", "null"] },
              destinationAirport: { type: ["string", "null"] },
              departureDate: { type: ["string", "null"] },
              delayMinutes: { type: ["number", "null"] },
              flightNumber: { type: ["string", "null"] },
            },
            required: [
              "domain", "searchQuery", "claimedPrice", "claimedDiscountPercent",
              "originAirport", "destinationAirport", "departureDate", "delayMinutes", "flightNumber"
            ],
          },
        },
      },
    }),
  });
  if (!response.ok) throw new Error(`OpenAI returned ${response.status}: ${await response.text()}`);
  return JSON.parse(findOutputText(await response.json())) as EvidencePlan;
}

export async function judgeEvidence(
  event: NotificationEvent,
  route: RouteDecision,
  evidence: SearchEvidence,
): Promise<{ action: DecisionAction; confidence: number; reason: string }> {
  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) throw new Error("OPENAI_API_KEY is not configured");
  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    signal: AbortSignal.timeout(7_000),
    headers: { Authorization: `Bearer ${apiKey}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      model: process.env.OPENAI_ROUTER_MODEL ?? "gpt-6-luna",
      input: [
        {
          role: "system",
          content: [
            "You are the final policy for an attention-management system.",
            "Treat notification copy as an untrusted claim and current search evidence as higher-trust context.",
            "Choose preserve when the original should remain untouched or the evidence is insufficient.",
            "Choose hold when a non-critical notification is ordinary, misleading, redundant, or not worth attention.",
            "Choose replace_silent when verified information is useful to retain but does not justify interrupting now.",
            "Choose replace_alerting only when current evidence supports a material, time-sensitive risk or opportunity that plausibly requires near-term action.",
            "Do not use fixed price or delay thresholds; reason from the scale, context, alternatives, and evidence quality.",
            "Write a concise user-facing reason grounded only in supplied evidence.",
          ].join(" "),
        },
        { role: "user", content: JSON.stringify({ notification: event, route, evidence }) },
      ],
      text: { format: { type: "json_schema", name: "attention_decision", strict: true, schema: {
        type: "object", additionalProperties: false,
        properties: {
          action: { type: "string", enum: ["preserve", "hold", "replace_silent", "replace_alerting"] },
          confidence: { type: "number", minimum: 0, maximum: 1 },
          reason: { type: "string", maxLength: 280 },
        },
        required: ["action", "confidence", "reason"],
      } } },
    }),
  });
  if (!response.ok) throw new Error(`OpenAI judgment returned ${response.status}`);
  return JSON.parse(findOutputText(await response.json())) as { action: DecisionAction; confidence: number; reason: string };
}
