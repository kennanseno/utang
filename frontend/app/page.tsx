"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { api, setToken } from "@/lib/api";

export default function OnboardingPage() {
  const router = useRouter();
  const [step, setStep] = useState<"phone" | "otp">("phone");
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [devCode, setDevCode] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleRequestOtp() {
    setError(null);
    setLoading(true);
    try {
      const res = await api.requestOtp(phone.trim());
      setDevCode(res.devCode);
      setStep("otp");
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }

  async function handleVerify() {
    setError(null);
    setLoading(true);
    try {
      const res = await api.verifyOtp(phone.trim(), code.trim());
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
          <span className="brand">Utang</span>
        </h1>
      </div>

      <div className="card">
        <p className="muted">
          Track utang easily and get paid faster—without awkward conversations.
        </p>

        {step === "phone" && (
          <>
            <label htmlFor="phone">Mobile number</label>
            <input
              id="phone"
              type="tel"
              inputMode="tel"
              placeholder="09XX XXX XXXX"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
            />
            <button
              onClick={handleRequestOtp}
              disabled={loading || phone.trim().length < 7}
            >
              {loading ? "Sending…" : "Send OTP"}
            </button>
          </>
        )}

        {step === "otp" && (
          <>
            <label htmlFor="otp">Enter OTP</label>
            <input
              id="otp"
              type="text"
              inputMode="numeric"
              placeholder="6-digit code"
              value={code}
              onChange={(e) => setCode(e.target.value)}
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
                setStep("phone");
                setCode("");
              }}
            >
              Change number
            </button>
          </>
        )}

        {error && <p className="error">{error}</p>}
      </div>
    </>
  );
}
