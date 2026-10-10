const required = ["SERPAPI_KEY", "OPENAI_API_KEY", "TYPESAFE_API_KEY"];
for (const name of required) {
  if (!process.env[name]) throw new Error(`${name} is not configured`);
}

const notification = {
  packageName: "com.shopco.app",
  title: "Sony WH-1000XM5 dropped 35%",
  text: "Only 20 minutes left. Buy now for ₹24,990.",
  category: "promotion",
};

const timed = async (label, fn) => {
  const started = performance.now();
  const value = await fn();
  return { label, elapsedMs: Math.round(performance.now() - started), value };
};

const typeSafe = await timed("typesafe", async () => {
  const response = await fetch("https://api.typesafe.ai/v1/systemone", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${process.env.TYPESAFE_API_KEY}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model: "jev-latest",
      state: notification,
      questions: {
        domain: {
          type: "choice",
          instructions: "Which notification domain best describes this state?",
          criteria: {
            commerce: "A product, merchant, price, promotion, or purchase",
            travel: "A flight, hotel, route, booking, delay, or trip",
            critical: "An OTP, alarm, emergency, security event, or direct conversation",
            other: "None of the other options",
          },
        },
        claims_time_pressure: {
          type: "noul",
          instructions: "The notification explicitly claims that the user has little time to act.",
        },
        verification_is_useful: {
          type: "noul",
          instructions: "Current public search results could materially help evaluate the notification's claim.",
        },
      },
    }),
  });
  if (!response.ok) throw new Error(`TypeSafe ${response.status}: ${await response.text()}`);
  const body = await response.json();
  return {
    model: body.model,
    domain: body.answers?.domain,
    timePressure: body.answers?.claims_time_pressure,
    verificationUseful: body.answers?.verification_is_useful,
    usage: body.usage,
  };
});

const modelsResponse = await fetch("https://api.openai.com/v1/models", {
  headers: { Authorization: `Bearer ${process.env.OPENAI_API_KEY}` },
});
if (!modelsResponse.ok) throw new Error(`OpenAI models ${modelsResponse.status}`);
const availableModels = (await modelsResponse.json()).data.map((model) => model.id);
const preferredModels = ["gpt-6-luna", "gpt-6-astra", "gpt-5.2-mini", "gpt-4.1-mini"];
const openAIModel = preferredModels.find((model) => availableModels.includes(model));
if (!openAIModel) throw new Error("No preferred OpenAI model is available");

const openAI = await timed("openai", async () => {
  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${process.env.OPENAI_API_KEY}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model: openAIModel,
      input: [
        {
          role: "system",
          content: "Classify a mobile notification for an attention-protection system. Do not decide whether to suppress it.",
        },
        { role: "user", content: JSON.stringify(notification) },
      ],
      text: {
        format: {
          type: "json_schema",
          name: "notification_route",
          strict: true,
          schema: {
            type: "object",
            additionalProperties: false,
            properties: {
              domain: { type: "string", enum: ["commerce", "travel", "critical", "other"] },
              claimsTimePressure: { type: "boolean" },
              verificationUseful: { type: "boolean" },
            },
            required: ["domain", "claimsTimePressure", "verificationUseful"],
          },
        },
      },
    }),
  });
  if (!response.ok) throw new Error(`OpenAI ${response.status}: ${await response.text()}`);
  const body = await response.json();
  const outputText = body.output
    ?.flatMap((item) => item.content ?? [])
    .find((item) => item.type === "output_text")?.text;
  if (!outputText) throw new Error(`OpenAI response did not contain output text: ${body.status}`);
  return { model: openAIModel, route: JSON.parse(outputText), usage: body.usage };
});

const serpApi = await timed("serpapi", async () => {
  const url = new URL("https://serpapi.com/search.json");
  url.search = new URLSearchParams({
    engine: "google_shopping",
    q: "Sony WH-1000XM5",
    gl: "in",
    hl: "en",
    api_key: process.env.SERPAPI_KEY,
  });
  const response = await fetch(url);
  if (!response.ok) throw new Error(`SerpApi ${response.status}: ${await response.text()}`);
  const body = await response.json();
  return {
    searchId: body.search_metadata?.id,
    status: body.search_metadata?.status,
    resultCount: body.shopping_results?.length ?? 0,
    firstResults: (body.shopping_results ?? []).slice(0, 3).map(({ title, price, source }) => ({ title, price, source })),
  };
});

console.log(JSON.stringify({ typeSafe, openAI, serpApi }, null, 2));
