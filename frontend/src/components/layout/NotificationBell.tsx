import { useState, useRef, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Bell, Check } from 'lucide-react';
import { notificationsApi } from '@/api/notifications';
import { useNavigate } from 'react-router-dom';

export function NotificationBell() {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data: unreadCount = 0 } = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: notificationsApi.unreadCount,
    refetchInterval: 30000,
  });

  const { data: notifications } = useQuery({
    queryKey: ['notifications', 'list'],
    queryFn: () => notificationsApi.list(0, 10),
    enabled: open,
  });

  const markReadMutation = useMutation({
    mutationFn: notificationsApi.markRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  const markAllReadMutation = useMutation({
    mutationFn: notificationsApi.markAllRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setOpen(!open)}
        className="relative p-2 rounded-lg text-[#60718A] hover:text-[var(--oqh-text)] hover:bg-[#F7F9FC] transition-colors"
        aria-label="Notifications"
      >
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <span className="absolute -top-0.5 -right-0.5 h-4 min-w-4 px-1 rounded-full bg-[var(--oqh-danger)] text-white text-[10px] font-bold flex items-center justify-center">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 mt-2 w-80 bg-white rounded-xl border border-[var(--oqh-border)] shadow-[0_8px_30px_rgba(14,43,73,.1)] z-50 overflow-hidden">
          <div className="flex items-center justify-between px-4 py-3 border-b border-[var(--oqh-border)]">
            <span className="text-sm font-semibold text-[var(--oqh-text)]">Notifications</span>
            {unreadCount > 0 && (
              <button
                onClick={() => markAllReadMutation.mutate()}
                className="text-xs text-[var(--oqh-blue)] hover:text-[var(--oqh-primary)] font-medium flex items-center gap-1"
              >
                <Check className="h-3 w-3" />
                Mark all read
              </button>
            )}
          </div>
          <div className="max-h-80 overflow-y-auto">
            {(notifications?.content ?? []).length === 0 ? (
              <div className="px-4 py-8 text-center text-sm text-[#9BA8B9]">No notifications</div>
            ) : (
              notifications?.content.map((n) => (
                <div
                  key={n.id}
                  className={`px-4 py-3 border-b border-[var(--oqh-border)]/30 cursor-pointer hover:bg-[#F7F9FC] transition-colors ${!n.read ? 'bg-[var(--oqh-active-bg)]/50' : ''}`}
                  onClick={() => {
                    if (!n.read) markReadMutation.mutate(n.id);
                    if (n.link) { navigate(n.link); setOpen(false); }
                  }}
                >
                  <p className={`text-sm ${!n.read ? 'font-medium text-[var(--oqh-text)]' : 'text-[var(--oqh-text-muted)]'}`}>
                    {n.title}
                  </p>
                  <p className="text-xs text-[#9BA8B9] mt-0.5">{n.message}</p>
                  <p className="text-[10px] text-[#9BA8B9]/70 mt-1">
                    {new Date(n.createdAt).toLocaleString()}
                  </p>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
