"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api, clearToken, formatPeso, getToken, Customer } from "@/lib/api";

export default function DashboardPage() {
  const router = useRouter();
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [storeName, setStoreName] = useState<string>("");
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [adding, setAdding] = useState(false);

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

  return (
    <>
      <div className="topbar">
        <h1>
          <span className="brand">Utang</span>{" "}
          <span className="muted" style={{ fontSize: 14 }}>
            {storeName}
          </span>
        </h1>
        <a className="link" onClick={logout}>
          Logout
        </a>
      </div>

      <div className="card">
        <div className="muted">Total nakautang sa iyo</div>
        <div className={`balance ${totalOwed > 0 ? "owed" : "clear"}`}>
          {formatPeso(totalOwed)}
        </div>
      </div>

      <div className="card">
        <strong>Add suki</strong>
        <label htmlFor="name">Name</label>
        <input
          id="name"
          placeholder="Pangalan (required)"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <label htmlFor="cphone">Phone (optional)</label>
        <input
          id="cphone"
          type="tel"
          inputMode="tel"
          placeholder="09XX XXX XXXX"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
        />
        <button onClick={handleAdd} disabled={adding || !name.trim()}>
          {adding ? "Adding…" : "Add suki"}
        </button>
      </div>

      <strong>Mga suki</strong>
      {loading && <p className="muted">Loading…</p>}
      {!loading && customers.length === 0 && (
        <p className="muted">Wala pang suki. Add your first one above.</p>
      )}
      <div style={{ marginTop: 8 }}>
        {customers.map((c) => (
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

      {error && <p className="error">{error}</p>}
    </>
  );
}
