"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { api, formatPeso, PublicPay } from "@/lib/api";

export default function PublicPayPage() {
  const params = useParams<{ token: string }>();
  const token = params.token;
  const [data, setData] = useState<PublicPay | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const res = await api.publicPay(token);
        setData(res);
      } catch (e) {
        setError((e as Error).message);
      } finally {
        setLoading(false);
      }
    }
    void load();
  }, [token]);

  if (loading) {
    return <p className="muted center">Loading…</p>;
  }

  if (error || !data) {
    return <p className="error center">{error ?? "Payment page not found."}</p>;
  }

  const hasBalance = data.outstandingBalance > 0;

  return (
    <>
      <div className="topbar">
        <h1>
          <span className="brand">Utang</span>
        </h1>
      </div>

      <div className="card center">
        <div className="muted">Bayad kay</div>
        <div style={{ fontSize: 20, fontWeight: 700, margin: "4px 0 16px" }}>
          {data.storeName}
        </div>
        <div className="muted">Utang ni {data.customerName}</div>
        <div className={`balance ${hasBalance ? "owed" : "clear"}`}>
          {formatPeso(data.outstandingBalance)}
        </div>
      </div>

      {hasBalance && data.checkoutUrl ? (
        <a href={data.checkoutUrl} target="_blank" rel="noopener noreferrer">
          <button>Magbayad online</button>
        </a>
      ) : (
        <div className="card center muted">Walang utang. Salamat!</div>
      )}

      <p className="muted center" style={{ marginTop: 16 }}>
        Secure payment powered by PayMongo.
      </p>
    </>
  );
}
