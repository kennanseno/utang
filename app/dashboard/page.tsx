"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api, clearToken, formatPeso, getToken, Customer } from "@/lib/api";
import { Logo } from "../Logo";

const CUSTOMER_PAGE_SIZE = 10;

export default function DashboardPage() {
  const router = useRouter();
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [storeName, setStoreName] = useState<string>("");
  const [hasQrCode, setHasQrCode] = useState(true);
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [adding, setAdding] = useState(false);
  const [search, setSearch] = useState("");
  const [visibleCount, setVisibleCount] = useState(CUSTOMER_PAGE_SIZE);
  const [confirmLogout, setConfirmLogout] = useState(false);

  useEffect(() => {
    if (!getToken()) {
      router.replace("/");
      return;
    }
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function load() {
    setError(null);
    try {
      const [me, list] = await Promise.all([api.me(), api.listCustomers()]);
      setStoreName(me.name);
      setHasQrCode(me.hasQrCode);
      setCustomers(list);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }

  async function handleAdd() {
    if (!name.trim()) return;
    setAdding(true);
    setError(null);
    try {
      await api.createCustomer(name.trim(), phone.trim() || undefined);
      setName("");
      setPhone("");
      await load();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setAdding(false);
    }
  }

  function logout() {
    clearToken();
    router.replace("/");
  }

  const totalOwed = customers.reduce(
    (sum, c) => sum + Math.max(0, c.currentBalance),
    0
  );

  const query = search.trim().toLowerCase();
  const visibleCustomers = (
    query
      ? customers.filter((c) => c.name.toLowerCase().includes(query))
      : customers
  )
    .slice()
    .sort((a, b) => b.currentBalance - a.currentBalance);

  const pagedCustomers = visibleCustomers.slice(0, visibleCount);

  return (
    <>
      <div className="topbar">
        <h1>
          <span className="brand-lockup">
            <Logo />
            <span className="brand">Utang</span>
          </span>{" "}
          <span className="muted" style={{ fontSize: 14 }}>
            {storeName}
          </span>
        </h1>
        <span style={{ display: "flex", gap: 12 }}>
          <Link className="link" href="/settings">
            Store details
          </Link>
          <a className="link" onClick={() => setConfirmLogout(true)}>
            Logout
          </a>
        </span>
      </div>

      {confirmLogout && (
        <div className="card">
          <strong>Log out?</strong>
          <p className="muted">You&apos;ll need to sign in again next time.</p>
          <div className="row">
            <button onClick={logout}>Yes, log out</button>
            <button
              className="secondary"
              onClick={() => setConfirmLogout(false)}
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      <div className="card">
        <div className="muted">Total nakautang sa iyo</div>
        <div className={`balance ${totalOwed > 0 ? "owed" : "clear"}`}>
          {formatPeso(totalOwed)}
        </div>
      </div>
      {!loading && !hasQrCode && (
        <div className="notice">
          <strong>Add your payment QR code</strong>
          <p style={{ margin: "6px 0 10px" }} className="muted">
            Some features are disabled until you upload your GCash/Maya QR code.
            Your suki won&apos;t be able to scan and pay you until it&apos;s
            added.
          </p>
          <Link className="link" href="/settings">
            Add QR code in Store details →
          </Link>
        </div>
      )}

      <div className="card">
        <strong>Add suki</strong>
        <label htmlFor="name">Name</label>
        <input
          id="name"
          placeholder="Pangalan (required)"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <label htmlFor="cphone">Mobile number</label>
        <input
          id="cphone"
          type="tel"
          inputMode="tel"
          placeholder="09XX XXX XXXX (optional)"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
        />
        <p className="muted" style={{ marginTop: -4, marginBottom: 8 }}>
          Add a mobile number to text reminders directly. Without one, you can
          still copy the reminder message.
        </p>
        <button onClick={handleAdd} disabled={adding || !name.trim()}>
          {adding ? "Adding…" : "Add suki"}
        </button>
      </div>

      <strong>Mga suki</strong>
      {loading && <p className="muted">Loading…</p>}
      {!loading && customers.length === 0 && (
        <p className="muted">Wala pang suki. Add your first one above.</p>
      )}
      {!loading && customers.length > 0 && (
        <input
          type="search"
          placeholder="Search suki by name…"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setVisibleCount(CUSTOMER_PAGE_SIZE);
          }}
          style={{ marginTop: 8 }}
        />
      )}
      {!loading && customers.length > 0 && visibleCustomers.length === 0 && (
        <p className="muted" style={{ marginTop: 8 }}>
          No suki matched &ldquo;{search.trim()}&rdquo;.
        </p>
      )}
      <div style={{ marginTop: 8 }}>
        {pagedCustomers.map((c) => (
          <Link key={c.id} href={`/customers/${c.id}`} className="list-item">
            <div>
              <div className="name">{c.name}</div>
              {c.phoneNumber && <div className="muted">{c.phoneNumber}</div>}
            </div>
            <div className={`pill ${c.currentBalance > 0 ? "owed" : "clear"}`}>
              {formatPeso(c.currentBalance)}
            </div>
          </Link>
        ))}
      </div>
      {visibleCustomers.length > visibleCount && (
        <button
          className="secondary"
          onClick={() =>
            setVisibleCount((n) => n + CUSTOMER_PAGE_SIZE)
          }
          style={{ marginTop: 8 }}
        >
          Load more ({visibleCustomers.length - visibleCount} more)
        </button>
      )}

      {error && <p className="error">{error}</p>}
    </>
  );
}
