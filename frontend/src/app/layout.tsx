import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import ClientBody from "./ClientBody";
import { STALE_SERVICE_WORKER_CLEANUP_SCRIPT } from "@/lib/cleanupStaleServiceWorkers";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "finsight",
  description: "finsight — 뉴스 · 경제 · 실시간 VOD",
  icons: {
    icon: [{ url: "/favicon.ico", sizes: "any" }, { url: "/finsight-logo.png", type: "image/png" }],
    apple: "/finsight-logo.png",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className={`${geistSans.variable} ${geistMono.variable}`}>
      <head>
        <script
          dangerouslySetInnerHTML={{ __html: STALE_SERVICE_WORKER_CLEANUP_SCRIPT }}
        />
      </head>
      <body suppressHydrationWarning className="antialiased">
        <ClientBody>{children}</ClientBody>
      </body>
    </html>
  );
}
