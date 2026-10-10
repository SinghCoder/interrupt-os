import { createServer } from "node:http";
import { createInstallationToken, installationIdFromToken } from "./auth.js";
import { evaluate } from "./evaluator.js";
import { recordFeedback } from "./feedback.js";
import type { FeedbackEvent, NotificationEvent } from "./types.js";

const port = Number(process.env.PORT ?? 8787);

createServer(async (request, response) => {
  response.setHeader("Content-Type", "application/json");
  if (request.method === "GET" && request.url === "/health") {
    response.writeHead(200).end(JSON.stringify({ status: "ok" }));
    return;
  }
  if (request.method === "POST" && request.url === "/v1/installations") {
    response.writeHead(201).end(JSON.stringify(createInstallationToken()));
    return;
  }
  const installationId = installationIdFromToken(request.headers.authorization);
  if (!installationId) {
    response.writeHead(401).end(JSON.stringify({ error: "unauthorized" }));
    return;
  }
  if (request.method === "POST" && request.url === "/v1/feedback") {
    try {
      const chunks: Buffer[] = [];
      for await (const chunk of request) chunks.push(Buffer.from(chunk));
      await recordFeedback(installationId, JSON.parse(Buffer.concat(chunks).toString("utf8")) as FeedbackEvent);
      response.writeHead(202).end(JSON.stringify({ accepted: true }));
    } catch (error) {
      const message = error instanceof Error ? error.message : "unknown error";
      response.writeHead(400).end(JSON.stringify({ error: "invalid_request", message }));
    }
    return;
  }
  if (request.method !== "POST" || request.url !== "/v1/evaluate") {
    response.writeHead(404).end(JSON.stringify({ error: "not_found" }));
    return;
  }
  try {
    const chunks: Buffer[] = [];
    for await (const chunk of request) chunks.push(Buffer.from(chunk));
    const event = JSON.parse(Buffer.concat(chunks).toString("utf8")) as NotificationEvent;
    response.writeHead(200).end(JSON.stringify(await evaluate(event, installationId)));
  } catch (error) {
    const message = error instanceof Error ? error.message : "unknown error";
    response.writeHead(400).end(JSON.stringify({ error: "invalid_request", message }));
  }
}).listen(port, "0.0.0.0", () => {
  console.log(`Interrupt OS service listening on ${port}`);
});
