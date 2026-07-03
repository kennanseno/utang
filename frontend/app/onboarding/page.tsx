"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { api, getToken } from "@/lib/api";

export default function OnboardingProfilePage() {
  const router = useRouter();
  const [storeName, setStoreName] = useState("");
  const [ownerName, setOwnerName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!getToken()) {
      router.replace("/");
      return;
    }
    // If the store already finished onboarding, skip straight to the dashboard.
    void api
      .me()
      .then((store) => {
        if (store.onboarded) {
          router.replace("/dashboard");
        } else if (store.name && store.name !== "My Store") {
          setStoreName(store.name);
        }
      })
      .catch(() => {
        // Ignore; the form still works and the API will re-validate on submit.
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function handleSubmit() {
    if (!storeName.trim()) return;
    setError(null);
    setLoading(true);
    try {
      await api.onboard(storeName.trim(), ownerName.trim() || undefined);
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
        <strong>Set up your store</strong>
        <p className="muted">
          Tell us about your tindahan. This shows up in your suki&apos;s payment
          reminders.
        </p>

        <label htmlFor="storeName">Store name</label>
        <input
          id="storeName"
          placeholder="e.g. Aling Nena Store"
          value={storeName}
          onChange={(e) => setStoreName(e.target.value)}
        />

        <label htmlFor="ownerName">Your name (optional)</label>
        <input
          id="ownerName"
          placeholder="e.g. Nena Reyes"
          value={ownerName}
          onChange={(e) => setOwnerName(e.target.value)}
        />

        <button onClick={handleSubmit} disabled={loading || !storeName.trim()}>
          {loading ? "Saving…" : "Continue"}
        </button>

        {error && <p className="error">{error}</p>}
      </div>
    </>
  );
}
