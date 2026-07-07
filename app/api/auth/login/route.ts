import { NextRequest, NextResponse } from "next/server";
import { issueToken, verifyPassword } from "@/lib/server/auth";
import { query } from "@/lib/server/db";
import { jsonError, unauthorized } from "@/lib/server/errors";
import { toStoreResponse } from "@/lib/server/mappers";
import { normalizeUsername, requirePassword } from "@/lib/server/validation";

type StoreRow = {
  id: number;
  username: string | null;
  password_hash: string | null;
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

    const storeRes = await query<StoreRow>(
      `SELECT id, username, password_hash, phone_number, email, name, owner_name, onboarded, qr_code_content_type
       FROM stores
       WHERE username = $1`,
      [username]
    );
    if (storeRes.rowCount === 0) {
      unauthorized("Invalid username or password");
    }

    const store = storeRes.rows[0];
    const ok = await verifyPassword(password, store.password_hash);
    if (!ok) {
      unauthorized("Invalid username or password");
    }

    return NextResponse.json({
      token: issueToken(store.id),
      store: toStoreResponse(store),
    });
  } catch (error) {
    return jsonError(error);
  }
}
