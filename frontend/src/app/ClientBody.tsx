"use client";

import { useEffect } from "react";
import { cleanupStaleServiceWorkers } from "@/lib/cleanupStaleServiceWorkers";
import { installNextHmrBfcacheGuard } from "@/lib/nextHmrBfcacheGuard";
import { clearAuthSession, readAccessToken } from "@/lib/finsightToken";

if (typeof window !== "undefined") {
  installNextHmrBfcacheGuard();
}

export default function ClientBody({
  children,
}: {
  children: React.ReactNode;
}) {
  useEffect(() => {
    installNextHmrBfcacheGuard();
    cleanupStaleServiceWorkers();
    const token = readAccessToken();
    if (!token) return;
    void fetch("/api/v1/auth/me", {
      headers: { Accept: "application/json", Authorization: `Bearer ${token}` },
    }).then((res) => {
      if (res.status === 401 || res.status === 403) {
        clearAuthSession();
      }
    }).catch(() => {
      void 0;
    });
  }, []);

  return <div className="antialiased">{children}</div>;
}
