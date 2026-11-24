import { cn } from '@/lib/utils';

interface BadgeProps {
  variant?: 'success' | 'danger' | 'warning' | 'info' | 'default';
  children: React.ReactNode;
}

const variants = {
  success: 'bg-[#EAF8E6] text-[#0A7B3E] ring-[#12B76A]/20',
  danger: 'bg-[#FFEAED] text-[#B91C1C] ring-[#EF4444]/20',
  warning: 'bg-[#FFF4DC] text-[#92400E] ring-[#F59E0B]/20',
  info: 'bg-[#E9F2FF] text-[#1D4ED8] ring-[#2477F3]/20',
  default: 'bg-gray-50 text-[#48586E] ring-[#E0E7EF]',
};

export function Badge({ variant = 'default', children }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-md px-2 py-1 text-xs font-medium ring-1 ring-inset',
        variants[variant]
      )}
    >
      {children}
    </span>
  );
}
