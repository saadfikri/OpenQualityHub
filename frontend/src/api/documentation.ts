import api from './client';

export const documentationApi = {
  list: (params?: Record<string, string>) =>
    api.get('/documentation', { params }).then((r) => r.data),

  getById: (id: string) =>
    api.get(`/documentation/${id}`).then((r) => r.data),

  create: (data: {
    title: string;
    description?: string;
    docType: string;
    url?: string;
    repoPath?: string;
    category: string;
    projectId?: string;
    ownerUserId?: string;
    reviewCycleDays?: number;
  }) => api.post('/documentation', data).then((r) => r.data),

  delete: (id: string) =>
    api.delete(`/documentation/${id}`).then((r) => r.data),

  runHealthChecks: (id: string) =>
    api.post(`/documentation/${id}/health-checks`).then((r) => r.data),

  createReview: (data: { assetId: string; reviewerId?: string; dueDate?: string }) =>
    api.post('/documentation/reviews', data).then((r) => r.data),

  updateReviewStatus: (reviewId: string, status: string, comment?: string) =>
    api.patch(`/documentation/reviews/${reviewId}`, { status, comment }).then((r) => r.data),

  getDashboard: () =>
    api.get('/documentation/dashboard').then((r) => r.data),
};
