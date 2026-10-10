export type DecisionAction = "preserve" | "hold" | "replace_silent" | "replace_alerting";
export type NotificationDomain = "commerce" | "travel" | "critical" | "other";

export interface NotificationEvent {
  eventId: string;
  sourcePackage: string;
  sourceLabel: string;
  title: string;
  body: string;
  category?: string | null;
  postedAt: number;
  locale?: string;
  currency?: string;
}

export interface EvidenceItem {
  label: string;
  detail: string;
  source?: string;
  url?: string;
}

export interface DecisionResponse {
  decisionId: string;
  action: DecisionAction;
  domain: NotificationDomain;
  confidence: number;
  reason: string;
  summary: string;
  reviewRecommended: boolean;
  evidence: EvidenceItem[];
  latencyMs: number;
  expiresAt: string;
  providerTrace: {
    router: string;
    planner?: string;
    search?: string;
  };
}

export type FeedbackCorrection = "correct" | "should_interrupt" | "should_stay_quiet";

export interface FeedbackEvent {
  feedbackId: string;
  decisionId: string;
  domain: NotificationDomain;
  sourcePackage: string;
  sourceLabel: string;
  title: string;
  body: string;
  originalAction: DecisionAction;
  reason: string;
  correction: FeedbackCorrection;
  createdAt: number;
}

export interface RouteDecision {
  domain: NotificationDomain;
  domainConfidence: number;
  claimsTimePressure: number;
  verificationUseful: number;
  provider: string;
}

export interface EvidencePlan {
  domain: "commerce" | "travel";
  searchQuery: string;
  claimedPrice: number | null;
  claimedDiscountPercent: number | null;
  originAirport: string | null;
  destinationAirport: string | null;
  departureDate: string | null;
  delayMinutes: number | null;
  flightNumber: string | null;
}
