// Typed client for the Utang backend API.

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

const TOKEN_KEY = "utang.token";
const DEVICE_KEY = "utang.device";

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

/**
 * Stable per-browser device identifier. Trusted devices skip the OTP step on
 * login; a new browser/device must verify by OTP the first time.
 */
export function getDeviceId(): string {
  if (typeof window === "undefined") return "";
  let id = window.localStorage.getItem(DEVICE_KEY);
  if (!id) {
    id =
      typeof crypto !== "undefined" && "randomUUID" in crypto
        ? crypto.randomUUID()
        : `dev-${Date.now()}-${Math.random().toString(36).slice(2)}`;
    window.localStorage.setItem(DEVICE_KEY, id);
  }
  return id;
}

export interface Store {
  id: number;
  username: string | null;
  phoneNumber: string;
  name: string;
  ownerName: string | null;
  onboarded: boolean;
  phoneVerified: boolean;
  hasQrCode: boolean;
}

export interface AuthResult {
  token: string;
  store: Store;
}

export type LoginResult =
  | { status: "AUTHENTICATED"; token: string; store: Store }
  | { status: "OTP_REQUIRED"; phoneNumber: string; devCode: string | null };

export interface PhoneVerification {
  phoneNumber: string;
  devCode: string | null;
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
}

export interface PublicPay {
  storeName: string;
  storePhoneNumber: string;
  customerName: string;
  outstandingBalance: number;
  storeHasQrCode: boolean;
  history: PublicLedgerEntry[];
}

export interface PublicLedgerEntry {
  type: "DEBIT" | "CREDIT";
  amount: number;
  note: string | null;
  createdAt: string;
}

async function request<T>(
  path: string,
  options: RequestInit = {},
  auth = true
): Promise<T> {
  const isForm = options.body instanceof FormData;
  const headers: Record<string, string> = {
    "X-Device-Id": getDeviceId(),
    // Let the browser set the multipart boundary for FormData bodies.
    ...(isForm ? {} : { "Content-Type": "application/json" }),
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
  register: (input: {
    username: string;
    password: string;
    phoneNumber: string;
    storeName: string;
    ownerName?: string;
  }) =>
    request<AuthResult>(
      "/auth/register",
      {
        method: "POST",
        body: JSON.stringify({
          username: input.username,
          password: input.password,
          phoneNumber: input.phoneNumber,
          storeName: input.storeName,
          ownerName: input.ownerName || null,
        }),
      },
      false
    ),

  login: (username: string, password: string) =>
    request<LoginResult>(
      "/auth/login",
      { method: "POST", body: JSON.stringify({ username, password }) },
      false
    ),

  verifyDevice: (username: string, code: string) =>
    request<AuthResult>(
      "/auth/verify-device",
      { method: "POST", body: JSON.stringify({ username, code }) },
      false
    ),

  updateStore: (storeName: string, ownerName: string | undefined, phoneNumber: string) =>
    request<Store>("/store", {
      method: "PUT",
      body: JSON.stringify({
        storeName,
        ownerName: ownerName || null,
        phoneNumber,
      }),
    }),

  requestPhoneVerification: () =>
    request<PhoneVerification>("/store/phone/verify/request", {
      method: "POST",
    }),

  confirmPhoneVerification: (code: string) =>
    request<Store>("/store/phone/verify/confirm", {
      method: "POST",
      body: JSON.stringify({ code }),
    }),

  uploadQrCode: (file: File) => {
    const form = new FormData();
    form.append("file", file);
    return request<Store>("/store/qr", { method: "PUT", body: form });
  },

  removeQrCode: () => request<Store>("/store/qr", { method: "DELETE" }),

  me: () => request<Store>("/me"),

  listCustomers: () => request<Customer[]>("/customers"),

  getCustomer: (id: number) => request<Customer>(`/customers/${id}`),

  createCustomer: (name: string, phoneNumber: string) =>
    request<Customer>("/customers", {
      method: "POST",
      body: JSON.stringify({ name, phoneNumber }),
    }),

  updateCustomer: (id: number, name: string, phoneNumber: string) =>
    request<Customer>(`/customers/${id}`, {
      method: "PUT",
      body: JSON.stringify({ name, phoneNumber }),
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

  publicPay: (token: string) =>
    request<PublicPay>(`/public/pay/${token}`, {}, false),
};

export function formatPeso(amount: number): string {
  return new Intl.NumberFormat("en-PH", {
    style: "currency",
    currency: "PHP",
  }).format(amount);
}

/**
 * Fetches the store's QR code image (which requires an auth header) and returns
 * an object URL usable as an <img src>. Returns null when no QR is uploaded.
 * Callers should URL.revokeObjectURL the result when done.
 */
export async function fetchQrCodeUrl(): Promise<string | null> {
  const headers: Record<string, string> = { "X-Device-Id": getDeviceId() };
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(`${API_BASE_URL}/store/qr`, { headers });
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`Request failed (${res.status})`);
  const blob = await res.blob();
  return URL.createObjectURL(blob);
}

/**
 * Fetches a store's payment QR code for the public pay page (no auth). Returns
 * an object URL usable as an <img src>, or null when the store has none.
 * Callers should URL.revokeObjectURL the result when done.
 */
export async function fetchPublicQrCodeUrl(
  token: string
): Promise<string | null> {
  const res = await fetch(`${API_BASE_URL}/public/pay/${token}/qr`);
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`Request failed (${res.status})`);
  const blob = await res.blob();
  return URL.createObjectURL(blob);
}