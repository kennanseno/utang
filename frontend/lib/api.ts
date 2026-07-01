// Typed client for the Utang backend API.

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

const TOKEN_KEY = "utang.token";

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(TOKEN_KEY);
}

export interface Store {
  id: number;
  phoneNumber: string;
  name: string;
}

export interface Customer {
  id: number;
  name: string;
  phoneNumber: string | null;
  currentBalance: number;
  payToken: string;
}

export interface LedgerEntry {
  id: number;
  type: "DEBIT" | "CREDIT";
  amount: number;
  note: string | null;
  createdAt: string;
}

export interface Ledger {
  customerId: number;
  currentBalance: number;
  entries: LedgerEntry[];
}

export interface ReminderPreview {
  message: string;
  canSendToday: boolean;
}

export interface PublicPay {
  storeName: string;
  customerName: string;
  outstandingBalance: number;
  checkoutUrl: string | null;
}

async function request<T>(
  path: string,
  options: RequestInit = {},
  auth = true
): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string> | undefined),
  };
  if (auth) {
    const token = getToken();
    if (token) headers.Authorization = `Bearer ${token}`;
  }

  const res = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });
  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const body = await res.json();
      if (body?.message) message = body.message;
    } catch {
      // ignore parse errors
    }
    throw new Error(message);
  }
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export const api = {
  requestOtp: (phoneNumber: string) =>
    request<{ phoneNumber: string; devCode: string; message: string }>(
      "/auth/request-otp",
      { method: "POST", body: JSON.stringify({ phoneNumber }) },
      false
    ),

  verifyOtp: (phoneNumber: string, code: string) =>
    request<{ token: string; store: Store }>(
      "/auth/verify-otp",
      { method: "POST", body: JSON.stringify({ phoneNumber, code }) },
      false
    ),

  me: () => request<Store>("/me"),

  listCustomers: () => request<Customer[]>("/customers"),

  getCustomer: (id: number) => request<Customer>(`/customers/${id}`),

  createCustomer: (name: string, phoneNumber?: string) =>
    request<Customer>("/customers", {
      method: "POST",
      body: JSON.stringify({ name, phoneNumber: phoneNumber || null }),
    }),

  debit: (customerId: number, amount: number, note?: string) =>
    request<Customer>("/ledger/debit", {
      method: "POST",
      body: JSON.stringify({ customerId, amount, note: note || null }),
    }),

  credit: (customerId: number, amount: number, note?: string) =>
    request<Customer>("/ledger/credit", {
      method: "POST",
      body: JSON.stringify({ customerId, amount, note: note || null }),
    }),

  getLedger: (customerId: number) =>
    request<Ledger>(`/customers/${customerId}/ledger`),

  reminderPreview: (customerId: number) =>
    request<ReminderPreview>(`/customers/${customerId}/reminder-preview`),

  remind: (customerId: number) =>
    request<{ message: string; sent: boolean }>(
      `/customers/${customerId}/remind`,
      { method: "POST" }
    ),

  createPaymentLink: (customerId: number, amount: number) =>
    request<{ referenceId: string; amount: number; checkoutUrl: string }>(
      "/payments/link",
      { method: "POST", body: JSON.stringify({ customerId, amount }) }
    ),

  publicPay: (token: string) =>
    request<PublicPay>(`/public/pay/${token}`, {}, false),
};

export function formatPeso(amount: number): string {
  return new Intl.NumberFormat("en-PH", {
    style: "currency",
    currency: "PHP",
  }).format(amount);
}
