import { badRequest } from "./errors";

/**
 * Parses an optional phone number. Returns null when the input is missing or
 * blank (customers may be added without a mobile number), otherwise validates
 * and normalizes it to E.164 the same way as `toE164`.
 */
export function toE164OrNull(input: unknown): string | null {
  if (input == null) return null;
  if (typeof input !== "string" || !input.trim()) return null;
  return toE164(input);
}

export function toE164(input: string): string {
  if (!input || !input.trim()) {
    badRequest("phoneNumber is required");
  }
  const digits = input.trim().replace(/[\s()-]/g, "");

  if (digits.startsWith("+")) {
    const rest = digits.slice(1);
    requireDigits(rest, input);
    return `+${rest}`;
  }
  if (digits.startsWith("00")) {
    const rest = digits.slice(2);
    requireDigits(rest, input);
    return `+${rest}`;
  }
  if (/^09\d{9}$/.test(digits)) {
    return `+63${digits.slice(1)}`;
  }
  if (/^639\d{9}$/.test(digits)) {
    return `+${digits}`;
  }
  if (/^9\d{9}$/.test(digits)) {
    return `+63${digits}`;
  }
  badRequest(`Invalid phone number: ${input}`);
}

function requireDigits(value: string, original: string) {
  if (!/^\d{8,15}$/.test(value)) {
    badRequest(`Invalid phone number: ${original}`);
  }
}
