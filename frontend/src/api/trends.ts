import api from './client';
import type { PassRateTrendData, DefectTrendData, TrendPointData } from '@/types/api';

export const trendsApi = {
  passRate: (days = 30, projectId?: string) =>
    api
      .get<PassRateTrendData>('/trends/pass-rate', { params: { days, projectId } })
      .then((r) => r.data),

  defects: (days = 30) =>
    api
      .get<DefectTrendData>('/trends/defects', { params: { days } })
      .then((r) => r.data),

  testVolume: (days = 30, projectId?: string) =>
    api
      .get<TrendPointData>('/trends/test-volume', { params: { days, projectId } })
      .then((r) => r.data),

  flaky: (days = 30, projectId?: string) =>
    api
      .get<TrendPointData>('/trends/flaky', { params: { days, projectId } })
      .then((r) => r.data),
};
