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

The app opens with a concise value proposition: “Keep the interruptions that matter.” The user sees how notification content is processed, chooses a protection posture, grants notification access, and selects apps to protect. Cloud processing is stated plainly before access is requested.

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
- Controls to correct the decision and adjust future policy.

Internal model chain-of-thought is never shown or stored. Only structured decisions, evidence, and concise explanations are retained.

### Policies

Users configure each app as Observe, Protect, Always deliver, or Mute. Protect mode permits Interrupt OS to cancel and replace qualifying notifications. Observe mode records recommendations but leaves originals intact.

### Failure behaviour

The system fails open. If the service is unavailable, evidence is insufficient, or the evaluation exceeds its deadline, the original notification remains. OTPs, calls, alarms, conversations, foreground-service notifications, emergency alerts, and Interrupt OS's own notifications bypass cloud evaluation.

## Android architecture

The app is native Kotlin with Jetpack Compose, targeting Android API 36 and supporting API 29 and later.

Main components:

- `NotificationCaptureService`: a `NotificationListenerService` that normalizes eligible notifications and observes cancellation/removal events.
- `EligibilityPolicy`: deterministic safety exclusions, per-app mode, deduplication, and loop prevention.
- `DecisionClient`: authenticated HTTPS client with bounded timeouts and request cancellation.
- `DecisionCoordinator`: owns the event state machine and only cancels an original after a conclusive Protect-mode decision.
- `ReplacementNotifier`: posts Interrupt OS notifications with evidence and preserved safe actions.
- `DecisionRepository`: encrypted local persistence for the timeline and user corrections.
- Compose UI: onboarding, home, timeline, detail, protected apps, profile, privacy, and settings.

Notification event states are `observed`, `evaluating`, `preserved`, `held`, `replaced`, `timed_out`, and `failed`. State transitions are idempotent by source notification key plus post time.

## Backend architecture

The backend is a stateless TypeScript service deployed to AWS Lambda behind an HTTP API in `ap-south-1`. DynamoDB stores pseudonymous device configuration and bounded decision metadata only where server-side persistence is required. Secrets are stored in AWS Secrets Manager.

The evaluation pipeline is provider-independent:

1. Deterministic safety and domain rules.
2. Fast classification experiment: Jev, OpenAI structured output, or rules.
3. Evidence planning through OpenAI structured function calls when live evidence is useful.
4. SerpApi runtime queries.
5. Deterministic evidence normalization and scoring.
6. Final typed decision and concise explanation.

Jev is included only if measured latency, calibration, and decision quality improve the pipeline. A provider interface and recorded evaluation corpus allow it to be enabled or removed without changing the Android client.

The response contract includes `action`, `domain`, `confidence`, `reason`, `evidence`, `expiresAt`, `latency`, and a stable `decisionId`. Actions are `preserve`, `hold`, `replace_silent`, and `replace_alerting`.

## Evidence providers

Commerce uses Google Shopping and Google Product results. The system compares normalized product identity, total price when available, merchant distribution, price insights, and availability. It does not claim historical lows without historical evidence.

Travel uses Google Flights for current alternatives and Google Maps Directions when reachability affects the decision. A delay notification is treated as the source claim; live search establishes alternatives and consequences rather than pretending to independently verify private booking state.

## Privacy and security

- API keys exist only in AWS Secrets Manager.
- Transport uses HTTPS; the Android app authenticates with an installation-scoped credential.
- Notification content is minimized before upload and excluded from ordinary logs.
- The user can delete local history and revoke the installation credential.
- Server metadata has a short retention window and no advertising use.
- Release builds contain no fixture code, scenario controls, private endpoints, or development certificates.

## QA fixtures

Two separate fixture packages share a small library but have distinct identities: a retailer and an airline. They post ordinary Android notifications from externally supplied fixture payloads. They are built and installed only for automated end-to-end testing.

The fixture suite covers misleading commerce urgency, useful commerce urgency, harmless travel delay, connection-threatening delay, API timeout, malformed notification, duplicated post, app restart, and offline recovery.

## Visual design

The visual language is warm, restrained, and editorial: graphite and warm off-white surfaces, muted stone secondary content, and amber reserved for justified attention. It avoids neon AI gradients, generic shields, notification bells, glass-heavy dashboards, and fear-based copy.

Generated onboarding artwork illustrates many signals resolving into one justified interruption. The adaptive launcher icon remains vector-native for correct masking and monochrome theming.

## Verification and release bar

The emulator acceptance run must demonstrate a native source notification, listener ingestion, cloud evaluation, live SerpApi evidence, cancellation or preservation, replacement delivery, visible history, correction feedback, and safe failure on network loss. Automated tests cover decision rules, serialization, idempotency, timeout behaviour, and UI state.

A physical-device pass remains required before claiming broad production readiness because OEM background execution and notification presentation vary. Store signing, listing, privacy-policy publication, and multi-OEM acceptance are release operations outside the emulator build.

