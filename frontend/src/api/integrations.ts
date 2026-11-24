import api from './client';
import type {
  Page,
  IntegrationConfig,
  WebhookEventItem,
  IntegrationDashboard,
} from '@/types/api';

export const integrationsApi = {
  getDashboard: () =>
    api.get<IntegrationDashboard>('/integrations/dashboard').then((r) => r.data),

  getAll: (params?: Record<string, string>) =>
    api.get<Page<IntegrationConfig>>('/integrations', { params }).then((r) => r.data),

  getById: (id: string) =>
    api.get<IntegrationConfig>(`/integrations/${id}`).then((r) => r.data),

  create: (data: {
    name: string;
    description?: string;
    provider: string;
    webhookSecret?: string;
    projectId?: string;
  }) => api.post<IntegrationConfig>('/integrations', data).then((r) => r.data),

  update: (
    id: string,
    data: { name?: string; description?: string; enabled?: boolean; webhookSecret?: string; config?: string }
  ) => api.put<IntegrationConfig>(`/integrations/${id}`, data).then((r) => r.data),

  delete: (id: string) => api.delete(`/integrations/${id}`),

  getEvents: (id: string, params?: Record<string, string>) =>
    api.get<Page<WebhookEventItem>>(`/integrations/${id}/events`, { params }).then((r) => r.data),

  getWebhookUrl: (id: string) =>
    api.get<string>(`/integrations/${id}/webhook-url`).then((r) => r.data),
};
