import { NextRequest, NextResponse } from "next/server";
import { query } from "@/lib/server/db";
import { jsonError, notFound } from "@/lib/server/errors";

type PublicRow = {
  customer_id: number;
  customer_name: string;
  current_balance: string | number;
  store_name: string;
  store_phone_number: string;
  store_has_qr: boolean;
};

type LedgerRow = {
  type: "DEBIT" | "CREDIT";
  amount: string | number;
  note: string | null;
  created_at: string;
};

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET(
  req: NextRequest,
  { params }: { params: { token: string } }
) {
  try {
    const token = params.token;
    const base = await query<PublicRow>(
      `SELECT
         c.id AS customer_id,
         c.name AS customer_name,
         c.current_balance,
         s.name AS store_name,
         s.phone_number AS store_phone_number,
         (s.qr_code_content_type IS NOT NULL AND s.qr_code_content_type <> '') AS store_has_qr
       FROM customers c
       JOIN stores s ON s.id = c.store_id
       WHERE c.pay_token = $1`,
      [token]
    );
    if (base.rowCount === 0) {
      notFound(
        "Payment page not found. The link may be invalid, expired, or the customer record no longer exists."
      );
    }
    const row = base.rows[0];

    const search = req.nextUrl.searchParams;
    const page = Math.max(0, Number(search.get("page") ?? "0") || 0);
    const size = Math.min(
      100,
      Math.max(1, Number(search.get("size") ?? "10") || 10)
    );
    const offset = page * size;

    const [history, total] = await Promise.all([
      query<LedgerRow>(
        `SELECT type, amount, note, created_at
         FROM ledger_entries
         WHERE customer_id = $1
         ORDER BY created_at DESC
         LIMIT $2 OFFSET $3`,
        [row.customer_id, size, offset]
      ),
      query<{ total: string }>(
        "SELECT COUNT(*)::text AS total FROM ledger_entries WHERE customer_id = $1",
        [row.customer_id]
      ),
    ]);

    const totalHistory = Number(total.rows[0].total);
    return NextResponse.json({
      storeName: row.store_name,
      storePhoneNumber: row.store_phone_number,
      customerName: row.customer_name,
      outstandingBalance: Number(row.current_balance),
      storeHasQrCode: row.store_has_qr,
      history: history.rows.map((entry) => ({
        type: entry.type,
        amount: Number(entry.amount),
        note: entry.note,
        createdAt: entry.created_at,
      })),
      page,
      size,
      totalHistory,
      hasMore: offset + history.rows.length < totalHistory,
    });
  } catch (error) {
    return jsonError(error);
  }
}
