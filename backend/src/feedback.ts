import { DynamoDBClient } from "@aws-sdk/client-dynamodb";
import { DynamoDBDocumentClient, PutCommand, QueryCommand } from "@aws-sdk/lib-dynamodb";
import type { FeedbackEvent } from "./types.js";

const memoryFeedback = new Map<string, FeedbackEvent[]>();
const documentClient = DynamoDBDocumentClient.from(new DynamoDBClient({}));

export async function recordFeedback(installationId: string, feedback: FeedbackEvent): Promise<void> {
  validateFeedback(feedback);
  const tableName = process.env.FEEDBACK_TABLE_NAME;
  if (!tableName) {
    const current = memoryFeedback.get(installationId) ?? [];
    memoryFeedback.set(installationId, [feedback, ...current].slice(0, 30));
    return;
  }
  await documentClient.send(new PutCommand({
    TableName: tableName,
    Item: {
      installationId,
      feedbackKey: `${feedback.createdAt.toString().padStart(16, "0")}#${feedback.feedbackId}`,
      ...feedback,
      expiresAtEpoch: Math.floor(Date.now() / 1000) + 90 * 24 * 60 * 60,
    },
    ConditionExpression: "attribute_not_exists(installationId) AND attribute_not_exists(feedbackKey)",
  }));
}

export async function recentFeedback(installationId: string, limit = 20): Promise<FeedbackEvent[]> {
  const tableName = process.env.FEEDBACK_TABLE_NAME;
  if (!tableName) return (memoryFeedback.get(installationId) ?? []).slice(0, limit);
  const result = await documentClient.send(new QueryCommand({
    TableName: tableName,
    KeyConditionExpression: "installationId = :installationId",
    ExpressionAttributeValues: { ":installationId": installationId },
    ScanIndexForward: false,
    Limit: limit,
  }));
  return (result.Items ?? []).map((item) => ({
    feedbackId: String(item.feedbackId),
    decisionId: String(item.decisionId),
    domain: item.domain as FeedbackEvent["domain"],
    sourcePackage: String(item.sourcePackage),
    sourceLabel: String(item.sourceLabel),
    title: String(item.title),
    body: String(item.body),
    originalAction: item.originalAction as FeedbackEvent["originalAction"],
    reason: String(item.reason),
    correction: item.correction as FeedbackEvent["correction"],
    createdAt: Number(item.createdAt),
  }));
}

function validateFeedback(feedback: FeedbackEvent): void {
  if (!feedback.feedbackId || !feedback.decisionId || !feedback.sourcePackage) throw new Error("feedback identifiers are required");
  if (!(["correct", "should_interrupt", "should_stay_quiet"] as string[]).includes(feedback.correction)) throw new Error("feedback correction is invalid");
  if (feedback.title.length > 500 || feedback.body.length > 2_000 || feedback.reason.length > 500) throw new Error("feedback content is invalid");
}
