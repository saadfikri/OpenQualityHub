import api from './client';
import type { Page, AuditEventItem } from '@/types/api';

export const auditApi = {
  getEvents: (params?: Record<string, string>) =>
    api.get<Page<AuditEventItem>>('/audit', { params }).then((r) => r.data),
};
