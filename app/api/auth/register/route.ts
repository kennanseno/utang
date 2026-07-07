import { NextRequest, NextResponse } from "next/server";
import { hashPassword, issueToken } from "@/lib/server/auth";
import { query } from "@/lib/server/db";
import { HttpError, conflict, isUniqueViolation, jsonError } from "@/lib/server/errors";
import { toStoreResponse } from "@/lib/server/mappers";
import { toE164 } from "@/lib/server/phone";
import {
  normalizeEmail,
  normalizeOwnerName,
  normalizeStoreName,
  normalizeUsername,
  requirePassword,
} from "@/lib/server/validation";

const GENERIC_TAKEN_MESSAGE =
  "Some of those details are already in use. Please try a different username, store name, email, or mobile number.";

type StoreRow = {
  id: number;
  username: string | null;
  phone_number: string;
  email: string | null;
  name: string;
  owner_name: string | null;
  onboarded: boolean;
  qr_code_content_type: string | null;
};

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const username = normalizeUsername(body.username);
    const password = requirePassword(body.password);
    const phoneNumber = toE164(body.phoneNumber);
    const email = normalizeEmail(body.email);
    const storeName = normalizeStoreName(body.storeName);
    const ownerName = normalizeOwnerName(body.ownerName);

    const exists = await query<{
      username_exists: boolean;
      store_name_exists: boolean;
      phone_exists: boolean;
      email_exists: boolean;
    }>(
      `SELECT
          EXISTS(SELECT 1 FROM stores WHERE username = $1) AS username_exists,
          EXISTS(SELECT 1 FROM stores WHERE LOWER(name) = LOWER($2)) AS store_name_exists,
          EXISTS(SELECT 1 FROM stores WHERE phone_number = $3) AS phone_exists,
          EXISTS(SELECT 1 FROM stores WHERE email = $4) AS email_exists`,
      [username, storeName, phoneNumber, email]
    );

    const taken = exists.rows[0];
    if (
      taken.username_exists ||
      taken.store_name_exists ||
      taken.phone_exists ||
      taken.email_exists
    ) {
      conflict(GENERIC_TAKEN_MESSAGE);
    }

    const passwordHash = await hashPassword(password);
    const inserted = await query<StoreRow>(
      `INSERT INTO stores (username, password_hash, phone_number, email, name, owner_name, onboarded)
       VALUES ($1, $2, $3, $4, $5, $6, TRUE)
       RETURNING id, username, phone_number, email, name, owner_name, onboarded, qr_code_content_type`,
      [username, passwordHash, phoneNumber, email, storeName, ownerName]
    );

    const store = inserted.rows[0];
    return NextResponse.json({
      token: issueToken(store.id),
      store: toStoreResponse(store),
    });
  } catch (error) {
    if (isUniqueViolation(error)) {
      return jsonError(new HttpError(409, GENERIC_TAKEN_MESSAGE));
    }
    return jsonError(error);
  }
}
