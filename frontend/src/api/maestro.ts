import api from './client';
import type {
  Page,
  MaestroFlowResult,
  MaestroDashboardResponse,
  MaestroDeviceMatrixResponse,
  MaestroFlowAnalyticsResponse,
} from '@/types/api';

export const maestroApi = {
  getDashboard: () =>
    api.get<MaestroDashboardResponse>('/maestro/dashboard').then((r) => r.data),

  getDeviceMatrix: () =>
    api.get<MaestroDeviceMatrixResponse>('/maestro/device-matrix').then((r) => r.data),

  getFlowAnalytics: () =>
    api.get<MaestroFlowAnalyticsResponse>('/maestro/flow-analytics').then((r) => r.data),

  getFlowResults: (page = 0, size = 20, platform?: string) =>
    api.get<Page<MaestroFlowResult>>('/maestro/flows', { params: { page, size, platform } }).then((r) => r.data),

  getFlowsByTestRun: (testRunId: string) =>
    api.get<MaestroFlowResult[]>(`/maestro/test-runs/${testRunId}/flows`).then((r) => r.data),

  importResults: (projectId: string, name: string, file: File, environment?: string) => {
    const formData = new FormData();
    formData.append('projectId', projectId);
    formData.append('name', name);
    formData.append('file', file);
    if (environment) formData.append('environment', environment);
    return api.post('/maestro/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then((r) => r.data);
  },
};
