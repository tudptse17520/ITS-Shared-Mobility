import { cn } from "@/lib/utils";

export function PageContainer({ children, className }: { children: React.ReactNode; className?: string }) {
  return (
    <div className={cn("animate-fade-in flex w-full min-w-0 flex-col gap-5 px-3 py-4 sm:p-5 xl:p-7", className)}>
      {children}
    </div>
  );
}
