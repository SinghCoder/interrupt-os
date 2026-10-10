# Interrupt OS v1 design

Date: 2026-10-10

## Product thesis

Every notification is a request to spend attention. Interrupt OS evaluates that request against the user's policy, current circumstances, and live external evidence. It does not merely summarize notifications: it decides whether the claimed urgency has a meaningful consequence for this user now.

The v1 must look and behave like a genuine consumer product. QA scenario tooling is built separately and never appears in the production application.

## Scope

The first release supports two domains:

1. Commerce notifications that claim price, scarcity, or deadline urgency.
2. Travel notifications describing delays, cancellations, itinerary changes, or time-sensitive alternatives.

The app also safely handles unknown domains. Unknown and unsupported notifications are preserved unless a user policy deterministically silences them.

## User experience

### Onboarding

The app opens with a concise value proposition: “Keep the interruptions that matter.” The user sees how notification content is processed and grants notification access. Cloud processing is stated plainly before access is requested.

### Home

The home screen answers three questions without requiring navigation:

- Is protection running?
- How many requests for attention were evaluated today?
- Which recent decisions matter?

It uses a calm editorial layout rather than a security-dashboard aesthetic. The primary metric is “interruptions held back,” balanced by “important alerts delivered” so the product does not reward indiscriminate suppression.

### Decision detail

Every modified notification has an evidence record containing:

- Original app and claim.
- Final action and confidence.
- A short user-facing reason.
- Current-world evidence with merchant, flight, map, or news sources.
- Timing and provider health.

Internal model chain-of-thought is never shown or stored. Only structured decisions, evidence, and concise explanations are retained.

### Policies

Users enable or disable protection for supported domains. Commerce and travel are enabled by default; unsupported domains are observed without changing delivery.

### Failure behaviour

The system fails open. Eligible notifications are withdrawn while evaluation runs; if the service is unavailable, evidence is insufficient, or the evaluation exceeds its deadline, Interrupt OS restores the original title, body, source label, and content intent. Android platform categories for calls, alarms, conversations, navigation, system notifications, and foreground services bypass cloud evaluation entirely. Interrupt OS's own notifications are excluded to prevent loops.

## Android architecture

The app is native Kotlin with Jetpack Compose, targeting Android API 37 and supporting API 29 and later.

Main components:

- `NotificationCaptureService`: a `NotificationListenerService` that normalizes eligible notifications and observes cancellation/removal events.
- `EligibilityPolicy`: platform-category safety exclusions and loop prevention.
- `DecisionClient`: authenticated HTTPS client with bounded timeouts and request cancellation.
- `ReplacementNotifier`: posts Interrupt OS notifications with evidence and preserved safe actions.
- `DecisionStore`: Android Keystore-encrypted local persistence for the activity timeline.
- Compose UI: onboarding, home, activity detail, domain rules, and system access.

Notification event states are `observed`, `evaluating`, `preserved`, `held`, `replaced`, `timed_out`, and `failed`. State transitions are idempotent by source notification key plus post time.

## Backend architecture

The backend is a stateless TypeScript service deployed to AWS Lambda behind an HTTP API in `ap-south-1`. Installations receive signed, expiring credentials; the service stores no notification history. Provider secrets are stored in AWS Secrets Manager.

The evaluation pipeline is provider-independent:

1. Android platform-category safety exclusions.
2. Fast semantic classification through Jev, failing open if classification is unavailable.
3. Evidence planning through OpenAI structured function calls when live evidence is useful.
4. SerpApi runtime queries.
5. Provider-neutral evidence normalization.
6. Model-driven typed decision and concise explanation, with no fixed product, price, or delay thresholds.

Jev is included only if measured latency, calibration, and decision quality improve the pipeline. A provider interface and recorded evaluation corpus allow it to be enabled or removed without changing the Android client.

The response contract includes `action`, `domain`, `confidence`, `reason`, `evidence`, `expiresAt`, `latency`, and a stable `decisionId`. Actions are `preserve`, `hold`, `replace_silent`, and `replace_alerting`.

## Evidence providers

Commerce uses Google Shopping and Google Product results. The system retrieves product identity, current merchant distribution, prices, and price insights when available. It does not claim historical lows without historical evidence.

Travel uses Google Flights for current alternatives and Google Search for public flight-status corroboration. A private itinerary change remains a source claim unless current public results independently support it.

## Privacy and security

- API keys exist only in AWS Secrets Manager.
- Transport uses HTTPS; the Android app authenticates with an installation-scoped credential.
- Notification content is minimized before upload and excluded from ordinary logs.
- The user can delete encrypted local history and revoke notification access at any time. Installation credentials expire automatically.
- The service does not persist decision requests or notification bodies.
- Release builds contain no fixture code, scenario controls, private endpoints, or development certificates.

## QA fixtures

Two separate fixture packages have distinct identities: a retailer and an airline. They post ordinary Android notifications only from externally supplied payloads and contain no default scenario copy. They are built and installed only for automated end-to-end testing.

The QA harness discovers a product or route at runtime, retrieves current SerpApi evidence, and then asks a fixture package to post only the resulting notification text. Interrupt OS does not receive the generator's evidence or expected outcome; it independently plans a search, retrieves current results, and decides. When current search cannot corroborate a claimed travel disruption, the correct acceptance outcome is preservation rather than a forced escalation.

## Visual design

The visual language is warm, restrained, and editorial: graphite and warm off-white surfaces, muted stone secondary content, and amber reserved for justified attention. It avoids neon AI gradients, generic shields, notification bells, glass-heavy dashboards, and fear-based copy.

Generated onboarding artwork illustrates many signals resolving into one justified interruption. The adaptive launcher icon remains vector-native for correct masking and monochrome theming.

## Verification and release bar

The emulator acceptance run must demonstrate a native source notification, listener ingestion, cloud evaluation, independent live SerpApi evidence, immediate withdrawal of eligible originals, quiet replacement or faithful restoration, and visible history.

A physical-device pass remains required before claiming broad production readiness because OEM background execution and notification presentation vary. Store signing, listing, privacy-policy publication, and multi-OEM acceptance are release operations outside the emulator build.
