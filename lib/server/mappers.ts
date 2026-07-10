type StoreRow = {
  id: number;
  username: string | null;
  phone_number: string;
  email: string | null;
  name: string;
  owner_name: string | null;
  onboarded: boolean;
  qr_code_content_type: string | null;
};

type CustomerRow = {
  id: number;
  name: string;
  phone_number: string | null;
  current_balance: string | number;
  pay_token: string;
};

type LedgerRow = {
  id: number;
  type: "DEBIT" | "CREDIT";
  amount: string | number;
  note: string | null;
  created_at: string;
};

export function toStoreResponse(row: StoreRow) {
  return {
    id: row.id,
    username: row.username,
    phoneNumber: row.phone_number,
    email: row.email,
    name: row.name,
    ownerName: row.owner_name,
    onboarded: row.onboarded,
    hasQrCode: !!row.qr_code_content_type,
  };
}

export function toCustomerResponse(row: CustomerRow) {
  return {
    id: row.id,
    name: row.name,
    phoneNumber: row.phone_number,
    currentBalance: Number(row.current_balance),
    payToken: row.pay_token,
  };
}

export function toLedgerEntryResponse(row: LedgerRow) {
  return {
    id: row.id,
    type: row.type,
    amount: Number(row.amount),
    note: row.note,
    createdAt: row.created_at,
  };
}
