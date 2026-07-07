type LogoProps = {
  /** Pixel size of the square badge. */
  size?: number;
};

/**
 * Utang brand mark: a rounded badge with a Philippine peso (₱) glyph, echoing
 * the app's purpose of tracking money owed. Uses `currentColor` for the badge
 * so it follows the surrounding brand color (and adapts to light/dark themes).
 */
export function Logo({ size = 28 }: LogoProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 40 40"
      fill="none"
      role="img"
      aria-label="Utang logo"
    >
      <rect x="2" y="2" width="36" height="36" rx="10" fill="currentColor" />
      {/* Peso mark */}
      <path
        d="M15 11h6a5 5 0 0 1 0 10h-6"
        stroke="#ffffff"
        strokeWidth="3.2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <line
        x1="15"
        y1="11"
        x2="15"
        y2="30"
        stroke="#ffffff"
        strokeWidth="3.2"
        strokeLinecap="round"
      />
      <line
        x1="9.5"
        y1="16"
        x2="24"
        y2="16"
        stroke="#ffffff"
        strokeWidth="2.6"
        strokeLinecap="round"
      />
      <line
        x1="9.5"
        y1="20"
        x2="24"
        y2="20"
        stroke="#ffffff"
        strokeWidth="2.6"
        strokeLinecap="round"
      />
    </svg>
  );
}
