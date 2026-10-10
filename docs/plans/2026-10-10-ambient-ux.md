# Interrupt OS ambient UX

## Product posture

Interrupt OS is infrastructure, not a destination. A successful user should almost never need to open it. The default experience is silent handling, with Android notifications remaining the primary surface.

## Interaction architecture

- Confident decisions happen without asking the user to review them.
- A model may mark an ambiguous, consequential decision as worth reviewing.
- Review-worthy quiet decisions accumulate into one low-priority recap notification.
- Corrections use explicit outcomes: `correct`, `should_interrupt`, or `should_stay_quiet`.
- Corrections are retained as structured preference examples and supplied to later model judgments. They are not converted into fixed thresholds or brittle sender rules.
- The application is an audit and control surface: ambient status, exceptional review, compact recent activity, and settings.

## Screen hierarchy

1. **Home** — protection state, a single activity sentence, an exceptional review card when necessary, and three compact recent decisions.
2. **Review** — only unresolved decisions selected by the model as useful to review.
3. **Activity** — compact audit rows; evidence and reasoning appear on demand in a bottom sheet.
4. **Settings** — protected domains and Android notification access.

There is no bottom navigation, engagement score, streak, time-saved estimate, or permanent review queue.

## Visual system

- Neutral mineral surfaces carry most of the interface.
- Muted sage means the system is operating normally.
- Amber is limited to uncertainty and requested review.
- Brick is limited to a genuine interruption or error.
- Operational screens use a humanist sans-serif hierarchy. Serif is retained only as a small brand accent during onboarding.
- Motion is short and quiet. There are no card-toss effects, confetti, pulsing urgency, or gamified counters.

## Feedback behavior

Every feedback event includes the decision, original action, source, domain, and the explicit correction. The backend stores recent examples per installation with a retention period and supplies them to the reasoning model. The model is instructed to generalize cautiously from preferences while continuing to prioritize current evidence and consequential risk.

