import type { PoolClient } from "pg";
import { query } from "./db";
import { notFound } from "./errors";

export type StoreRow = {
  id: number;
  username: string | null;
  password_hash: string | null;
  phone_number: string;
  email: string | null;
  name: string;
  owner_name: string | null;
  onboarded: boolean;
  qr_code_content_type: string | null;
};

export async function getStoreById(storeId: number): Promise<StoreRow> {
  const res = await query<StoreRow>(
    `SELECT id, username, password_hash, phone_number, email, name, owner_name, onboarded, qr_code_content_type
     FROM stores WHERE id = $1`,
    [storeId]
  );
  if (res.rowCount === 0) {
    notFound("Store not found");
  }
  return res.rows[0];
}

export async function getStoreByIdWithClient(
  client: PoolClient,
  storeId: number
): Promise<StoreRow> {
  const res = await client.query<StoreRow>(
    `SELECT id, username, password_hash, phone_number, email, name, owner_name, onboarded, qr_code_content_type
     FROM stores WHERE id = $1`,
    [storeId]
  );
  if (res.rowCount === 0) {
    notFound("Store not found");
  }
  return res.rows[0];
}
