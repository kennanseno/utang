import { NextRequest, NextResponse } from "next/server";
import { requireStoreId } from "@/lib/server/auth";
import { jsonError } from "@/lib/server/errors";
import { toStoreResponse } from "@/lib/server/mappers";
import { getStoreById } from "@/lib/server/store";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  try {
    const storeId = await requireStoreId(req);
    const store = await getStoreById(storeId);
    return NextResponse.json(toStoreResponse(store));
  } catch (error) {
    return jsonError(error);
  }
}
