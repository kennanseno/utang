import { NextRequest, NextResponse } from "next/server";
import { requireStoreId } from "@/lib/server/auth";
import { query } from "@/lib/server/db";
import { HttpError, conflict, isUniqueViolation, jsonError } from "@/lib/server/errors";
import { toStoreResponse } from "@/lib/server/mappers";
import { toE164 } from "@/lib/server/phone";
import { getStoreById } from "@/lib/server/store";
import {
  normalizeEmail,
  normalizeOwnerName,
  normalizeStoreName,
} from "@/lib/server/validation";

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

export async function PUT(req: NextRequest) {
  try {
    const storeId = await requireStoreId(req);
    const body = await req.json();
    const storeName = normalizeStoreName(body.storeName);
    const ownerName = normalizeOwnerName(body.ownerName);
    const phoneNumber = toE164(body.phoneNumber);
    const email = normalizeEmail(body.email);

    const nameExists = await query<{ exists: boolean }>(
      "SELECT EXISTS(SELECT 1 FROM stores WHERE LOWER(name) = LOWER($1) AND id <> $2) AS exists",
      [storeName, storeId]
    );
    if (nameExists.rows[0].exists) {
      conflict("Store name already taken");
    }

    const phoneExists = await query<{ exists: boolean }>(
      "SELECT EXISTS(SELECT 1 FROM stores WHERE phone_number = $1 AND id <> $2) AS exists",
      [phoneNumber, storeId]
    );
    if (phoneExists.rows[0].exists) {
      conflict("Mobile number already registered");
    }

    const emailExists = await query<{ exists: boolean }>(
      "SELECT EXISTS(SELECT 1 FROM stores WHERE email = $1 AND id <> $2) AS exists",
      [email, storeId]
    );
    if (emailExists.rows[0].exists) {
      conflict("Email already registered");
    }

    await query(
      `UPDATE stores
       SET name = $1, owner_name = $2, phone_number = $3, email = $4
       WHERE id = $5`,
      [storeName, ownerName, phoneNumber, email, storeId]
    );

    const updated = await getStoreById(storeId);
    return NextResponse.json(toStoreResponse(updated));
  } catch (error) {
    if (isUniqueViolation(error)) {
      const store = error as { constraint?: string };
      if (store.constraint?.includes("email")) {
        return jsonError(new HttpError(409, "Email already registered"));
      }
      if (store.constraint?.includes("phone")) {
        return jsonError(new HttpError(409, "Mobile number already registered"));
      }
      if (store.constraint?.includes("name")) {
        return jsonError(new HttpError(409, "Store name already taken"));
      }
    }
    return jsonError(error);
  }
}
