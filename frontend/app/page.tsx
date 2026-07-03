"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api, setToken } from "@/lib/api";
import { Logo } from "./Logo";

export default function LoginPage() {
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleLogin() {
    if (!username.trim() || !password) return;
    setError(null);
    setLoading(true);
    try {
      const res = await api.login(username.trim(), password);
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
        <strong>Log in</strong>
        <p className="muted">
          Track utang easily and get paid faster—without awkward conversations.
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

        {error && <p className="error">{error}</p>}
      </div>
    </>
  );
}
