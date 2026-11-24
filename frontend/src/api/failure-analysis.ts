import api from './client';

export interface FailureAnalysisData {
  id: string;
  testRunId: string;
  testRunName: string;
  status: string;
  failureCategory: string | null;
  summary: string | null;
  rootCause: string | null;
  suggestedFix: string | null;
  confidenceScore: number | null;
  aiModel: string | null;
  promptTokens: number | null;
  completionTokens: number | null;
  humanFeedback: string;
  feedbackComment: string | null;
  feedbackBy: string | null;
  feedbackAt: string | null;
  createdAt: string;
}

export const failureAnalysisApi = {
  trigger: (testRunId: string) =>
    api.post<FailureAnalysisData>('/failure-analysis/trigger', { testRunId }).then((r) => r.data),

  getByTestRun: (testRunId: string) =>
    api.get<FailureAnalysisData>(`/failure-analysis/test-run/${testRunId}`).then((r) => r.data),

  submitFeedback: (analysisId: string, feedback: string, comment?: string) =>
    api.post<FailureAnalysisData>(`/failure-analysis/${analysisId}/feedback`, { feedback, comment }).then((r) => r.data),

  getStatus: () =>
    api.get<{ available: boolean }>('/failure-analysis/status').then((r) => r.data),
};
