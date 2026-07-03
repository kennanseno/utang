"use client";

import { useEffect, useState, type ChangeEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api, getToken, setToken } from "@/lib/api";
import { Logo } from "../Logo";

const USERNAME_PATTERN = /^[a-zA-Z0-9]+$/;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const QR_MAX_BYTES = 2 * 1024 * 1024;
const QR_ACCEPTED = ["image/png", "image/jpeg", "image/webp", "image/gif"];

interface FieldErrors {
  storeName?: string;
  username?: string;
  password?: string;
  email?: string;
  phone?: string;
}

function validate(values: {
  storeName: string;
  username: string;
  password: string;
  email: string;
  phone: string;
}): FieldErrors {
  const errors: FieldErrors = {};

  if (!values.storeName.trim()) {
    errors.storeName = "Store name is required.";
  }

  const username = values.username.trim();
  if (!username) {
    errors.username = "Username is required.";
  } else if (username.length < 3) {
    errors.username = "Username must be at least 3 characters.";
  } else if (!USERNAME_PATTERN.test(username)) {
    errors.username = "Only letters and numbers are allowed.";
  }

  if (!values.password) {
    errors.password = "Password is required.";
  } else if (values.password.length < 6) {
    errors.password = "Password must be at least 6 characters.";
  }

  const email = values.email.trim();
  if (!email) {
    errors.email = "Email is required.";
  } else if (!EMAIL_PATTERN.test(email)) {
    errors.email = "Enter a valid email address.";
  }

  const phone = values.phone.trim();
  if (!phone) {
    errors.phone = "Mobile number is required.";
  } else if (phone.replace(/\D/g, "").length < 10) {
    errors.phone = "Enter a valid mobile number, e.g. 09XX XXX XXXX.";
  }

  return errors;
}

export default function RegisterPage() {
  const router = useRouter();
  const [storeName, setStoreName] = useState("");
  const [ownerName, setOwnerName] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [qrFile, setQrFile] = useState<File | null>(null);
  const [qrPreview, setQrPreview] = useState<string | null>(null);
  const [qrError, setQrError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);

  // Clean up the preview object URL when it changes or on unmount.
  useEffect(() => {
    return () => {
      if (qrPreview) URL.revokeObjectURL(qrPreview);
    };
  }, [qrPreview]);

  function handleQrChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0] ?? null;
    setQrError(null);
    if (!file) {
      setQrFile(null);
      setQrPreview((prev) => {
        if (prev) URL.revokeObjectURL(prev);
        return null;
      });
      return;
    }
    if (!QR_ACCEPTED.includes(file.type)) {
      setQrError("QR code must be a PNG, JPG, WebP or GIF image.");
      return;
    }
    if (file.size > QR_MAX_BYTES) {
      setQrError("QR code image must be 2 MB or smaller.");
      return;
    }
    setQrFile(file);
    setQrPreview((prev) => {
      if (prev) URL.revokeObjectURL(prev);
      return URL.createObjectURL(file);
    });
  }

  useEffect(() => {
    // Already signed in — no need to register again.
    if (getToken()) {
      router.replace("/dashboard");
    }
  }, [router]);

  // Once the user has attempted to submit, re-validate live so errors clear
  // as they fix each field.
  useEffect(() => {
    if (submitted) {
      setFieldErrors(validate({ storeName, username, password, email, phone }));
      // Clear the server-side "already in use" message once the owner edits
      // any field, so a stale error doesn't linger.
      setError(null);
    }
  }, [submitted, storeName, username, password, email, phone]);

  async function handleSubmit() {
    setSubmitted(true);
    const errors = validate({ storeName, username, password, email, phone });
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      setError(null);
      return;
    }

    setError(null);
    setLoading(true);
    try {
      const res = await api.register({
        username: username.trim(),
        password,
        phoneNumber: phone.trim(),
        email: email.trim(),
        storeName: storeName.trim(),
        ownerName: ownerName.trim() || undefined,
      });
      setToken(res.token);
      // QR code is optional; upload it after the account exists. If it fails the
      // owner can still add it later from Store details, so don't block sign-in.
      if (qrFile) {
        try {
          await api.uploadQrCode(qrFile);
        } catch {
          // Ignore — QR can be added later in Store details.
        }
      }
      router.push("/dashboard");
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
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

      <div className="card">
        <strong>Create your account</strong>
        <p className="muted">
          Set up your tindahan. Your store name shows up in your suki&apos;s
          payment reminders.
        </p>

        <label htmlFor="storeName">Store name</label>
        <input
          id="storeName"
          placeholder="e.g. Aling Nena Store"
          value={storeName}
          aria-invalid={!!fieldErrors.storeName}
          onChange={(e) => setStoreName(e.target.value)}
        />
        {fieldErrors.storeName && (
          <p className="error field-error">{fieldErrors.storeName}</p>
        )}

        <label htmlFor="ownerName">Your name (optional)</label>
        <input
          id="ownerName"
          placeholder="e.g. Nena Reyes"
          value={ownerName}
          onChange={(e) => setOwnerName(e.target.value)}
        />

        <label htmlFor="username">Username</label>
        <input
          id="username"
          autoComplete="username"
          placeholder="e.g. alingnena"
          value={username}
          aria-invalid={!!fieldErrors.username}
          onChange={(e) => setUsername(e.target.value)}
        />
        {fieldErrors.username ? (
          <p className="error field-error">{fieldErrors.username}</p>
        ) : (
          <p className="muted" style={{ marginTop: 6 }}>
            At least 3 characters: letters and numbers only.
          </p>
        )}

        <label htmlFor="password">Password</label>
        <input
          id="password"
          type="password"
          autoComplete="new-password"
          placeholder="at least 6 characters"
          value={password}
          aria-invalid={!!fieldErrors.password}
          onChange={(e) => setPassword(e.target.value)}
        />
        {fieldErrors.password && (
          <p className="error field-error">{fieldErrors.password}</p>
        )}

        <label htmlFor="email">Email</label>
        <input
          id="email"
          type="email"
          inputMode="email"
          autoComplete="email"
          placeholder="you@example.com"
          value={email}
          aria-invalid={!!fieldErrors.email}
          onChange={(e) => setEmail(e.target.value)}
        />
        {fieldErrors.email ? (
          <p className="error field-error">{fieldErrors.email}</p>
        ) : (
          <p className="muted" style={{ marginTop: 6 }}>
            Verify this later in Store details to secure your account.
          </p>
        )}

        <label htmlFor="phone">Mobile number</label>
        <input
          id="phone"
          type="tel"
          inputMode="tel"
          placeholder="09XX XXX XXXX"
          value={phone}
          aria-invalid={!!fieldErrors.phone}
          onChange={(e) => setPhone(e.target.value)}
        />
        {fieldErrors.phone ? (
          <p className="error field-error">{fieldErrors.phone}</p>
        ) : (
          <p className="muted" style={{ marginTop: 6 }}>
            Shown to your suki so they can reach you about their utang.
          </p>
        )}

        <label htmlFor="qr">Payment QR code (optional)</label>
        <input
          id="qr"
          type="file"
          accept="image/png,image/jpeg,image/webp,image/gif"
          onChange={handleQrChange}
        />
        {qrError ? (
          <p className="error field-error">{qrError}</p>
        ) : (
          <p className="muted" style={{ marginTop: 6 }}>
            Upload your GCash/Maya QR so suki can scan and pay. You can add or
            change this later in Store details.
          </p>
        )}
        {qrPreview && (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={qrPreview}
            alt="QR code preview"
            style={{
              marginTop: 10,
              maxWidth: 180,
              width: "100%",
              borderRadius: "var(--radius)",
              border: "1px solid var(--border)",
            }}
          />
        )}

        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}

        <button onClick={handleSubmit} disabled={loading}>
          {loading ? "Creating account…" : "Create account"}
        </button>

        <p className="muted center" style={{ marginTop: 16 }}>
          May account ka na?{" "}
          <Link className="link" href="/">
            Log in
          </Link>
        </p>
      </div>
    </>
  );
}
