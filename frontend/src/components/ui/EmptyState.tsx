import type { ReactNode } from 'react';

interface EmptyStateProps {
  icon: ReactNode;
  title: string;
  description: string;
  action?: ReactNode;
}

export function EmptyState({ icon, title, description, action }: EmptyStateProps) {
  return (
    <div className="text-center py-12">
      <div className="mx-auto h-12 w-12 text-[#9BA8B9]">{icon}</div>
      <h3 className="mt-4 text-sm font-semibold text-[var(--oqh-text)]">{title}</h3>
      <p className="mt-1 text-sm text-[var(--oqh-text-muted)]">{description}</p>
      {action && <div className="mt-6">{action}</div>}
    </div>
  );
}
