"use client";

import { useEffect, useState, type ChangeEvent } from "react";
import { useRouter } from "next/navigation";
import { api, fetchQrCodeUrl, getToken } from "@/lib/api";
import ThemeToggle from "../ThemeToggle";
import { Logo } from "../Logo";

const QR_MAX_BYTES = 2 * 1024 * 1024;
const QR_ACCEPTED = ["image/png", "image/jpeg", "image/webp", "image/gif"];

export default function StoreSettingsPage() {
  const router = useRouter();
  const [storeName, setStoreName] = useState("");
  const [ownerName, setOwnerName] = useState("");
  const [phone, setPhone] = useState("");
  const [phoneVerified, setPhoneVerified] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  // Payment QR code.
  const [qrUrl, setQrUrl] = useState<string | null>(null);
  const [hasQr, setHasQr] = useState(false);
  const [qrBusy, setQrBusy] = useState(false);
  const [qrError, setQrError] = useState<string | null>(null);

  // Phone verification flow.
  const [verifyStep, setVerifyStep] = useState<"idle" | "code">("idle");
  const [code, setCode] = useState("");
  const [maskedPhone, setMaskedPhone] = useState("");
  const [devCode, setDevCode] = useState<string | null>(null);
  const [verifyError, setVerifyError] = useState<string | null>(null);
  const [verifyBusy, setVerifyBusy] = useState(false);

  useEffect(() => {
    if (!getToken()) {
      router.replace("/");
      return;
    }
    void api
      .me()
      .then((store) => {
        setStoreName(store.name);
        setOwnerName(store.ownerName ?? "");
        setPhone(store.phoneNumber);
        setPhoneVerified(store.phoneVerified);
        setHasQr(store.hasQrCode);
        if (store.hasQrCode) {
          void fetchQrCodeUrl()
            .then((url) => setQrUrl(url))
            .catch(() => {});
        }
      })
      .catch((e) => setError((e as Error).message))
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Revoke the QR object URL on unmount to avoid leaking it.
  useEffect(() => {
    return () => {
      if (qrUrl) URL.revokeObjectURL(qrUrl);
    };
  }, [qrUrl]);

  async function handleQrChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0] ?? null;
    // Allow re-selecting the same file later.
    e.target.value = "";
    setQrError(null);
    if (!file) return;
    if (!QR_ACCEPTED.includes(file.type)) {
      setQrError("QR code must be a PNG, JPG, WebP or GIF image.");
      return;
    }
    if (file.size > QR_MAX_BYTES) {
      setQrError("QR code image must be 2 MB or smaller.");
      return;
    }
    setQrBusy(true);
    try {
      await api.uploadQrCode(file);
      const url = await fetchQrCodeUrl();
      setHasQr(true);
      setQrUrl((prev) => {
        if (prev) URL.revokeObjectURL(prev);
        return url;
      });
    } catch (err) {
      setQrError((err as Error).message);
    } finally {
      setQrBusy(false);
    }
  }

  async function handleRemoveQr() {
    setQrError(null);
    setQrBusy(true);
    try {
      await api.removeQrCode();
      setHasQr(false);
      setQrUrl((prev) => {
        if (prev) URL.revokeObjectURL(prev);
        return null;
      });
    } catch (err) {
      setQrError((err as Error).message);
    } finally {
      setQrBusy(false);
    }
  }

  async function handleSave() {
    if (!storeName.trim() || !phone.trim()) return;
    setError(null);
    setSaved(false);
    setSaving(true);
    try {
      const updated = await api.updateStore(
        storeName.trim(),
        ownerName.trim() || undefined,
        phone.trim()
      );
      setStoreName(updated.name);
      setOwnerName(updated.ownerName ?? "");
      setPhone(updated.phoneNumber);
      setPhoneVerified(updated.phoneVerified);
      // Changing the number resets verification, so cancel any in-progress flow.
      setVerifyStep("idle");
      setSaved(true);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setSaving(false);
    }
  }

  async function handleRequestVerification() {
    setVerifyError(null);
    setVerifyBusy(true);
    try {
      const res = await api.requestPhoneVerification();
      setMaskedPhone(res.phoneNumber);
      setDevCode(res.devCode);
      setCode("");
      setVerifyStep("code");
    } catch (e) {
      setVerifyError((e as Error).message);
    } finally {
      setVerifyBusy(false);
    }
  }

  async function handleConfirmVerification() {
    if (code.trim().length < 4) return;
    setVerifyError(null);
    setVerifyBusy(true);
    try {
      const updated = await api.confirmPhoneVerification(code.trim());
      setPhoneVerified(updated.phoneVerified);
      setVerifyStep("idle");
    } catch (e) {
      setVerifyError((e as Error).message);
    } finally {
      setVerifyBusy(false);
    }
  }

  return (
    <>
      <div className="topbar">
        <h1>
          <span className="brand-lockup">
            <Logo />
            <span className="brand">Utang</span>
          </span>{" "}
          <span className="muted" style={{ fontSize: 14 }}>
            Store details
          </span>
        </h1>
        <a className="link" onClick={() => router.push("/dashboard")}>
          Back
        </a>
      </div>

      <div className="card">
        <strong>Store details</strong>
        <p className="muted">
          Update your tindahan info. Your store name shows up in your
          suki&apos;s payment reminders.
        </p>

        <label htmlFor="phone">
          Phone number{" "}
          {!loading &&
            (phoneVerified ? (
              <span className="badge ok">Verified</span>
            ) : (
              <span className="badge warn">Not verified</span>
            ))}
        </label>
        <input
          id="phone"
          type="tel"
          inputMode="tel"
          placeholder="09XX XXX XXXX"
          value={phone}
          onChange={(e) => {
            setPhone(e.target.value);
            setSaved(false);
          }}
        />

        {!loading && !phoneVerified && (
          <div className="notice">
            <p style={{ margin: 0 }}>
              Your mobile number isn&apos;t verified yet. Some features are
              disabled until you verify it — you won&apos;t be able to send SMS
              reminders to your suki or receive login and OTP codes on a new
              device.
            </p>
            {verifyStep === "idle" && (
              <button
                className="secondary"
                onClick={handleRequestVerification}
                disabled={verifyBusy}
                style={{ marginTop: 10 }}
              >
                {verifyBusy ? "Sending code…" : "Verify this number"}
              </button>
            )}
            {verifyStep === "code" && (
              <>
                <p className="muted" style={{ marginTop: 10 }}>
                  We sent a code to {maskedPhone}.
                </p>
                <label htmlFor="otp">Enter OTP</label>
                <input
                  id="otp"
                  type="text"
                  inputMode="numeric"
                  placeholder="6-digit code"
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  onKeyDown={(e) =>
                    e.key === "Enter" && handleConfirmVerification()
                  }
                />
                {devCode && (
                  <p className="muted">
                    Dev mode code: <strong>{devCode}</strong>
                  </p>
                )}
                <button
                  onClick={handleConfirmVerification}
                  disabled={verifyBusy || code.trim().length < 4}
                >
                  {verifyBusy ? "Verifying…" : "Confirm code"}
                </button>
                <button
                  className="secondary"
                  onClick={() => {
                    setVerifyStep("idle");
                    setVerifyError(null);
                  }}
                >
                  Cancel
                </button>
              </>
            )}
            {verifyError && <p className="error">{verifyError}</p>}
          </div>
        )}

        <label htmlFor="storeName">Store name</label>
        <input
          id="storeName"
          placeholder="e.g. Aling Nena Store"
          value={storeName}
          onChange={(e) => {
            setStoreName(e.target.value);
            setSaved(false);
          }}
        />

        <label htmlFor="ownerName">Your name (optional)</label>
        <input
          id="ownerName"
          placeholder="e.g. Nena Reyes"
          value={ownerName}
          onChange={(e) => {
            setOwnerName(e.target.value);
            setSaved(false);
          }}
        />

        <label htmlFor="qr">
          Payment QR code{" "}
          {!loading &&
            (hasQr ? (
              <span className="badge ok">Added</span>
            ) : (
              <span className="badge warn">Not added</span>
            ))}
        </label>
        <p className="muted" style={{ marginTop: 0 }}>
          Upload your GCash/Maya QR so suki can scan and pay.
        </p>
        {!loading && !hasQr && (
          <div className="notice">
            <p style={{ margin: 0 }}>
              You haven&apos;t added a payment QR code yet. Some features are
              disabled until you upload it — your suki won&apos;t be able to scan
              and pay you.
            </p>
          </div>
        )}
        {qrUrl && (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={qrUrl}
            alt="Payment QR code"
            style={{
              display: "block",
              maxWidth: 200,
              width: "100%",
              borderRadius: "var(--radius)",
              border: "1px solid var(--border)",
              marginBottom: 10,
            }}
          />
        )}
        <input
          id="qr"
          type="file"
          accept="image/png,image/jpeg,image/webp,image/gif"
          onChange={handleQrChange}
          disabled={qrBusy}
        />
        {qrBusy && <p className="muted">Uploading…</p>}
        {qrUrl && !qrBusy && (
          <button
            className="secondary"
            onClick={handleRemoveQr}
            style={{ marginTop: 10 }}
          >
            Remove QR code
          </button>
        )}
        {qrError && <p className="error">{qrError}</p>}

        <button
          onClick={handleSave}
          disabled={loading || saving || !storeName.trim() || !phone.trim()}
        >
          {saving ? "Saving…" : "Save changes"}
        </button>

        {saved && <p className="muted">Saved.</p>}
        {error && <p className="error">{error}</p>}
      </div>

      <ThemeToggle />
    </>
  );
}
