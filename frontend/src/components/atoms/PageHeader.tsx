import type { LucideIcon } from 'lucide-react';

interface PageHeaderProps {
  icon: LucideIcon;
  iconClasses: string;
  title: string;
}

export function PageHeader({ icon: Icon, iconClasses, title }: PageHeaderProps) {
  return (
    <div className="flex items-center gap-3">
      <div className={`flex h-10 w-10 items-center justify-center rounded-md ${iconClasses}`}>
        <Icon className="h-5 w-5" strokeWidth={2} aria-hidden="true" />
      </div>
      <h1 className="text-2xl font-semibold text-foreground">{title}</h1>
    </div>
  );
}
