import { describe, expect, it } from "vitest";
import { recentFeedback, recordFeedback } from "../src/feedback.js";

describe("feedback memory store", () => {
  it("returns the latest correction for an installation", async () => {
    const event = {
      feedbackId: "feedback-1",
      decisionId: "decision-1",
      domain: "commerce" as const,
      sourcePackage: "example.shop",
      sourceLabel: "Example Shop",
      title: "A sale",
      body: "A sale body",
      originalAction: "hold" as const,
      reason: "Ordinary promotion",
      correction: "should_interrupt" as const,
      createdAt: Date.now(),
    };
    await recordFeedback("installation-test", event);
    expect(await recentFeedback("installation-test")).toEqual([event]);
  });
});
