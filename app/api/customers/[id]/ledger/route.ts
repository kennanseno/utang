import { NextRequest, NextResponse } from "next/server";
import { requireStoreId } from "@/lib/server/auth";
import { query } from "@/lib/server/db";
import { jsonError, notFound } from "@/lib/server/errors";
import { toLedgerEntryResponse } from "@/lib/server/mappers";

type CustomerBalanceRow = {
  id: number;
  current_balance: string | number;
};

type LedgerRow = {
  id: number;
  type: "DEBIT" | "CREDIT";
  amount: string | number;
  note: string | null;
  created_at: string;
};

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET(
  req: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const storeId = await requireStoreId(req);
    const customerId = Number(params.id);
    const customer = await query<CustomerBalanceRow>(
      "SELECT id, current_balance FROM customers WHERE id = $1 AND store_id = $2",
      [customerId, storeId]
    );
    if (customer.rowCount === 0) {
      notFound(`Customer not found: ${customerId}`);
    }

    const search = req.nextUrl.searchParams;
    const page = Math.max(0, Number(search.get("page") ?? "0") || 0);
    const size = Math.min(
      100,
      Math.max(1, Number(search.get("size") ?? "10") || 10)
    );
    const offset = page * size;

    const [entries, total] = await Promise.all([
      query<LedgerRow>(
        `SELECT id, type, amount, note, created_at
         FROM ledger_entries
         WHERE customer_id = $1
         ORDER BY created_at DESC
         LIMIT $2 OFFSET $3`,
        [customerId, size, offset]
      ),
      query<{ total: string }>(
        "SELECT COUNT(*)::text AS total FROM ledger_entries WHERE customer_id = $1",
        [customerId]
      ),
    ]);

    const totalEntries = Number(total.rows[0].total);
    return NextResponse.json({
      customerId,
      currentBalance: Number(customer.rows[0].current_balance),
      entries: entries.rows.map(toLedgerEntryResponse),
      page,
      size,
      totalEntries,
      hasMore: offset + entries.rows.length < totalEntries,
    });
  } catch (error) {
    return jsonError(error);
  }
}
