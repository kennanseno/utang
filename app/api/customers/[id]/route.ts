import { NextRequest, NextResponse } from "next/server";
import { requireStoreId } from "@/lib/server/auth";
import { query } from "@/lib/server/db";
import { jsonError, notFound } from "@/lib/server/errors";
import { toCustomerResponse } from "@/lib/server/mappers";
import { toE164 } from "@/lib/server/phone";
import { normalizeCustomerName } from "@/lib/server/validation";

type CustomerRow = {
  id: number;
  name: string;
  phone_number: string;
  current_balance: string | number;
  pay_token: string;
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
    const customer = await loadOwnedCustomer(storeId, customerId);
    return NextResponse.json(toCustomerResponse(customer));
  } catch (error) {
    return jsonError(error);
  }
}

export async function PUT(
  req: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const storeId = await requireStoreId(req);
    const customerId = Number(params.id);
    await loadOwnedCustomer(storeId, customerId);

    const body = await req.json();
    const name = normalizeCustomerName(body.name);
    const phoneNumber = toE164(body.phoneNumber);
    const updated = await query<CustomerRow>(
      `UPDATE customers
       SET name = $1, phone_number = $2
       WHERE id = $3 AND store_id = $4
       RETURNING id, name, phone_number, current_balance, pay_token`,
      [name, phoneNumber, customerId, storeId]
    );
    return NextResponse.json(toCustomerResponse(updated.rows[0]));
  } catch (error) {
    return jsonError(error);
  }
}

export async function DELETE(
  req: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const storeId = await requireStoreId(req);
    const customerId = Number(params.id);
    await loadOwnedCustomer(storeId, customerId);
    await query("DELETE FROM ledger_entries WHERE customer_id = $1", [customerId]);
    await query("DELETE FROM customers WHERE id = $1 AND store_id = $2", [
      customerId,
      storeId,
    ]);
    return new NextResponse(null, { status: 204 });
  } catch (error) {
    return jsonError(error);
  }
}

async function loadOwnedCustomer(storeId: number, customerId: number) {
  if (!Number.isInteger(customerId) || customerId <= 0) {
    notFound(`Customer not found: ${customerId}`);
  }
  const customer = await query<CustomerRow>(
    `SELECT id, name, phone_number, current_balance, pay_token
     FROM customers
     WHERE id = $1 AND store_id = $2`,
    [customerId, storeId]
  );
  if (customer.rowCount === 0) {
    notFound(`Customer not found: ${customerId}`);
  }
  return customer.rows[0];
}
