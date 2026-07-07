import { NextRequest, NextResponse } from "next/server";
import { requireStoreId } from "@/lib/server/auth";
import { query } from "@/lib/server/db";
import { jsonError, notFound } from "@/lib/server/errors";

type ReminderRow = {
  customer_name: string;
  current_balance: string | number;
  pay_token: string;
  store_name: string;
};

const AMOUNT_FORMAT = new Intl.NumberFormat("en-PH", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET(
  req: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const storeId = await requireStoreId(req);
    const customerId = Number(params.id);
    const row = await query<ReminderRow>(
      `SELECT c.name AS customer_name, c.current_balance, c.pay_token, s.name AS store_name
       FROM customers c
       JOIN stores s ON s.id = c.store_id
       WHERE c.id = $1 AND c.store_id = $2`,
      [customerId, storeId]
    );
    if (row.rowCount === 0) {
      notFound(`Customer not found: ${customerId}`);
    }

    const reminder = row.rows[0];
    const amount = Math.max(0, Number(reminder.current_balance));
    const publicBaseUrl =
      process.env.PUBLIC_BASE_URL ??
      process.env.NEXT_PUBLIC_PUBLIC_BASE_URL ??
      "http://localhost:3000";
    const message =
      `Hi ${reminder.customer_name}! May utang ka na ₱${AMOUNT_FORMAT.format(amount)} sa ${reminder.store_name}.\n` +
      `Pwede ka magbayad dito: ${publicBaseUrl}/pay/${reminder.pay_token}`;

    return NextResponse.json({ message });
  } catch (error) {
    return jsonError(error);
  }
}
