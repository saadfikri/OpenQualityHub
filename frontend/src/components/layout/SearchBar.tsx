import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, FileText, TestTube2, Bug, Users, FolderKanban } from 'lucide-react';
import { searchApi } from '@/api/search';
import type { SearchResult } from '@/api/search';

const TYPE_ICON: Record<string, typeof Search> = {
  PROJECT: FolderKanban,
  TEST_RUN: TestTube2,
  DEFECT: Bug,
  TEAM: Users,
  DOCUMENTATION: FileText,
};

export function SearchBar() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<SearchResult[]>([]);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout>>(undefined);
  const navigate = useNavigate();

  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  function handleChange(value: string) {
    setQuery(value);
    if (debounceRef.current) clearTimeout(debounceRef.current);
    if (value.trim().length < 2) {
      setResults([]);
      setOpen(false);
      return;
    }
    debounceRef.current = setTimeout(async () => {
      setLoading(true);
      try {
        const data = await searchApi.search(value.trim());
        setResults(data.results);
        setOpen(true);
      } catch {
        setResults([]);
      } finally {
        setLoading(false);
      }
    }, 300);
  }

  function handleSelect(result: SearchResult) {
    setOpen(false);
    setQuery('');
    navigate(result.link);
  }

  return (
    <div ref={ref} className="relative w-full max-w-md">
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[#9BA8B9]" />
        <input
          type="text"
          value={query}
          onChange={(e) => handleChange(e.target.value)}
          onFocus={() => { if (results.length > 0) setOpen(true); }}
          placeholder="Search projects, test runs, defects..."
          className="w-full pl-10 pr-4 py-2 text-sm bg-[#F7F9FC] border border-[var(--oqh-border)] rounded-[10px] focus:outline-none focus:ring-2 focus:ring-[var(--oqh-blue)] focus:bg-white transition-colors"
        />
        {loading && (
          <div className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 border-2 border-[var(--oqh-blue)] border-t-transparent rounded-full animate-spin" />
        )}
      </div>

      {open && results.length > 0 && (
        <div className="absolute top-full left-0 right-0 mt-1 bg-white rounded-xl border border-[var(--oqh-border)] shadow-[0_8px_30px_rgba(14,43,73,.1)] z-50 overflow-hidden max-h-80 overflow-y-auto">
          {results.map((r) => {
            const Icon = TYPE_ICON[r.type] ?? Search;
            return (
              <button
                key={`${r.type}-${r.id}`}
                onClick={() => handleSelect(r)}
                className="w-full px-4 py-3 flex items-start gap-3 hover:bg-[#F7F9FC] text-left transition-colors border-b border-[var(--oqh-border)]/30 last:border-0"
              >
                <Icon className="h-4 w-4 mt-0.5 text-[#9BA8B9] shrink-0" />
                <div className="min-w-0">
                  <p className="text-sm font-medium text-[var(--oqh-text)] truncate">{r.title}</p>
                  {r.subtitle && <p className="text-xs text-[var(--oqh-text-muted)] truncate">{r.subtitle}</p>}
                  <span className="text-[10px] text-[#9BA8B9] uppercase">{r.type.replace('_', ' ')}</span>
                </div>
              </button>
            );
          })}
        </div>
      )}

      {open && query.trim().length >= 2 && results.length === 0 && !loading && (
        <div className="absolute top-full left-0 right-0 mt-1 bg-white rounded-xl border border-[var(--oqh-border)] shadow-[0_8px_30px_rgba(14,43,73,.1)] z-50 px-4 py-6 text-center text-sm text-[#9BA8B9]">
          No results found for "{query}"
        </div>
      )}
    </div>
  );
}
