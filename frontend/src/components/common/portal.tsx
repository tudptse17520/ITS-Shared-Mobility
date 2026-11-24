"use client";

import { useSyncExternalStore, ReactNode } from "react";
import { createPortal } from "react-dom";

interface PortalProps {
  children: ReactNode;
}

const emptySubscribe = () => () => {};

export function Portal({ children }: PortalProps) {
  const mounted = useSyncExternalStore(
    emptySubscribe,
    () => true,
    () => false
  );

  if (!mounted) return null;

  return createPortal(children, document.body);
}
