"use client";

import { useEffect } from "react";
import AuthSessionProvider from "@/components/AuthSessionProvider";
import PasswordExpiryGuard from "@/components/PasswordExpiryGuard";
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

  return (
    <AuthSessionProvider>
      <PasswordExpiryGuard />
      <div className="antialiased">{children}</div>
    </AuthSessionProvider>
  );
}
