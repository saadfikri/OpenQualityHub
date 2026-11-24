import api from './client';
import type {
  Page,
  TestDataTemplate,
  TestDataTemplateDetail,
  TestDataProfileItem,
  TestDataGenerationItem,
  TestDataDashboard,
} from '@/types/api';

export const testDataApi = {
  getDashboard: () =>
    api.get<TestDataDashboard>('/test-data/dashboard').then((r) => r.data),

  getTemplates: (params?: Record<string, string>) =>
    api.get<Page<TestDataTemplate>>('/test-data/templates', { params }).then((r) => r.data),

  getTemplateById: (id: string) =>
    api.get<TestDataTemplateDetail>(`/test-data/templates/${id}`).then((r) => r.data),

  createTemplate: (data: {
    name: string;
    description?: string;
    projectId?: string;
    fields: {
      name: string;
      fieldType: string;
      displayOrder: number;
      nullable: boolean;
      fakerGenerator?: string;
      constraints?: Record<string, unknown>;
    }[];
  }) => api.post<TestDataTemplate>('/test-data/templates', data).then((r) => r.data),

  updateTemplate: (id: string, data: Record<string, unknown>) =>
    api.put<TestDataTemplate>(`/test-data/templates/${id}`, data).then((r) => r.data),

  deleteTemplate: (id: string) => api.delete(`/test-data/templates/${id}`),

  createProfile: (data: {
    name: string;
    templateId: string;
    profileType: string;
    description?: string;
    overrides?: string;
  }) => api.post<TestDataProfileItem>('/test-data/profiles', data).then((r) => r.data),

  updateProfile: (id: string, data: Record<string, unknown>) =>
    api.put<TestDataProfileItem>(`/test-data/profiles/${id}`, data).then((r) => r.data),

  deleteProfile: (id: string) => api.delete(`/test-data/profiles/${id}`),

  generate: (data: {
    templateId: string;
    profileId?: string;
    rowCount: number;
    exportFormat?: string;
  }) => api.post<TestDataGenerationItem>('/test-data/generate', data).then((r) => r.data),

  getGeneration: (id: string) =>
    api.get<TestDataGenerationItem>(`/test-data/generations/${id}`).then((r) => r.data),

  getGenerationsByTemplate: (templateId: string, params?: Record<string, string>) =>
    api
      .get<Page<TestDataGenerationItem>>(`/test-data/templates/${templateId}/generations`, { params })
      .then((r) => r.data),

  downloadGeneration: (id: string) =>
    api.get(`/test-data/generations/${id}/download`, { responseType: 'blob' }).then((r) => r.data),
};
