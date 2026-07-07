import { NextResponse } from "next/server";
import { query } from "@/lib/server/db";
import { jsonError } from "@/lib/server/errors";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET() {
  try {
    const [stores, customers, totals] = await Promise.all([
      query<{ value: string }>("SELECT COUNT(*)::text AS value FROM stores"),
      query<{ value: string }>("SELECT COUNT(*)::text AS value FROM customers"),
      query<{ total_recorded: string; total_collected: string }>(
        `SELECT
           COALESCE(SUM(CASE WHEN type = 'DEBIT' THEN amount ELSE 0 END), 0)::text AS total_recorded,
           COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE 0 END), 0)::text AS total_collected
         FROM ledger_entries`
      ),
    ]);

    return NextResponse.json({
      storeCount: Number(stores.rows[0].value),
      customerCount: Number(customers.rows[0].value),
      totalRecorded: Number(totals.rows[0].total_recorded),
      totalCollected: Number(totals.rows[0].total_collected),
    });
  } catch (error) {
    return jsonError(error);
  }
}
