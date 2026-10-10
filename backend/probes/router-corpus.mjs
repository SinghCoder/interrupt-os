const cases = [
  ["commerce", "Myntra", "Extra 20% off ends tonight", "Your wishlist styles are nearly gone"],
  ["commerce", "Amazon", "Sony headphones dropped 35%", "Only 20 minutes left. ₹24,990 today"],
  ["commerce", "Swiggy", "Free delivery for the next hour", "Order from restaurants near you"],
  ["travel", "Air India", "AI 302 delayed by 75 minutes", "Delhi to Bengaluru now departs at 18:55"],
  ["travel", "Booking.com", "Your hotel changed your check-in", "Review the updated arrival window"],
  ["travel", "Uber", "Leave in 8 minutes", "Traffic to Delhi airport is increasing"],
  ["critical", "Messages", "Your verification code is 839102", "Do not share this code"],
  ["critical", "Clock", "Alarm", "Wake up"],
  ["critical", "Phone", "Incoming call", "Mum"],
  ["other", "YouTube", "A creator uploaded a new video", "Watch now"],
  ["other", "Slack", "Design review", "A teammate mentioned you"],
  ["other", "Photos", "Memories from this day", "See your photos from 4 years ago"],
].map(([expected, source, title, body], index) => ({ id: `n${index}`, expected, source, title, body }));

const question = (item) => ({
  type: "choice",
  instructions: {
    question: "Which notification domain best describes this item?",
    notification_id: item.id,
  },
  criteria: {
    commerce: "A product, merchant, price, promotion, food order, or purchase",
    travel: "A flight, hotel, ride, route, booking, delay, or trip",
    critical: "An OTP, alarm, emergency, security event, or incoming call",
    other: "None of the other options, including ordinary work and social messages",
  },
});

const timed = async (fn) => {
  const start = performance.now();
  const value = await fn();
  return { elapsedMs: Math.round(performance.now() - start), value };
};

const jev = await timed(async () => {
  const response = await fetch("https://api.typesafe.ai/v1/systemone", {
    method: "POST",
    headers: { Authorization: `Bearer ${process.env.TYPESAFE_API_KEY}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      model: "jev-latest",
      state: { notifications: cases.map(({ id, source, title, body }) => ({ id, source, title, body })) },
      questions: Object.fromEntries(cases.map((item) => [item.id, question(item)])),
    }),
  });
  if (!response.ok) throw new Error(`TypeSafe ${response.status}: ${await response.text()}`);
  const body = await response.json();
  return { model: body.model, answers: body.answers };
});

const rows = cases.map((item) => {
  const answer = jev.value.answers[item.id];
  return {
    id: item.id,
    expected: item.expected,
    predicted: answer.choice,
    confidence: answer.confidence,
    correct: answer.choice === item.expected,
  };
});

console.log(JSON.stringify({
  model: jev.value.model,
  elapsedMs: jev.elapsedMs,
  accuracy: rows.filter((row) => row.correct).length / rows.length,
  rows,
}, null, 2));
