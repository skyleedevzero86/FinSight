"use client";

import { useEffect } from "react";
import { cleanupStaleServiceWorkers } from "@/lib/cleanupStaleServiceWorkers";

export default function ClientBody({
  children,
}: {
  children: React.ReactNode;
}) {
  useEffect(() => {
    cleanupStaleServiceWorkers();
  }, []);

  return <div className="antialiased">{children}</div>;
}
