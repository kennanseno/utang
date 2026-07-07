import { NextRequest, NextResponse } from "next/server";
import { query } from "@/lib/server/db";
import { jsonError } from "@/lib/server/errors";

type QrRow = {
  qr_code_image: Buffer | null;
  qr_code_content_type: string | null;
};

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET(
  _req: NextRequest,
  { params }: { params: { token: string } }
) {
  try {
    const token = params.token;
    const row = await query<QrRow>(
      `SELECT s.qr_code_image, s.qr_code_content_type
       FROM customers c
       JOIN stores s ON s.id = c.store_id
       WHERE c.pay_token = $1`,
      [token]
    );
    if (row.rowCount === 0) {
      return new NextResponse(null, { status: 404 });
    }
    const qr = row.rows[0];
    if (!qr.qr_code_image || !qr.qr_code_content_type) {
      return new NextResponse(null, { status: 404 });
    }
    return new NextResponse(qr.qr_code_image, {
      status: 200,
      headers: { "Content-Type": qr.qr_code_content_type },
    });
  } catch (error) {
    return jsonError(error);
  }
}
