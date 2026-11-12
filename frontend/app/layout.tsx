import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "LicenseLeak AI | Creator licensing review",
  description: "Review licensing terms, usage evidence, assisted analysis, and payment activity in one workspace.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
