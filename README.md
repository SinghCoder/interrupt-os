# Interrupt OS

Interrupt OS is an Android attention layer that evaluates whether incoming notifications deserve to interrupt the user. It combines per-app policy, personal context, and live external evidence before preserving, suppressing, or replacing a notification.

The project contains:

- `android/`: the production Android application.
- `backend/`: the cloud decision service.
- `fixtures/`: isolated QA notification-source applications.
- `docs/`: product design, architecture, and implementation plans.
- `design-assets/`: generated source artwork before Android optimization.

No API credentials belong in the Android application or repository.

