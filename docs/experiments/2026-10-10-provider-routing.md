# Provider routing experiment

Date: 2026-10-10

## Purpose

Determine whether Jev provides enough latency and decision-quality advantage to belong in the notification routing path rather than forcing it into the architecture by preference.

## Results

### Single commerce notification

- Jev 1.13.0: 400 ms.
- OpenAI `gpt-6-luna` structured output: 2,105 ms.
- Both correctly returned commerce, explicit time pressure, and useful live verification.
- Live SerpApi Google Shopping lookup: 3,515 ms, 40 results.

### Twelve-notification routing corpus

The corpus included three commerce, three travel, three critical, and three unrelated notifications. Jev evaluated one Choice question per notification in a single request.

- Accuracy against the authored labels: 12/12.
- End-to-end TypeSafe latency: 457 ms.
- Every returned Choice had confidence 1.0 on this simple corpus.

## Decision

Keep Jev as the fast semantic router for v1. Android platform categories bypass evaluation for calls, alarms, conversations, navigation, system, and foreground-service notifications; classification failure also fails open. Evidence interpretation and the final attention decision remain model-driven rather than using product-specific regular expressions or fixed price/delay thresholds. Keep the provider interface and corpus probe so Jev can be re-evaluated against harder examples and removed without an Android-client change.

These results establish feasibility, not general model accuracy. The authored corpus is small and clear-cut; ambiguous and adversarial cases remain part of acceptance testing.
