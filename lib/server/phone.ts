import { badRequest } from "./errors";

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
