import api from './client';

export interface SearchResult {
  id: string;
  type: string;
  title: string;
  subtitle: string | null;
  link: string;
}

export interface SearchResults {
  results: SearchResult[];
  totalCount: number;
}

export const searchApi = {
  search: (q: string, type?: string, limit = 20) =>
    api.get<SearchResults>('/search', { params: { q, type, limit } }).then((r) => r.data),
};
