import { badRequest } from "./errors";

const USERNAME_PATTERN = /^[a-zA-Z0-9]+$/;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function normalizeUsername(input: unknown): string {
  if (typeof input !== "string" || !input.trim()) {
    badRequest("username is required");
  }
  const value = input.trim();
  if (value.length < 3 || value.length > 60 || !USERNAME_PATTERN.test(value)) {
    badRequest("username may only contain letters and numbers");
  }
  return value.toLowerCase();
}

export function normalizeEmail(input: unknown): string {
  if (typeof input !== "string" || !input.trim()) {
    badRequest("email is required");
  }
  const value = input.trim().toLowerCase();
  if (!EMAIL_PATTERN.test(value)) {
    badRequest("email must be a well-formed email address");
  }
  return value;
}

export function normalizeStoreName(input: unknown): string {
  if (typeof input !== "string" || !input.trim()) {
    badRequest("storeName is required");
  }
  return input.trim();
}

export function normalizeOwnerName(input: unknown): string | null {
  if (input == null) return null;
  if (typeof input !== "string") return null;
  const value = input.trim();
  return value ? value : null;
}

export function requirePassword(input: unknown): string {
  if (typeof input !== "string" || !input) {
    badRequest("password is required");
  }
  if (input.length < 6 || input.length > 100) {
    badRequest("password size must be between 6 and 100");
  }
  return input;
}

export function normalizeCustomerName(input: unknown): string {
  if (typeof input !== "string" || !input.trim()) {
    badRequest("name is required");
  }
  return input.trim();
}

export function normalizePositiveAmount(input: unknown): number {
  const amount = typeof input === "number" ? input : Number(input);
  if (!Number.isFinite(amount) || amount <= 0) {
    badRequest("amount must be greater than zero");
  }
  return amount;
}
