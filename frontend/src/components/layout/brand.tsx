import Image from "next/image";
import Link from "next/link";
import { cn } from "@/lib/utils";

export function BrandMark({ className }: { className?: string }) {
  return (
    <span className={cn("its-brand-mark size-10 shrink-0 overflow-hidden rounded-[11px]", className)} aria-hidden="true">
      <Image src="/its-app-icon.svg" alt="" width={40} height={40} className="size-full" unoptimized />
    </span>
  );
}

export function AdminBrand() {
  return (
    <Link href="/admin/dashboard" className="flex w-fit items-center gap-3 rounded-xl">
      <BrandMark />
      <span className="min-w-0">
        <span className="block text-[15px] font-semibold leading-5">ITS Mobility</span>
        <span className="block text-xs text-muted-foreground">Quản trị đội xe</span>
      </span>
    </Link>
  );
}
