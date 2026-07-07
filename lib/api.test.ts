import { describe, expect, it } from "vitest";
import { formatPeso } from "./api";

describe("formatPeso", () => {
  it("formats Philippine peso amounts", () => {
    expect(formatPeso(1234.5)).toBe("₱1,234.50");
  });
});
