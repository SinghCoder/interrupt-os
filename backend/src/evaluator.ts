import { randomUUID } from "node:crypto";
import { judgeEvidence, planEvidence } from "./providers/openai.js";
import { collectEvidence, type SearchEvidence } from "./providers/serpapi.js";
import { recentFeedback } from "./feedback.js";
import { routeWithTypeSafe } from "./providers/typesafe.js";
import type { DecisionResponse, EvidencePlan, NotificationEvent, RouteDecision } from "./types.js";

export async function evaluate(event: NotificationEvent, installationId?: string): Promise<DecisionResponse> {
  const started = Date.now();
  validateEvent(event);

  let route: RouteDecision;
  try {
    route = await routeWithTypeSafe(event);
  } catch {
    return response(started, "preserve", "other", 0, "Delivered unchanged because classification was unavailable.", [], { router: "fail-open" });
  }
  if (route.domain === "critical" || route.domain === "other") {
    return response(started, "preserve", route.domain, route.domainConfidence, "Delivered unchanged; live verification would not improve this notification.", [], { router: route.provider });
  }

  let plan: EvidencePlan;
  try {
    plan = await planEvidence(event, route.domain);
  } catch (error) {
    console.warn("evidence_plan_failed", {
      domain: route.domain,
      error: error instanceof Error ? error.message : "unknown",
    });
    return response(started, "preserve", route.domain, Math.min(route.domainConfidence, 0.7), "Delivered unchanged because current evidence was unavailable.", [], { router: route.provider });
  }
  let evidence: SearchEvidence;
  try {
    evidence = await collectEvidence(plan);
  } catch (error) {
    console.warn("evidence_search_failed", {
      domain: route.domain,
      error: error instanceof Error ? error.message : "unknown",
    });
    return response(started, "preserve", route.domain, Math.min(route.domainConfidence, 0.7), "Delivered unchanged because current evidence was unavailable.", [], { router: route.provider, planner: "openai" });
  }

  try {
    const preferences = installationId ? await recentFeedback(installationId).catch(() => []) : [];
    const judgment = await judgeEvidence(event, route, evidence, preferences);
    return response(
      started,
      judgment.action,
      route.domain,
      judgment.confidence,
      judgment.reason,
      evidence.items,
      { router: route.provider, planner: "openai", search: evidence.provider },
      judgment.summary,
      judgment.reviewRecommended,
    );
  } catch (error) {
    console.warn("evidence_judgment_failed", {
      domain: route.domain,
      error: error instanceof Error ? error.message : "unknown",
    });
    return response(started, "preserve", route.domain, Math.min(route.domainConfidence, 0.6), "Delivered unchanged because a reliable decision could not be completed.", evidence.items, { router: route.provider, planner: "openai", search: evidence.provider });
  }
}

function validateEvent(event: NotificationEvent): void {
  if (!event.eventId || !event.sourcePackage) throw new Error("eventId and sourcePackage are required");
  if ((!event.title && !event.body) || event.title.length > 500 || event.body.length > 2_000) throw new Error("notification content is invalid");
}

function response(
  started: number,
  action: DecisionResponse["action"],
  domain: DecisionResponse["domain"],
  confidence: number,
  reason: string,
  evidence: DecisionResponse["evidence"],
  providerTrace: DecisionResponse["providerTrace"],
  summary = reason,
  reviewRecommended = false,
): DecisionResponse {
  const expiresAt = new Date(Date.now() + 10 * 60_000).toISOString();
  return {
    decisionId: randomUUID(),
    action,
    domain,
    confidence,
    reason,
    summary,
    reviewRecommended,
    evidence,
    latencyMs: Date.now() - started,
    expiresAt,
    providerTrace,
  };
}
