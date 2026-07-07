"use client";

import { useEffect, useState } from "react";

type Theme = "system" | "light" | "dark";

const THEME_KEY = "utang.theme";

const NEXT: Record<Theme, Theme> = {
  system: "light",
  light: "dark",
  dark: "system",
};

const LABEL: Record<Theme, string> = {
  system: "Theme: System",
  light: "Theme: Light",
  dark: "Theme: Dark",
};

function ThemeIcon({ theme }: { theme: Theme }) {
  if (theme === "light") {
    // Sun
    return (
      <svg
        width="18"
        height="18"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
      >
        <circle cx="12" cy="12" r="4" />
        <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />
      </svg>
    );
  }
  if (theme === "dark") {
    // Moon
    return (
      <svg
        width="18"
        height="18"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
      >
        <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
      </svg>
    );
  }
  // System (monitor)
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <rect x="2" y="3" width="20" height="14" rx="2" />
      <path d="M8 21h8M12 17v4" />
    </svg>
  );
}

export default function ThemeToggle() {
  const [theme, setTheme] = useState<Theme>("system");
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    const saved = window.localStorage.getItem(THEME_KEY) as Theme | null;
    setTheme(saved === "light" || saved === "dark" ? saved : "system");
    setMounted(true);
  }, []);

  function apply(next: Theme) {
    setTheme(next);
    if (next === "system") {
      window.localStorage.removeItem(THEME_KEY);
      document.documentElement.removeAttribute("data-theme");
    } else {
      window.localStorage.setItem(THEME_KEY, next);
      document.documentElement.setAttribute("data-theme", next);
    }
  }

  // Avoid hydration mismatch: only render once we've read the stored choice.
  if (!mounted) return null;

  return (
    <button
      type="button"
      className="theme-toggle"
      onClick={() => apply(NEXT[theme])}
      aria-label={LABEL[theme]}
      title={LABEL[theme]}
    >
      <ThemeIcon theme={theme} />
    </button>
  );
}
