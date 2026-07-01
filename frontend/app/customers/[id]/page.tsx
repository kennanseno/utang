"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import {
  api,
  formatPeso,
  getToken,
  Customer,
  Ledger,
} from "@/lib/api";

export default function CustomerPage() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const customerId = Number(params.id);

  const [customer, setCustomer] = useState<Customer | null>(null);
  const [ledger, setLedger] = useState<Ledger | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const [utang, setUtang] = useState("");
  const [utangNote, setUtangNote] = useState("");
  const [bayad, setBayad] = useState("");

  const [reminderOpen, setReminderOpen] = useState(false);
  const [reminderMsg, setReminderMsg] = useState("");
  const [canRemind, setCanRemind] = useState(true);

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
      const [c, l] = await Promise.all([
        api.getCustomer(customerId),
        api.getLedger(customerId),
      ]);
      setCustomer(c);
      setLedger(l);
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function addUtang() {
    const amount = Number(utang);
    if (!amount || amount <= 0) return;
    setError(null);
    try {
      await api.debit(customerId, amount, utangNote.trim() || undefined);
      setUtang("");
      setUtangNote("");
      await load();
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function recordBayad() {
    const amount = Number(bayad);
    if (!amount || amount <= 0) return;
    setError(null);
    try {
      await api.credit(customerId, amount, "Cash");
      setBayad("");
      await load();
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function openReminder() {
    setError(null);
    setNotice(null);
    try {
      const preview = await api.reminderPreview(customerId);
      setReminderMsg(preview.message);
      setCanRemind(preview.canSendToday);
      setReminderOpen(true);
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function copyReminder() {
    try {
      await navigator.clipboard.writeText(reminderMsg);
      // Copy counts as a reminder — log it (enforces once-per-day lock).
      await api.remind(customerId);
      setNotice("Message copied! Paste it in SMS or Messenger.");
      setCanRemind(false);
      setReminderOpen(false);
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function sendPaymentLink() {
    if (!customer) return;
    setError(null);
    setNotice(null);
    try {
      const link = await api.createPaymentLink(
        customerId,
        Math.max(0, customer.currentBalance)
      );
      await navigator.clipboard.writeText(link.checkoutUrl);
      setNotice("Payment link copied to clipboard.");
    } catch (e) {
      setError((e as Error).message);
    }
  }

  if (!customer) {
    return (
      <>
        <div className="topbar">
          <Link className="link" href="/dashboard">
            ← Back
          </Link>
        </div>
        {error ? <p className="error">{error}</p> : <p className="muted">Loading…</p>}
      </>
    );
  }

  const owed = customer.currentBalance > 0;

  return (
    <>
      <div className="topbar">
        <Link className="link" href="/dashboard">
          ← Back
        </Link>
      </div>

      <div className="card">
        <div className="name" style={{ fontSize: 18, fontWeight: 700 }}>
          {customer.name}
        </div>
        {customer.phoneNumber && (
          <div className="muted">{customer.phoneNumber}</div>
        )}
        <div className="muted" style={{ marginTop: 12 }}>
          Balance
        </div>
        <div className={`balance ${owed ? "owed" : "clear"}`}>
          {formatPeso(customer.currentBalance)}
        </div>
      </div>

      <div className="card">
        <strong>Add utang</strong>
        <label htmlFor="utang">Amount (₱)</label>
        <input
          id="utang"
          type="number"
          inputMode="decimal"
          placeholder="0.00"
          value={utang}
          onChange={(e) => setUtang(e.target.value)}
        />
        <label htmlFor="note">Note (optional)</label>
        <input
          id="note"
          placeholder="e.g. sardinas, load"
          value={utangNote}
          onChange={(e) => setUtangNote(e.target.value)}
        />
        <button onClick={addUtang} disabled={!Number(utang)}>
          Add utang
        </button>
      </div>

      <div className="card">
        <strong>Record bayad (cash)</strong>
        <label htmlFor="bayad">Amount (₱)</label>
        <input
          id="bayad"
          type="number"
          inputMode="decimal"
          placeholder="0.00"
          value={bayad}
          onChange={(e) => setBayad(e.target.value)}
        />
        <button
          className="secondary"
          onClick={recordBayad}
          disabled={!Number(bayad)}
        >
          Record cash payment
        </button>
      </div>

      <div className="card">
        <div className="row">
          <button onClick={openReminder}>Remind</button>
          <button
            className="secondary"
            onClick={sendPaymentLink}
            disabled={!owed}
          >
            Send pay link
          </button>
        </div>
      </div>

      {reminderOpen && (
        <div className="card">
          <strong>Reminder message</strong>
          <p className="muted">Edit before copying if you like.</p>
          <textarea
            value={reminderMsg}
            onChange={(e) => setReminderMsg(e.target.value)}
          />
          {!canRemind && (
            <p className="muted">
              You already reminded this suki today. Copying again is allowed but
              won&apos;t be logged twice.
            </p>
          )}
          <button onClick={copyReminder}>Copy message</button>
          <button
            className="secondary"
            onClick={() => setReminderOpen(false)}
          >
            Cancel
          </button>
        </div>
      )}

      <div className="card">
        <strong>Ledger history</strong>
        {ledger && ledger.entries.length === 0 && (
          <p className="muted">No entries yet.</p>
        )}
        {ledger?.entries.map((e) => (
          <div key={e.id} className="entry">
            <div>
              <div>{e.type === "DEBIT" ? "Utang" : "Bayad"}</div>
              {e.note && <div className="muted">{e.note}</div>}
            </div>
            <div
              className={`amt ${e.type === "DEBIT" ? "debit" : "credit"}`}
            >
              {e.type === "DEBIT" ? "+" : "−"}
              {formatPeso(e.amount)}
            </div>
          </div>
        ))}
      </div>

      {notice && <p className="link center">{notice}</p>}
      {error && <p className="error">{error}</p>}
    </>
  );
}
