import api from './client';
import type {
  Page,
  ContributorItem,
  ContributorActivityItem,
  ContributorDetail,
} from '@/types/api';

export const contributorsApi = {
  getAll: (params?: Record<string, string>) =>
    api.get<Page<ContributorItem>>('/contributors', { params }).then((r) => r.data),

  getById: (id: string) =>
    api.get<ContributorDetail>(`/contributors/${id}`).then((r) => r.data),

  getActivities: (id: string, params?: Record<string, string>) =>
    api
      .get<Page<ContributorActivityItem>>(`/contributors/${id}/activities`, { params })
      .then((r) => r.data),

  getByProject: (projectId: string, params?: Record<string, string>) =>
    api
      .get<Page<ContributorActivityItem>>(`/contributors/by-project/${projectId}`, { params })
      .then((r) => r.data),

  linkUser: (id: string, userId: string) =>
    api.post<ContributorItem>(`/contributors/${id}/link-user?userId=${userId}`).then((r) => r.data),

  unlinkUser: (id: string) =>
    api.delete<ContributorItem>(`/contributors/${id}/link-user`).then((r) => r.data),
};
