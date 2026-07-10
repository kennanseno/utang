import { NextRequest, NextResponse } from "next/server";
import { newPayToken, requireStoreId } from "@/lib/server/auth";
import { query } from "@/lib/server/db";
import { jsonError } from "@/lib/server/errors";
import { toCustomerResponse } from "@/lib/server/mappers";
import { toE164OrNull } from "@/lib/server/phone";
import { normalizeCustomerName } from "@/lib/server/validation";

type CustomerRow = {
  id: number;
  name: string;
  phone_number: string | null;
  current_balance: string | number;
  pay_token: string;
};

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  try {
    const storeId = await requireStoreId(req);
    const customers = await query<CustomerRow>(
      `SELECT id, name, phone_number, current_balance, pay_token
       FROM customers
       WHERE store_id = $1
       ORDER BY name ASC`,
      [storeId]
    );
    return NextResponse.json(customers.rows.map(toCustomerResponse));
  } catch (error) {
    return jsonError(error);
  }
}

export async function POST(req: NextRequest) {
  try {
    const storeId = await requireStoreId(req);
    const body = await req.json();
    const name = normalizeCustomerName(body.name);
    const phoneNumber = toE164OrNull(body.phoneNumber);

    const inserted = await query<CustomerRow>(
      `INSERT INTO customers (store_id, name, phone_number, pay_token)
       VALUES ($1, $2, $3, $4)
       RETURNING id, name, phone_number, current_balance, pay_token`,
      [storeId, name, phoneNumber, newPayToken()]
    );
    return NextResponse.json(toCustomerResponse(inserted.rows[0]));
  } catch (error) {
    return jsonError(error);
  }
}
