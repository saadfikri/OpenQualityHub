package com.openqualityhub.service.impl;

import com.openqualityhub.dto.response.SearchResultResponse;
import com.openqualityhub.dto.response.SearchResultsResponse;
import com.openqualityhub.service.SearchService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchServiceImpl implements SearchService {

    @PersistenceContext
    private EntityManager em;

    @Override
    public SearchResultsResponse search(String query, String type, int limit) {
        if (query == null || query.trim().length() < 2) {
            return SearchResultsResponse.builder()
                    .results(List.of())
                    .totalCount(0)
                    .build();
        }

        String searchType = (type == null || type.isBlank()) ? "ALL" : type.toUpperCase();
        List<SearchResultResponse> results = new ArrayList<>();
        int perTypeLimit = "ALL".equals(searchType) ? Math.max(limit / 5, 3) : limit;

        if ("ALL".equals(searchType) || "PROJECT".equals(searchType)) {
            results.addAll(searchProjects(query, perTypeLimit));
        }
        if ("ALL".equals(searchType) || "TEST_RUN".equals(searchType)) {
            results.addAll(searchTestRuns(query, perTypeLimit));
        }
        if ("ALL".equals(searchType) || "DEFECT".equals(searchType)) {
            results.addAll(searchDefects(query, perTypeLimit));
        }
        if ("ALL".equals(searchType) || "TEAM".equals(searchType)) {
            results.addAll(searchTeams(query, perTypeLimit));
        }
        if ("ALL".equals(searchType) || "DOCUMENTATION".equals(searchType)) {
            results.addAll(searchDocumentation(query, perTypeLimit));
        }

        // Trim to overall limit
        if (results.size() > limit) {
            results = results.subList(0, limit);
        }

        return SearchResultsResponse.builder()
                .results(results)
                .totalCount(results.size())
                .build();
    }

    private List<SearchResultResponse> searchProjects(String query, int limit) {
        String sql = "SELECT id, name, description FROM projects " +
                "WHERE NOT is_deleted AND to_tsvector('english', name || ' ' || COALESCE(description, '')) @@ plainto_tsquery('english', :query) " +
                "LIMIT :limit";
        return executeSearch(sql, query, limit, "PROJECT", "/projects");
    }

    private List<SearchResultResponse> searchTestRuns(String query, int limit) {
        String sql = "SELECT id, name, environment FROM test_runs " +
                "WHERE NOT is_deleted AND to_tsvector('english', name || ' ' || COALESCE(environment, '')) @@ plainto_tsquery('english', :query) " +
                "LIMIT :limit";
        return executeSearch(sql, query, limit, "TEST_RUN", "/test-runs");
    }

    private List<SearchResultResponse> searchDefects(String query, int limit) {
        String sql = "SELECT id, title, description FROM defects " +
                "WHERE NOT is_deleted AND to_tsvector('english', title || ' ' || COALESCE(description, '')) @@ plainto_tsquery('english', :query) " +
                "LIMIT :limit";
        return executeSearch(sql, query, limit, "DEFECT", "/defects");
    }

    private List<SearchResultResponse> searchTeams(String query, int limit) {
        String sql = "SELECT id, name, description FROM teams " +
                "WHERE NOT is_deleted AND to_tsvector('english', name || ' ' || COALESCE(description, '')) @@ plainto_tsquery('english', :query) " +
                "LIMIT :limit";
        return executeSearch(sql, query, limit, "TEAM", "/teams");
    }

    private List<SearchResultResponse> searchDocumentation(String query, int limit) {
        String sql = "SELECT id, title, description FROM documentation_assets " +
                "WHERE NOT is_deleted AND to_tsvector('english', title || ' ' || COALESCE(description, '')) @@ plainto_tsquery('english', :query) " +
                "LIMIT :limit";
        return executeSearch(sql, query, limit, "DOCUMENTATION", "/documentation");
    }

    @SuppressWarnings("unchecked")
    private List<SearchResultResponse> executeSearch(String sql, String queryStr, int limit, String type, String basePath) {
        Query query = em.createNativeQuery(sql);
        query.setParameter("query", queryStr);
        query.setParameter("limit", limit);

        List<Object[]> rows = query.getResultList();
        List<SearchResultResponse> results = new ArrayList<>();

        for (Object[] row : rows) {
            UUID id = (UUID) row[0];
            String title = (String) row[1];
            String subtitle = row[2] != null ? truncate((String) row[2], 120) : null;

            String link;
            switch (type) {
                case "TEST_RUN" -> link = basePath + "/" + id;
                case "DOCUMENTATION" -> link = basePath + "/" + id;
                default -> link = basePath;
            }

            results.add(SearchResultResponse.builder()
                    .id(id)
                    .type(type)
                    .title(title)
                    .subtitle(subtitle)
                    .link(link)
                    .build());
        }

        return results;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
