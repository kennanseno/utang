"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { api, fetchPublicQrCodeUrl, formatPeso, PublicPay } from "@/lib/api";
import { Logo } from "../../Logo";

export default function PublicPayPage() {
  const params = useParams<{ token: string }>();
  const token = params.token;
  const [data, setData] = useState<PublicPay | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [qrUrl, setQrUrl] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [copyError, setCopyError] = useState<string | null>(null);

  useEffect(() => {
    async function load() {
      try {
        const res = await api.publicPay(token);
        setData(res);
        if (res.storeHasQrCode) {
          fetchPublicQrCodeUrl(token)
            .then(setQrUrl)
            .catch(() => {});
        }
      } catch (e) {
        setError((e as Error).message);
      } finally {
        setLoading(false);
      }
    }
    void load();
  }, [token]);

  // Release the object URL for the QR image when it changes or on unmount.
  useEffect(() => {
    return () => {
      if (qrUrl) URL.revokeObjectURL(qrUrl);
    };
  }, [qrUrl]);

  if (loading) {
    return <p className="muted center">Loading…</p>;
  }

  if (error || !data) {
    return <p className="error center">{error ?? "Payment page not found."}</p>;
  }

  const hasBalance = data.outstandingBalance > 0;
  const paidMessage = `Hi ${data.storeName}! Ako po si ${data.customerName}. Nakabayad na po ako sa aking utang${
    hasBalance ? ` na ${formatPeso(data.outstandingBalance)}` : ""
  }. Salamat po!`;
  const smsHref = `sms:${data.storePhoneNumber.replace(/\s/g, "")}?&body=${encodeURIComponent(
    paidMessage
  )}`;

  async function copyPaidMessage() {
    setCopyError(null);
    try {
      await navigator.clipboard.writeText(paidMessage);
      setCopied(true);
    } catch (e) {
      setCopyError((e as Error).message);
    }
  }

  return (
    <>
      <div className="topbar">
        <h1>
          <span className="brand-lockup">
            <Logo />
            <span className="brand">Utang</span>
          </span>
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

      {qrUrl && (
        <div className="card center">
          <strong>I-scan para magbayad</strong>
          <p className="muted" style={{ marginTop: 4 }}>
            Gamitin ang GCash o Maya app para i-scan ang QR code.
          </p>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src={qrUrl}
            alt={`${data.storeName} payment QR code`}
            style={{
              maxWidth: 260,
              width: "100%",
              height: "auto",
              margin: "12px auto 0",
              display: "block",
              borderRadius: 12,
            }}
          />
        </div>
      )}

      {!hasBalance && (
        <div className="card center muted">Walang utang. Salamat!</div>
      )}

      <div className="card">
        <strong>Nakabayad ka na? Ipaalam sa tindera</strong>
        <p className="muted" style={{ marginTop: 4 }}>
          Kopyahin ang mensahe at i-text sa tindera para malaman niyang
          nakabayad ka na.
        </p>

        <div
          style={{
            marginTop: 12,
            padding: 12,
            borderRadius: 12,
            background: "var(--bg)",
            border: "1px solid var(--border)",
            whiteSpace: "pre-wrap",
          }}
        >
          {paidMessage}
        </div>

        <a href={smsHref}>
          <button style={{ marginTop: 12 }}>I-text ang tindera</button>
        </a>

        <button className="secondary" onClick={copyPaidMessage}>
          {copied ? "✓ Nakopya na!" : "Kopyahin ang mensahe"}
        </button>

        <div className="center" style={{ marginTop: 12 }}>
          <div className="muted">Numero ng tindera</div>
          <a href={`tel:${data.storePhoneNumber.replace(/\s/g, "")}`}>
            <strong>{data.storePhoneNumber}</strong>
          </a>
        </div>

        {copyError && (
          <p className="error center" style={{ marginTop: 8 }}>
            {copyError}
          </p>
        )}
      </div>

      {data.history.length > 0 && (
        <div className="card">
          <strong>Transaction history</strong>
          <p className="muted" style={{ marginTop: 4 }}>
            Kompletong listahan ng utang at bayad.
          </p>
          <div style={{ marginTop: 8 }}>
            {data.history.map((e, i) => {
              const isDebit = e.type === "DEBIT";
              return (
                <div key={i} className="entry">
                  <div>
                    <div>{isDebit ? "Utang" : "Bayad"}</div>
                    {e.note && <div className="muted">{e.note}</div>}
                    <div className="muted">
                      {new Date(e.createdAt).toLocaleString("en-PH", {
                        dateStyle: "medium",
                        timeStyle: "short",
                      })}
                    </div>
                  </div>
                  <div className={`amt ${isDebit ? "debit" : "credit"}`}>
                    {isDebit ? "+" : "−"}
                    {formatPeso(e.amount)}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      <p className="muted center" style={{ marginTop: 16 }}>
        Secure payment powered by PayMongo.
      </p>
    </>
  );
}
