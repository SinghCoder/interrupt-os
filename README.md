# Interrupt OS

Interrupt OS is an Android attention layer that evaluates whether incoming notifications deserve to interrupt the user. It combines user policy, semantic reasoning, and live external evidence before preserving, suppressing, or replacing a notification.

The project contains:

- `android/`: the production Android application.
- `backend/`: the cloud decision service.
- `android/fixture-retail` and `android/fixture-travel`: isolated QA notification-source applications.
- `docs/`: product design, architecture, and implementation plans.
- `design-assets/`: generated source artwork before Android optimization.

No API credentials belong in the Android application or repository.

Provider credentials remain in AWS Secrets Manager; Android installations receive a scoped signed token.

## Verify locally

```sh
cd backend
npm run typecheck
npm test
npm run build

cd ../android
./gradlew :app:assembleDebug :fixture-retail:assembleDebug :fixture-travel:assembleDebug
```

The QA harness discovers its product or route at runtime, retrieves current SerpApi evidence, and supplies only the resulting notification payload to a fixture app. Interrupt OS receives no expected answer and performs its own model-and-search evaluation.

```sh
cd backend
source ~/.zshrc
npm run qa:scenario -- commerce-drop
npm run qa:scenario -- commerce-ordinary --post
npm run qa:scenario -- --post-latest
```
