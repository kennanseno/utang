import type { Metadata } from "next";

const title = "Bayaran ang utang mo — Utang";
const description =
  "Tingnan ang balanse mo at magbayad gamit ang QR code ng tindahan. Mabilis, ligtas, at kahit saan pwede.";

export const metadata: Metadata = {
  title,
  description,
  robots: { index: false, follow: false },
  openGraph: {
    title,
    description,
    type: "website",
    images: [{ url: "/logo.png", width: 512, height: 512, alt: "Utang" }],
  },
  twitter: {
    card: "summary",
    title,
    description,
    images: ["/logo.png"],
  },
};

export default function PublicPayLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <>{children}</>;
}
