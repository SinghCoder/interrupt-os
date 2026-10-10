import type { APIGatewayProxyHandlerV2 } from "aws-lambda";
import { createInstallationToken, verifyInstallationToken } from "./auth.js";
import { evaluate } from "./evaluator.js";
import type { NotificationEvent } from "./types.js";

export const handler: APIGatewayProxyHandlerV2 = async (event) => {
  if (event.requestContext.http.method === "GET" && event.rawPath === "/health") {
    return { statusCode: 200, headers: { "content-type": "application/json" }, body: JSON.stringify({ status: "ok" }) };
  }
  if (event.requestContext.http.method === "POST" && event.rawPath === "/v1/installations") {
    return { statusCode: 201, headers: { "content-type": "application/json" }, body: JSON.stringify(createInstallationToken()) };
  }
  if (event.requestContext.http.method !== "POST" || event.rawPath !== "/v1/evaluate") {
    return { statusCode: 404, headers: { "content-type": "application/json" }, body: JSON.stringify({ error: "not_found" }) };
  }
  if (!verifyInstallationToken(event.headers.authorization)) {
    return { statusCode: 401, headers: { "content-type": "application/json" }, body: JSON.stringify({ error: "unauthorized" }) };
  }
  try {
    const notification = JSON.parse(event.body ?? "{}") as NotificationEvent;
    const decision = await evaluate(notification);
    return { statusCode: 200, headers: { "content-type": "application/json" }, body: JSON.stringify(decision) };
  } catch (error) {
    const message = error instanceof Error ? error.message : "unknown error";
    return { statusCode: 400, headers: { "content-type": "application/json" }, body: JSON.stringify({ error: "invalid_request", message }) };
  }
};
