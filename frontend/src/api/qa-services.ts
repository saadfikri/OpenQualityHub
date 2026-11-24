import api from './client';

export const qaServicesApi = {
  list: (params?: Record<string, string>) =>
    api.get('/qa-services', { params }).then((r) => r.data),
};

export const qaRequestsApi = {
  list: (params?: Record<string, string>) =>
    api.get('/qa-requests', { params }).then((r) => r.data),

  getById: (id: string) =>
    api.get(`/qa-requests/${id}`).then((r) => r.data),

  create: (data: {
    title: string;
    description?: string;
    qaServiceId: string;
    requestingTeamId: string;
    projectId?: string;
    impact: string;
    urgency: string;
    estimatedHours?: number;
  }) => api.post('/qa-requests', data).then((r) => r.data),

  transition: (id: string, status: string, comment?: string) =>
    api.patch(`/qa-requests/${id}/transition`, null, {
      params: { status, ...(comment ? { comment } : {}) },
    }).then((r) => r.data),

  update: (id: string, data: Record<string, unknown>) =>
    api.put(`/qa-requests/${id}`, data).then((r) => r.data),

  overridePriority: (id: string, priority: number, reason: string) =>
    api.patch(`/qa-requests/${id}/priority-override`, { priority, reason }).then((r) => r.data),

  getDashboard: () =>
    api.get('/qa-requests/dashboard').then((r) => r.data),
};

export const slaApi = {
  listActive: () =>
    api.get('/sla/active').then((r) => r.data),
};

export const capacityApi = {
  getOverview: (weekStart?: string) =>
    api.get('/capacity/overview', { params: weekStart ? { weekStart } : {} }).then((r) => r.data),
};
