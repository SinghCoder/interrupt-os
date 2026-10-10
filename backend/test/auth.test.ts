import { describe, expect, it } from "vitest";
import { createInstallationToken, verifyInstallationToken } from "../src/auth.js";

describe("installation authentication", () => {
  it("accepts issued tokens and rejects tampering", () => {
    const { token } = createInstallationToken();
    expect(verifyInstallationToken(`Bearer ${token}`)).toBe(true);
    expect(verifyInstallationToken(`Bearer ${token}x`)).toBe(false);
  });
});
