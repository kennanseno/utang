import { randomBytes } from "crypto";
import { NextRequest } from "next/server";
import { compare, hash } from "bcryptjs";
import { query } from "./db";
import { unauthorized } from "./errors";

const PREFIX = "utang.";

export function issueToken(storeId: number): string {
  const raw = `store:${storeId}`;
  return `${PREFIX}${Buffer.from(raw, "utf8").toString("base64url")}`;
}

export function resolveStoreId(token: string | null): number | null {
  if (!token || !token.startsWith(PREFIX)) return null;
  try {
    const decoded = Buffer.from(token.slice(PREFIX.length), "base64url").toString(
      "utf8"
    );
    if (!decoded.startsWith("store:")) return null;
    const storeId = Number(decoded.slice("store:".length));
    return Number.isInteger(storeId) && storeId > 0 ? storeId : null;
  } catch {
    return null;
  }
}

export function getBearerToken(req: NextRequest): string | null {
  const header = req.headers.get("authorization");
  if (!header) return null;
  const [scheme, token] = header.split(" ");
  if (!scheme || !token || scheme.toLowerCase() !== "bearer") return null;
  return token;
}

export async function requireStoreId(req: NextRequest): Promise<number> {
  const token = getBearerToken(req);
  const storeId = resolveStoreId(token);
  if (!storeId) {
    unauthorized("Unauthorized");
  }

  const exists = await query<{ id: number }>("SELECT id FROM stores WHERE id = $1", [
    storeId,
  ]);
  if (exists.rowCount === 0) {
    unauthorized("Unauthorized");
  }
  return storeId;
}

export async function hashPassword(rawPassword: string): Promise<string> {
  return hash(rawPassword, 10);
}

export async function verifyPassword(
  rawPassword: string,
  passwordHash: string | null
): Promise<boolean> {
  if (!passwordHash) return false;
  return compare(rawPassword, passwordHash);
}

export function newPayToken(): string {
  return randomBytes(32).toString("hex");
}
