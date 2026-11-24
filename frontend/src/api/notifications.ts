import api from './client';
import type { Page } from '@/types/api';

export interface NotificationItem {
  id: string;
  title: string;
  message: string;
  type: string;
  read: boolean;
  link: string | null;
  createdAt: string;
}

export const notificationsApi = {
  list: (page = 0, size = 20) =>
    api.get<Page<NotificationItem>>('/notifications', { params: { page, size } }).then((r) => r.data),

  unreadCount: () =>
    api.get<number>('/notifications/unread-count').then((r) => r.data),

  markRead: (id: string) =>
    api.put<void>(`/notifications/${id}/read`),

  markAllRead: () =>
    api.put<void>('/notifications/read-all'),
};
