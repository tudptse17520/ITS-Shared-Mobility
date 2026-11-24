import { cn } from "@/lib/utils";

interface PageHeaderProps {
  title: string;
  description?: string;
  actions?: React.ReactNode;
  className?: string;
}

export function PageHeader({ title, description, actions, className }: PageHeaderProps) {
  return (
    <div className={cn("flex min-w-0 flex-wrap items-end justify-between gap-3 pb-1", className)}>
      <div className="min-w-0">
        <h1 className="text-[clamp(1.65rem,3vw,2.25rem)] leading-tight font-semibold tracking-[-0.035em]">{title}</h1>
        {description && <p className="mt-1.5 max-w-2xl text-sm leading-relaxed text-muted-foreground">{description}</p>}
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  );
}
