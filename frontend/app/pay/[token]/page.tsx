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
  const [notifyBusy, setNotifyBusy] = useState(false);
  const [notifyDone, setNotifyDone] = useState(false);
  const [notifyError, setNotifyError] = useState<string | null>(null);

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

  async function notifyPaid() {
    setNotifyBusy(true);
    setNotifyError(null);
    try {
      await api.notifyPaid(token);
      setNotifyDone(true);
    } catch (e) {
      setNotifyError((e as Error).message);
    } finally {
      setNotifyBusy(false);
    }
  }

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

      {notifyDone ? (
        <p className="link center" style={{ marginTop: 16 }}>
          Naipaalam na sa tindera na nakabayad ka na. Salamat!
        </p>
      ) : (
        <>
          <button
            className="secondary"
            onClick={notifyPaid}
            disabled={notifyBusy}
            style={{ marginTop: 16 }}
          >
            {notifyBusy ? "Sinasabi sa tindera…" : "Nakabayad na ako"}
          </button>
          <p className="muted center" style={{ marginTop: 8 }}>
            I-text ang tindera na nakapagbayad ka na.
          </p>
        </>
      )}
      {notifyError && <p className="error center">{notifyError}</p>}

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
