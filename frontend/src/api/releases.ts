import api from './client';

export const releasesApi = {
  getDashboard: () =>
    api.get('/releases/dashboard/readiness').then((r) => r.data),

  getLatestReadiness: (releaseId: string) =>
    api.get(`/releases/${releaseId}/readiness`).then((r) => r.data),

  getReadinessHistory: (releaseId: string, params?: Record<string, string>) =>
    api.get(`/releases/${releaseId}/readiness/history`, { params }).then((r) => r.data),

  generateReadiness: (releaseId: string) =>
    api.post(`/releases/${releaseId}/readiness`).then((r) => r.data),

  getSignOffs: (releaseId: string) =>
    api.get(`/releases/${releaseId}/sign-offs`).then((r) => r.data),

  signOff: (releaseId: string, data: { decision: string; comment?: string }) =>
    api.post(`/releases/${releaseId}/sign-offs`, data).then((r) => r.data),
};
