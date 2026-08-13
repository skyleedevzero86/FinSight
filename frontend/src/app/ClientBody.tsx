"use client";

import { useEffect } from "react";
import { cleanupStaleServiceWorkers } from "@/lib/cleanupStaleServiceWorkers";
import { installNextHmrBfcacheGuard } from "@/lib/nextHmrBfcacheGuard";

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
  }, []);

  return <div className="antialiased">{children}</div>;
}
