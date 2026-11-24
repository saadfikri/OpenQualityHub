import api from './client';

export const exportApi = {
  testRunsCsv: (projectId?: string) =>
    api.get('/export/test-runs', {
      params: projectId ? { projectId } : {},
      responseType: 'blob',
    }).then((r) => {
      const url = window.URL.createObjectURL(new Blob([r.data]));
      const a = document.createElement('a');
      a.href = url;
      a.download = 'test-runs.csv';
      a.click();
      window.URL.revokeObjectURL(url);
    }),

  defectsCsv: (projectId?: string, status?: string) =>
    api.get('/export/defects', {
      params: { ...(projectId && { projectId }), ...(status && { status }) },
      responseType: 'blob',
    }).then((r) => {
      const url = window.URL.createObjectURL(new Blob([r.data]));
      const a = document.createElement('a');
      a.href = url;
      a.download = 'defects.csv';
      a.click();
      window.URL.revokeObjectURL(url);
    }),
};
