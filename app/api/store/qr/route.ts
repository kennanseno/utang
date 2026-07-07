import { NextRequest, NextResponse } from "next/server";
import { requireStoreId } from "@/lib/server/auth";
import { query } from "@/lib/server/db";
import { badRequest, jsonError } from "@/lib/server/errors";
import { toStoreResponse } from "@/lib/server/mappers";
import { getStoreById } from "@/lib/server/store";

const QR_MAX_BYTES = 2 * 1024 * 1024;
const QR_ACCEPTED = new Set([
  "image/png",
  "image/jpeg",
  "image/jpg",
  "image/webp",
  "image/gif",
]);

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  try {
    const storeId = await requireStoreId(req);
    const qr = await query<{ qr_code_image: Buffer | null; qr_code_content_type: string | null }>(
      "SELECT qr_code_image, qr_code_content_type FROM stores WHERE id = $1",
      [storeId]
    );
    if (qr.rowCount === 0) {
      return new NextResponse(null, { status: 404 });
    }
    const row = qr.rows[0];
    if (!row.qr_code_image || !row.qr_code_content_type) {
      return new NextResponse(null, { status: 404 });
    }
    return new NextResponse(row.qr_code_image, {
      status: 200,
      headers: { "Content-Type": row.qr_code_content_type },
    });
  } catch (error) {
    return jsonError(error);
  }
}

export async function PUT(req: NextRequest) {
  try {
    const storeId = await requireStoreId(req);
    const form = await req.formData();
    const file = form.get("file");
    if (!(file instanceof File) || file.size === 0) {
      badRequest("QR code image is required");
    }
    if (!QR_ACCEPTED.has(file.type)) {
      badRequest("QR code must be a PNG, JPG, WebP or GIF image");
    }
    if (file.size > QR_MAX_BYTES) {
      badRequest("QR code image must be 2 MB or smaller");
    }
    const type = file.type === "image/jpg" ? "image/jpeg" : file.type;
    const bytes = Buffer.from(await file.arrayBuffer());
    await query(
      "UPDATE stores SET qr_code_image = $1, qr_code_content_type = $2 WHERE id = $3",
      [bytes, type, storeId]
    );
    const store = await getStoreById(storeId);
    return NextResponse.json(toStoreResponse(store));
  } catch (error) {
    return jsonError(error);
  }
}

export async function DELETE(req: NextRequest) {
  try {
    const storeId = await requireStoreId(req);
    await query(
      "UPDATE stores SET qr_code_image = NULL, qr_code_content_type = NULL WHERE id = $1",
      [storeId]
    );
    const store = await getStoreById(storeId);
    return NextResponse.json(toStoreResponse(store));
  } catch (error) {
    return jsonError(error);
  }
}
