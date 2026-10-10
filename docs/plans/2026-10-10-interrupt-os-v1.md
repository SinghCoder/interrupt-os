# Interrupt OS v1 implementation plan

## Phase 1: Foundations

- Create the Android Compose project, shared design tokens, navigation, persistence, and network contracts.
- Create the backend TypeScript project, typed decision contract, provider interfaces, and local tests.
- Install one API 36 ARM64 emulator and establish repeatable build/install/screenshot commands.

## Phase 2: Decision experiments

- Build a small notification corpus spanning commerce, travel, critical bypass, unsupported, and adversarial urgency.
- Measure deterministic rules, Jev, and OpenAI structured output for routing accuracy, abstention, latency, and cost.
- Select the smallest reliable cascade and document the experiment result.
- Validate live SerpApi Shopping/Product and Flights result shapes.

## Phase 3: End-to-end core

- Implement notification access, capture, normalization, exclusions, deduplication, and fail-open timeouts.
- Implement backend evaluation, evidence collection, and typed response.
- Implement cancellation and replacement notification flows.
- Add fixture packages and automated scenario injection.

## Phase 4: Product interface

- Implement polished onboarding, home, timeline, detail, protected-app policy, profile, privacy, and settings screens.
- Integrate optimized generated artwork and adaptive app icon.
- Add empty, loading, offline, degraded, and permission-loss states.
- Validate light/dark themes and compact/large emulator sizes.

## Phase 5: Cloud and reliability

- Verify the AWS identity, create least-scoped application resources, store provider credentials in Secrets Manager, and deploy to `ap-south-1`.
- Add installation authentication, request limits, structured logs without notification bodies, alarms, and bounded retention.
- Exercise provider failures, cold starts, retries, duplicate delivery, and process death.

## Phase 6: Acceptance

- Run unit, integration, Compose UI, and backend tests.
- Perform fresh-emulator onboarding and all end-to-end scenarios.
- Capture screenshots, video, timings, API evidence, and final APK hashes.
- Audit the APK for credentials and fixture code.
- Record remaining physical-device and store-release requirements.

