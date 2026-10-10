import { describe, expect, it } from "vitest";
import { createInstallationToken, installationIdFromToken, verifyInstallationToken } from "../src/auth.js";

describe("installation authentication", () => {
  it("accepts issued tokens and rejects tampering", () => {
    const { installationId, token } = createInstallationToken();
    expect(verifyInstallationToken(`Bearer ${token}`)).toBe(true);
    expect(installationIdFromToken(`Bearer ${token}`)).toBe(installationId);
    expect(verifyInstallationToken(`Bearer ${token}x`)).toBe(false);
  });
});
