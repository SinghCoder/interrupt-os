import { createHmac, randomUUID, timingSafeEqual } from "node:crypto";

interface InstallationClaims {
  sub: string;
  iat: number;
  exp: number;
}

function signingKey(): string {
  const value = process.env.INSTALLATION_SIGNING_KEY;
  if (value) return value;
  if (process.env.NODE_ENV === "production") throw new Error("INSTALLATION_SIGNING_KEY is not configured");
  return "interrupt-os-local-development-key";
}

function signature(payload: string): string {
  return createHmac("sha256", signingKey()).update(payload).digest("base64url");
}

export function createInstallationToken(): { installationId: string; token: string; expiresAt: string } {
  const now = Math.floor(Date.now() / 1000);
  const claims: InstallationClaims = { sub: randomUUID(), iat: now, exp: now + 90 * 24 * 60 * 60 };
  const payload = Buffer.from(JSON.stringify(claims)).toString("base64url");
  const token = `${payload}.${signature(payload)}`;
  return { installationId: claims.sub, token, expiresAt: new Date(claims.exp * 1000).toISOString() };
}

export function verifyInstallationToken(header: string | undefined): boolean {
  if (!header?.startsWith("Bearer ")) return false;
  const token = header.slice("Bearer ".length);
  const [payload, supplied] = token.split(".");
  if (!payload || !supplied) return false;
  const expected = signature(payload);
  const suppliedBuffer = Buffer.from(supplied);
  const expectedBuffer = Buffer.from(expected);
  if (suppliedBuffer.length !== expectedBuffer.length || !timingSafeEqual(suppliedBuffer, expectedBuffer)) return false;
  try {
    const claims = JSON.parse(Buffer.from(payload, "base64url").toString("utf8")) as InstallationClaims;
    return typeof claims.sub === "string" && claims.exp > Math.floor(Date.now() / 1000);
  } catch {
    return false;
  }
}

