import api from './client';
import type { Page, TestRun, TestRunDetail } from '@/types/api';

export const testRunsApi = {
  getAll: (page = 0, size = 20, projectId?: string) =>
    api.get<Page<TestRun>>('/test-runs', { params: { page, size, projectId } }).then((r) => r.data),

  getById: (id: string) =>
    api.get<TestRunDetail>(`/test-runs/${id}`).then((r) => r.data),

  importJUnit: (projectId: string, name: string, file: File, environment?: string) => {
    const formData = new FormData();
    formData.append('projectId', projectId);
    formData.append('name', name);
    formData.append('file', file);
    if (environment) formData.append('environment', environment);
    return api.post<TestRun>('/test-runs/import/junit', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then((r) => r.data);
  },

  importPlaywright: (projectId: string, name: string, file: File, environment?: string) => {
    const formData = new FormData();
    formData.append('projectId', projectId);
    formData.append('name', name);
    formData.append('file', file);
    if (environment) formData.append('environment', environment);
    return api.post<TestRun>('/test-runs/import/playwright', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then((r) => r.data);
  },

  importWebdriverIO: (projectId: string, name: string, file: File, environment?: string) => {
    const formData = new FormData();
    formData.append('projectId', projectId);
    formData.append('name', name);
    formData.append('file', file);
    if (environment) formData.append('environment', environment);
    return api.post<TestRun>('/test-runs/import/webdriverio', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then((r) => r.data);
  },

  importCypress: (projectId: string, name: string, file: File, environment?: string) => {
    const formData = new FormData();
    formData.append('projectId', projectId);
    formData.append('name', name);
    formData.append('file', file);
    if (environment) formData.append('environment', environment);
    return api.post<TestRun>('/test-runs/import/cypress', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then((r) => r.data);
  },

  importTestNG: (projectId: string, name: string, file: File, environment?: string) => {
    const formData = new FormData();
    formData.append('projectId', projectId);
    formData.append('name', name);
    formData.append('file', file);
    if (environment) formData.append('environment', environment);
    return api.post<TestRun>('/test-runs/import/testng', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then((r) => r.data);
  },

  delete: (id: string) =>
    api.delete(`/test-runs/${id}`),
};
