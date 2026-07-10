"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import {
  api,
  formatPeso,
  getToken,
  Customer,
  LedgerEntry,
} from "@/lib/api";

const LEDGER_PAGE_SIZE = 10;

export default function CustomerPage() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const customerId = Number(params.id);

  const [customer, setCustomer] = useState<Customer | null>(null);
  const [entries, setEntries] = useState<LedgerEntry[]>([]);
  const [ledgerPage, setLedgerPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const [utang, setUtang] = useState("");
  const [utangNote, setUtangNote] = useState("");
  const [bayad, setBayad] = useState("");

  const [reminderOpen, setReminderOpen] = useState(false);
  const [reminderMsg, setReminderMsg] = useState("");

  const [editingPhone, setEditingPhone] = useState(false);
  const [nameInput, setNameInput] = useState("");
  const [phoneInput, setPhoneInput] = useState("");
  const [phoneSaving, setPhoneSaving] = useState(false);
  const [confirmEdit, setConfirmEdit] = useState(false);
  const [phoneError, setPhoneError] = useState<string | null>(null);

  const [confirmDelete, setConfirmDelete] = useState(false);
  const [deleting, setDeleting] = useState(false);

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
        api.getLedger(customerId, 0, LEDGER_PAGE_SIZE),
      ]);
      setCustomer(c);
      setEntries(l.entries);
      setLedgerPage(l.page);
      setHasMore(l.hasMore);
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function loadMoreEntries() {
    if (loadingMore || !hasMore) return;
    setLoadingMore(true);
    setError(null);
    try {
      const next = await api.getLedger(customerId, ledgerPage + 1, LEDGER_PAGE_SIZE);
      setEntries((prev) => [...prev, ...next.entries]);
      setLedgerPage(next.page);
      setHasMore(next.hasMore);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoadingMore(false);
    }
  }

  async function addUtang() {
    const amount = Number(utang);
    if (!amount || amount <= 0) return;
    if (!utangNote.trim()) return;
    setError(null);
    try {
      await api.debit(customerId, amount, utangNote.trim());
      setUtang("");
      setUtangNote("");
      setReminderOpen(false);
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
      setReminderOpen(false);
      await load();
    } catch (e) {
      setError((e as Error).message);
    }
  }

  function startEditPhone() {
    setNameInput(customer?.name ?? "");
    setPhoneInput(customer?.phoneNumber ?? "");
    setPhoneError(null);
    setConfirmEdit(false);
    setEditingPhone(true);
  }

  async function savePhone() {
    if (!nameInput.trim()) return;
    setConfirmEdit(false);
    setPhoneSaving(true);
    setPhoneError(null);
    try {
      const updated = await api.updateCustomer(
        customerId,
        nameInput.trim(),
        phoneInput.trim() || undefined
      );
      setCustomer(updated);
      setEditingPhone(false);
    } catch (e) {
      setPhoneError((e as Error).message);
    } finally {
      setPhoneSaving(false);
    }
  }

  async function handleDelete() {
    setDeleting(true);
    setError(null);
    try {
      await api.deleteCustomer(customerId);
      router.replace("/dashboard");
    } catch (e) {
      setError((e as Error).message);
      setDeleting(false);
      setConfirmDelete(false);
    }
  }

  async function openReminder() {
    setError(null);
    setNotice(null);
    try {
      const preview = await api.reminderPreview(customerId);
      setReminderMsg(preview.message);
      setReminderOpen(true);
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function copyReminder() {
    try {
      await navigator.clipboard.writeText(reminderMsg);
      setNotice("Message copied! Paste it in SMS or Messenger.");
    } catch (e) {
      setError((e as Error).message);
    }
  }

  function textViaSms() {
    if (!customer?.phoneNumber) return;
    // sms: URI scheme (RFC 5724). The `?&body=` form prefills the message on
    // both iOS and Android. Navigating opens the Messages app so the owner
    // sends it from their own phone.
    const number = customer.phoneNumber.replace(/\s+/g, "");
    const href = `sms:${number}?&body=${encodeURIComponent(reminderMsg)}`;
    window.location.href = href;
    setNotice("Opening Messages… tap Send there to finish.");
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
        {editingPhone ? (
          <div>
            <label htmlFor="editname">Name</label>
            <input
              id="editname"
              placeholder="Pangalan"
              value={nameInput}
              onChange={(e) => {
                setNameInput(e.target.value);
                setPhoneError(null);
                setConfirmEdit(false);
              }}
            />
            <label htmlFor="editphone">Mobile number</label>
            <input
              id="editphone"
              type="tel"
              inputMode="tel"
              placeholder="09XX XXX XXXX (optional)"
              value={phoneInput}
              onChange={(e) => {
                setPhoneInput(e.target.value);
                setPhoneError(null);
                setConfirmEdit(false);
              }}
            />
            {phoneError && <p className="error">{phoneError}</p>}
            {confirmEdit ? (
              <div className="notice" style={{ marginTop: 4 }}>
                <p style={{ margin: 0 }}>Save these changes to this suki&apos;s details?</p>
                <div style={{ display: "flex", gap: 8, marginTop: 10 }}>
                  <button onClick={savePhone} disabled={phoneSaving}>
                    {phoneSaving ? "Saving…" : "Yes, save"}
                  </button>
                  <button
                    className="secondary"
                    onClick={() => setConfirmEdit(false)}
                    disabled={phoneSaving}
                  >
                    Cancel
                  </button>
                </div>
              </div>
            ) : (
              <div style={{ display: "flex", gap: 8, marginTop: 4 }}>
                <button
                  onClick={() => {
                    if (!nameInput.trim()) return;
                    setConfirmEdit(true);
                  }}
                  disabled={phoneSaving || !nameInput.trim()}
                >
                  Save changes
                </button>
                <button
                  className="secondary"
                  onClick={() => setEditingPhone(false)}
                  disabled={phoneSaving}
                >
                  Cancel
                </button>
              </div>
            )}
          </div>
        ) : (
          <>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 10,
              }}
            >
              <div className="name" style={{ fontSize: 18, fontWeight: 700 }}>
                {customer.name}
              </div>
              <a
                className="link"
                style={{ cursor: "pointer" }}
                onClick={startEditPhone}
              >
                Edit
              </a>
            </div>
            <div className="muted">
              {customer.phoneNumber ?? "No mobile number added"}
            </div>
          </>
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
        <label htmlFor="note">Note</label>
        <input
          id="note"
          placeholder="e.g. sardinas, load"
          value={utangNote}
          onChange={(e) => setUtangNote(e.target.value)}
        />
        <button onClick={addUtang} disabled={!Number(utang) || !utangNote.trim()}>
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
          {customer.phoneNumber ? (
            <button onClick={textViaSms}>Text via SMS</button>
          ) : (
            <div className="notice">
              <p style={{ margin: 0 }}>
                <strong>Text via SMS is disabled</strong> — this suki has no
                mobile number on file.
              </p>
              <p style={{ margin: "8px 0 0" }} className="muted">
                Copy the message below instead, or{" "}
                <a
                  className="link"
                  style={{ cursor: "pointer" }}
                  title="Add a mobile number to enable direct SMS texting"
                  onClick={() => {
                    setReminderOpen(false);
                    startEditPhone();
                  }}
                >
                  add a mobile number
                </a>{" "}
                to enable direct texting next time.
              </p>
            </div>
          )}
          <button className="secondary" onClick={copyReminder}>
            Copy message
          </button>
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
        {entries.length === 0 && (
          <p className="muted">No entries yet.</p>
        )}
        {entries.map((e) => (
          <div key={e.id} className="entry">
            <div>
              <div>{e.type === "DEBIT" ? "Utang" : "Bayad"}</div>
              {e.note && <div className="muted">{e.note}</div>}
            </div>
            <div
              className={`amt ${e.type === "DEBIT" ? "debit" : "credit"}`}
            >
              {e.type === "DEBIT" ? "+" : "\u2212"}
              {formatPeso(e.amount)}
            </div>
          </div>
        ))}
        {hasMore && (
          <button
            className="secondary"
            onClick={loadMoreEntries}
            disabled={loadingMore}
            style={{ marginTop: 8 }}
          >
            {loadingMore ? "Loading\u2026" : "Load more"}
          </button>
        )}
      </div>

      <div className="card">
        <strong>Delete suki</strong>
        <p className="muted">
          Removes {customer.name} and their entire utang history. This cannot be
          undone.
        </p>
        <button className="danger" onClick={() => setConfirmDelete(true)}>
          Delete suki
        </button>
      </div>

      {confirmDelete && (
        <div
          className="modal-overlay"
          onClick={() => !deleting && setConfirmDelete(false)}
        >
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h2>Delete {customer.name}?</h2>
            {owed ? (
              <div className="notice" style={{ marginTop: 0 }}>
                <strong>May natitirang utang pa ({formatPeso(customer.currentBalance)})</strong>
                <p style={{ margin: "6px 0 0" }} className="muted">
                  This suki still owes you money. Deleting them erases this
                  balance and their whole ledger for good.
                </p>
              </div>
            ) : (
              <p className="muted">
                This permanently removes the suki and their entire utang
                history. This cannot be undone.
              </p>
            )}
            <div style={{ display: "flex", gap: 8, marginTop: 16 }}>
              <button
                className="danger"
                onClick={handleDelete}
                disabled={deleting}
              >
                {deleting ? "Deleting\u2026" : "Yes, delete"}
              </button>
              <button
                className="secondary"
                onClick={() => setConfirmDelete(false)}
                disabled={deleting}
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {notice && <p className="link center">{notice}</p>}
      {error && <p className="error">{error}</p>}
    </>
  );
}
