import { useState, useDeferredValue } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { GitBranch, Search, ChevronLeft, ChevronRight } from 'lucide-react';
import { contributorsApi } from '@/api/contributors';
import { Badge } from '@/components/ui/Badge';
import type { Page, ContributorItem } from '@/types/api';

function formatDate(iso: string | null) {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

export function ContributorsPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const deferredSearch = useDeferredValue(search);

  const params: Record<string, string> = {
    page: String(page),
    size: '20',
    sort: 'lastActivityAt,desc',
  };
  if (deferredSearch.trim()) params.search = deferredSearch.trim();

  const { data, isLoading, isError } = useQuery<Page<ContributorItem>>({
    queryKey: ['contributors', page, deferredSearch],
    queryFn: () => contributorsApi.getAll(params),
  });

  const contributors = data?.content ?? [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[var(--oqh-text)] flex items-center gap-2">
            <GitBranch className="h-7 w-7 text-[var(--oqh-blue)]" />
            Contributors
          </h1>
          <p className="mt-1 text-sm text-[var(--oqh-text-muted)]">
            People who commit, trigger pipelines, and merge code across your projects.
          </p>
        </div>
        <div className="text-sm text-[var(--oqh-text-muted)]">
          {data?.totalElements ?? 0} contributors
        </div>
      </div>

      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[#9BA8B9]" />
        <input
          type="text"
          placeholder="Search by username..."
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0); }}
          className="w-full pl-9 pr-4 py-2 border border-[var(--oqh-border)] rounded-lg text-sm focus:ring-2 focus:ring-[var(--oqh-blue)] focus:border-[var(--oqh-blue)] outline-none"
        />
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center h-64 text-[var(--oqh-text-muted)]">Loading contributors...</div>
      ) : isError ? (
        <div className="flex items-center justify-center h-64 text-red-500">Failed to load contributors.</div>
      ) : contributors.length === 0 ? (
        <div className="text-center py-16 text-[var(--oqh-text-muted)]">
          <GitBranch className="h-12 w-12 mx-auto mb-3 text-[#9BA8B9]" />
          <p className="text-lg font-medium">No contributors yet</p>
          <p className="text-sm mt-1">Contributors are auto-discovered from webhook events.</p>
        </div>
      ) : (
        <>
          <div className="bg-white border border-[var(--oqh-border)] rounded-xl shadow-[var(--oqh-shadow)] overflow-hidden">
            <table className="min-w-full divide-y divide-[var(--oqh-border)]">
              <thead className="bg-[#F7F9FC]">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Contributor</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Provider</th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Commits</th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Pipelines</th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Merges</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Last Active</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-[var(--oqh-text-muted)] uppercase tracking-wider">Linked User</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--oqh-border)]">
                {contributors.map((c) => (
                  <tr key={c.id} className="hover:bg-[#F7F9FC] transition-colors">
                    <td className="px-6 py-4">
                      <Link to={`/contributors/${c.id}`} className="flex items-center gap-3 group">
                        {c.avatarUrl ? (
                          <img src={c.avatarUrl} alt="" className="h-8 w-8 rounded-full" />
                        ) : (
                          <div className="h-8 w-8 rounded-full bg-[var(--oqh-active-bg)] flex items-center justify-center text-[var(--oqh-blue)] text-sm font-semibold">
                            {(c.displayName || c.username).charAt(0).toUpperCase()}
                          </div>
                        )}
                        <div>
                          <p className="text-sm font-medium text-[var(--oqh-text)] group-hover:text-[var(--oqh-blue)] transition-colors">
                            {c.displayName || c.username}
                          </p>
                          <p className="text-xs text-[var(--oqh-text-muted)]">@{c.username}</p>
                        </div>
                      </Link>
                    </td>
                    <td className="px-6 py-4">
                      <Badge variant="info">{c.provider}</Badge>
                    </td>
                    <td className="px-6 py-4 text-right text-sm font-mono text-[var(--oqh-text)]">{c.totalCommits}</td>
                    <td className="px-6 py-4 text-right text-sm font-mono text-[var(--oqh-text)]">{c.totalPipelines}</td>
                    <td className="px-6 py-4 text-right text-sm font-mono text-[var(--oqh-text)]">{c.totalMerges}</td>
                    <td className="px-6 py-4 text-sm text-[var(--oqh-text-muted)]">{formatDate(c.lastActivityAt)}</td>
                    <td className="px-6 py-4 text-sm text-[var(--oqh-text-muted)]">
                      {c.linkedUserName ? (
                        <Badge variant="success">{c.linkedUserName}</Badge>
                      ) : (
                        <span className="text-[#9BA8B9]">Not linked</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {data && data.totalPages > 1 && (
            <div className="flex items-center justify-between">
              <p className="text-sm text-[var(--oqh-text-muted)]">
                Page {data.number + 1} of {data.totalPages}
              </p>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={data.first}
                  className="px-3 py-1.5 text-sm border rounded-lg disabled:opacity-40 hover:bg-[#F7F9FC]"
                  aria-label="Previous page"
                >
                  <ChevronLeft className="h-4 w-4" />
                </button>
                <button
                  type="button"
                  onClick={() => setPage((p) => p + 1)}
                  disabled={data.last}
                  className="px-3 py-1.5 text-sm border rounded-lg disabled:opacity-40 hover:bg-[#F7F9FC]"
                  aria-label="Next page"
                >
                  <ChevronRight className="h-4 w-4" />
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
