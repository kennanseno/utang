"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api, setToken } from "@/lib/api";
import { Logo } from "./Logo";

export default function LoginPage() {
  const router = useRouter();
  const [step, setStep] = useState<"login" | "otp">("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [code, setCode] = useState("");
  const [maskedPhone, setMaskedPhone] = useState("");
  const [devCode, setDevCode] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleLogin() {
    if (!username.trim() || !password) return;
    setError(null);
    setLoading(true);
    try {
      const res = await api.login(username.trim(), password);
      if (res.status === "AUTHENTICATED") {
        setToken(res.token);
        router.push("/dashboard");
      } else {
        setMaskedPhone(res.phoneNumber);
        setDevCode(res.devCode);
        setCode("");
        setStep("otp");
      }
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }

  async function handleVerify() {
    if (code.trim().length < 4) return;
    setError(null);
    setLoading(true);
    try {
      const res = await api.verifyDevice(username.trim(), code.trim());
      setToken(res.token);
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
        {step === "login" && (
          <>
            <strong>Log in</strong>
            <p className="muted">
              Track utang easily and get paid faster—without awkward
              conversations.
            </p>

            <label htmlFor="username">Username</label>
            <input
              id="username"
              autoComplete="username"
              placeholder="your username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />

            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleLogin()}
            />

            <button
              onClick={handleLogin}
              disabled={loading || !username.trim() || !password}
            >
              {loading ? "Logging in…" : "Log in"}
            </button>

            <p className="muted center" style={{ marginTop: 16 }}>
              Wala pang account?{" "}
              <Link className="link" href="/onboarding">
                Register
              </Link>
            </p>
          </>
        )}

        {step === "otp" && (
          <>
            <strong>Verify this device</strong>
            <p className="muted">
              New device detected. We sent a code to {maskedPhone}.
            </p>

            <label htmlFor="otp">Enter OTP</label>
            <input
              id="otp"
              type="text"
              inputMode="numeric"
              placeholder="6-digit code"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleVerify()}
            />
            {devCode && (
              <p className="muted">
                Dev mode code: <strong>{devCode}</strong>
              </p>
            )}

            <button
              onClick={handleVerify}
              disabled={loading || code.trim().length < 4}
            >
              {loading ? "Verifying…" : "Verify & Continue"}
            </button>
            <button
              className="secondary"
              onClick={() => {
                setStep("login");
                setCode("");
                setError(null);
              }}
            >
              Back
            </button>
          </>
        )}

        {error && <p className="error">{error}</p>}
      </div>
    </>
  );
}
