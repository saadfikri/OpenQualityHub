import api from './client';
import type { Page, UserProfile } from '@/types/api';

export const usersApi = {
  getAll: (params?: Record<string, string>) =>
    api.get<Page<UserProfile>>('/users', { params }).then((r) => r.data),

  getById: (id: string) =>
    api.get<UserProfile>(`/users/${id}`).then((r) => r.data),

  getMe: () =>
    api.get<UserProfile>('/users/me').then((r) => r.data),

  update: (id: string, data: { firstName?: string; lastName?: string; displayName?: string }) =>
    api.put<UserProfile>(`/users/${id}`, data).then((r) => r.data),

  changePassword: (data: { oldPassword: string; newPassword: string }) =>
    api.put<void>('/users/me/password', data),

  assignRole: (id: string, roleId: string) =>
    api.put<UserProfile>(`/users/${id}/role`, { roleId }).then((r) => r.data),

  deactivate: (id: string) =>
    api.put<UserProfile>(`/users/${id}/deactivate`).then((r) => r.data),

  activate: (id: string) =>
    api.put<UserProfile>(`/users/${id}/activate`).then((r) => r.data),
};
