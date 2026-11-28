import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from '@/context/AuthContext';
import { AppLayout } from '@/components/layout/AppLayout';
import { DashboardPage } from '@/pages/dashboard/DashboardPage';
import { TeamsPage } from '@/pages/teams/TeamsPage';
import { ProjectsPage } from '@/pages/projects/ProjectsPage';
import { TestRunsPage } from '@/pages/test-runs/TestRunsPage';
import { TestRunDetailPage } from '@/pages/test-runs/TestRunDetailPage';
import { QaRequestsPage } from '@/pages/qa-requests/QaRequestsPage';
import { QaRequestDetailPage } from '@/pages/qa-requests/QaRequestDetailPage';
import { QaDashboardPage } from '@/pages/qa-requests/QaDashboardPage';
import { DocumentationPage } from '@/pages/documentation/DocumentationPage';
import { DocumentationDetailPage } from '@/pages/documentation/DocumentationDetailPage';
import { ReleasesPage } from '@/pages/releases/ReleasesPage';
import { ReleaseDetailPage } from '@/pages/releases/ReleaseDetailPage';
import { IntegrationsPage } from '@/pages/integrations/IntegrationsPage';
import { IntegrationDetailPage } from '@/pages/integrations/IntegrationDetailPage';
import { UsersPage } from '@/pages/settings/UsersPage';
import { ContributorsPage } from '@/pages/contributors/ContributorsPage';
import { ContributorDetailPage } from '@/pages/contributors/ContributorDetailPage';
import { AuditTrailPage } from '@/pages/audit/AuditTrailPage';
import { TestDataPage } from '@/pages/test-data/TestDataPage';
import { TestDataDetailPage } from '@/pages/test-data/TestDataDetailPage';
import MaestroPage from '@/pages/maestro/MaestroPage';
import MaestroFlowsPage from '@/pages/maestro/MaestroFlowsPage';
import { LoginPage } from '@/pages/auth/LoginPage';
import { ErrorBoundary } from '@/components/ErrorBoundary';
import type { ReactNode } from 'react';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
    },
  },
});

function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

function App() {
  return (
    <ErrorBoundary>
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route
              element={
                <ProtectedRoute>
                  <AppLayout />
                </ProtectedRoute>
              }
            >
              <Route path="/" element={<DashboardPage />} />
              <Route path="/teams" element={<TeamsPage />} />
              <Route path="/projects" element={<ProjectsPage />} />
              <Route path="/test-runs" element={<TestRunsPage />} />
              <Route path="/test-runs/:id" element={<TestRunDetailPage />} />
              <Route path="/maestro" element={<MaestroPage />} />
              <Route path="/maestro/flows" element={<MaestroFlowsPage />} />
              <Route path="/qa-requests" element={<QaRequestsPage />} />
              <Route path="/qa-requests/:id" element={<QaRequestDetailPage />} />
              <Route path="/qa-dashboard" element={<QaDashboardPage />} />
              <Route path="/documentation" element={<DocumentationPage />} />
              <Route path="/documentation/:id" element={<DocumentationDetailPage />} />
              <Route path="/releases" element={<ReleasesPage />} />
              <Route path="/releases/:id" element={<ReleaseDetailPage />} />
              <Route path="/integrations" element={<IntegrationsPage />} />
              <Route path="/integrations/:id" element={<IntegrationDetailPage />} />
              <Route path="/contributors" element={<ContributorsPage />} />
              <Route path="/contributors/:id" element={<ContributorDetailPage />} />
              <Route path="/settings/users" element={<UsersPage />} />
              <Route path="/test-data" element={<TestDataPage />} />
              <Route path="/test-data/:id" element={<TestDataDetailPage />} />
              <Route path="/audit" element={<AuditTrailPage />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
    </ErrorBoundary>
  );
}

export default App;
