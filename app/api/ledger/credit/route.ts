import { NextRequest, NextResponse } from "next/server";
import { requireStoreId } from "@/lib/server/auth";
import { withTransaction } from "@/lib/server/db";
import { jsonError, notFound } from "@/lib/server/errors";
import { toCustomerResponse } from "@/lib/server/mappers";
import { normalizePositiveAmount } from "@/lib/server/validation";

type CustomerRow = {
  id: number;
  store_id: number;
  name: string;
  phone_number: string;
  current_balance: string | number;
  pay_token: string;
};

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function POST(req: NextRequest) {
  try {
    const storeId = await requireStoreId(req);
    const body = await req.json();
    const customerId = Number(body.customerId);
    const amount = normalizePositiveAmount(body.amount);
    const note =
      typeof body.note === "string" && body.note.trim() ? body.note.trim() : null;

    const updated = await withTransaction(async (client) => {
      const customer = await client.query<CustomerRow>(
        `SELECT id, store_id, name, phone_number, current_balance, pay_token
         FROM customers
         WHERE id = $1
         FOR UPDATE`,
        [customerId]
      );
      if (customer.rowCount === 0 || customer.rows[0].store_id !== storeId) {
        notFound(`Customer not found: ${customerId}`);
      }
      await client.query(
        "INSERT INTO ledger_entries (customer_id, type, amount, note) VALUES ($1, 'CREDIT', $2, $3)",
        [customerId, amount, note]
      );
      const next = await client.query<CustomerRow>(
        `UPDATE customers
         SET current_balance = current_balance - $1::numeric
         WHERE id = $2
         RETURNING id, store_id, name, phone_number, current_balance, pay_token`,
        [amount, customerId]
      );
      return next.rows[0];
    });

    return NextResponse.json(toCustomerResponse(updated));
  } catch (error) {
    return jsonError(error);
  }
}
