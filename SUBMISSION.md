# SerpApi India Hackathon submission copy

## Project name

Interrupt OS

## Tagline

Every notification should earn the right to interrupt.

## Track

AI Agents

## Project description

Interrupt OS is an Android attention agent that decides whether a notification
deserves to interrupt you. When an app claims that a fare is scarce, a price is
exceptional, or action is urgent, Interrupt OS plans the evidence it needs and
checks the live web before acting. SerpApi supplies current product offers,
merchant comparisons, flight alternatives, fare context, and status results at
runtime. A model combines that evidence with the notification and the user's
recent corrections, then either keeps the alert quiet or delivers it with a
concise, inspectable reason. Calls, alarms, navigation, and other consequential
notifications fail open and arrive immediately.

## Meaningful SerpApi usage

SerpApi is the live evidence layer in the decision loop, not a standalone lookup
or prebuilt dataset. Each eligible notification produces a model-generated
evidence plan. The service invokes Google Shopping and Google Product for exact
product and merchant comparisons, Google Flights for alternatives and price
insights, Google Flights Deals for destination deal claims, and Google Search
for current flight-status evidence. These results directly affect whether the
notification is suppressed or delivered. If evidence is unavailable, the
system fails open and preserves the original notification.

## Who it helps

Anyone whose attention is repeatedly claimed by promotional urgency, especially
people managing travel, shopping, and time-sensitive work from their phone.

## AI tools disclosure

OpenAI Codex was used for AI-assisted development. The application uses OpenAI
and TypeSafe/Jev at runtime. HyperFrames was used to compose the walkthrough;
ElevenLabs generated its narration and music.

## Existing project

No. Interrupt OS was created for this hackathon.

## Public links

- Repository: https://github.com/SinghCoder/interrupt-os
- Demo video: https://youtu.be/aAxrcv7zvlo

## Final checks

- Repository opens without authentication.
- Video opens in an incognito window and is under three minutes.
- Participant details are entered only in the authenticated submission form.
- The form is explicitly submitted, not left as a draft.
