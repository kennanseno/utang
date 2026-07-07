"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api, formatPeso, setToken, PublicStats } from "@/lib/api";
import { Logo } from "./Logo";

const FEATURES = [
  {
    emoji: "📒",
    title: "Isang tapik lang ang utang",
    body: "Record every utang and bayad in seconds—no more notebooks or scattered scratch paper.",
  },
  {
    emoji: "💸",
    title: "Mabilisang bayad",
    body: "Share a payment page with your QR code so suki can pay you anytime, kahit malayo.",
  },
  {
    emoji: "🔔",
    title: "Magalang na paalala",
    body: "Send a ready-made reminder via SMS—collect what's owed without the awkward asking.",
  },
];

function StatCard({
  value,
  label,
  ink,
}: {
  value: string;
  label: string;
  ink?: boolean;
}) {
  return (
    <div className="stat">
      <div className={`stat-value${ink ? " ink" : ""}`}>{value}</div>
      <div className="stat-label">{label}</div>
    </div>
  );
}

export default function LandingPage() {
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const [stats, setStats] = useState<PublicStats | null>(null);

  useEffect(() => {
    let active = true;
    async function loadStats() {
      try {
        const s = await api.publicStats();
        if (active) setStats(s);
      } catch {
        // Stats are best-effort; the page still works without them.
      }
    }
    void loadStats();
    // Refresh periodically so the counts feel live.
    const timer = setInterval(loadStats, 15000);
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, []);

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

  const count = (n: number) => n.toLocaleString("en-PH");

  return (
    <>
      <section className="hero">
        <span className="brand-lockup">
          <Logo />
          <span className="brand">Utang</span>
        </span>
        <h1 className="hero-title">
          Track <span className="accent">utang</span> and get paid faster
        </h1>
        <p className="hero-sub">
          The simple ledger for sari-sari stores. Record utang, remind suki, and
          collect payments—lahat sa telepono mo.
        </p>
        <div className="hero-cta">
          <Link href="/onboarding">
            <button style={{ width: "100%" }}>Start for free</button>
          </Link>
          <a className="link" href="#login">
            May account na? Mag-log in
          </a>
        </div>
      </section>

      <div className="section-label">Bakit Utang?</div>
      <div className="card">
        {FEATURES.map((f) => (
          <div key={f.title} className="feature">
            <span className="feature-emoji" aria-hidden="true">
              {f.emoji}
            </span>
            <div>
              <div className="feature-title">{f.title}</div>
              <p className="muted" style={{ margin: 0 }}>
                {f.body}
              </p>
            </div>
          </div>
        ))}
      </div>

      <div className="section-label">
        <span className="stats-live">
          <span className="live-dot" aria-hidden="true" />
          Live sa buong Utang
        </span>
      </div>
      <div className="card">
        <div className="stat-grid">
          <StatCard
            value={stats ? count(stats.storeCount) : "—"}
            label="Tindahang naka-onboard"
          />
          <StatCard
            value={stats ? count(stats.customerCount) : "—"}
            label="Suki na naidagdag"
          />
          <StatCard
            value={stats ? formatPeso(stats.totalRecorded) : "—"}
            label="Utang na naitala"
            ink
          />
          <StatCard
            value={stats ? formatPeso(stats.totalCollected) : "—"}
            label="Bayad na nakolekta"
          />
        </div>
        <p className="muted center" style={{ marginTop: 12, marginBottom: 0 }}>
          Sumali sa mga tindero at tinderang tumatrack ng utang nang tama.
        </p>
      </div>

      <div className="section-label" id="login">
        Mag-log in
      </div>
      <div className="card">
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

      <footer className="landing-footer">
        <p className="muted center" style={{ margin: 0 }}>
          May tanong, isyu, o suhestiyon?{" "}
          <a
            className="link"
            href="https://github.com/kennanseno/utang/issues"
            target="_blank"
            rel="noopener noreferrer"
          >
            Contact us
          </a>
        </p>
      </footer>
    </>
  );
}

