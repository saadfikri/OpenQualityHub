package com.openqualityhub.service.impl;

import com.openqualityhub.dto.response.SearchResultsResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

    @Mock private EntityManager em;

    @InjectMocks
    private SearchServiceImpl searchService;

    @BeforeEach
    void setUp() throws Exception {
        Field emField = SearchServiceImpl.class.getDeclaredField("em");
        emField.setAccessible(true);
        emField.set(searchService, em);
    }

    /** Helper to build a properly-typed List<Object[]> for query.getResultList(). */
    @SuppressWarnings("unchecked")
    private static List rows(Object[]... items) {
        List list = new ArrayList();
        Collections.addAll(list, items);
        return list;
    }

    private Query stubQuery() {
        Query mockQuery = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        return mockQuery;
    }

    // --- null/short query ---

    @Test
    void search_nullQuery_returnsEmptyResults() {
        SearchResultsResponse response = searchService.search(null, "ALL", 10);

        assertEquals(0, response.getTotalCount());
        assertTrue(response.getResults().isEmpty());
        verifyNoInteractions(em);
    }

    @Test
    void search_singleCharQuery_returnsEmptyResults() {
        SearchResultsResponse response = searchService.search("a", "ALL", 10);

        assertEquals(0, response.getTotalCount());
        assertTrue(response.getResults().isEmpty());
    }

    @Test
    void search_emptyQuery_returnsEmptyResults() {
        SearchResultsResponse response = searchService.search("", "ALL", 10);

        assertEquals(0, response.getTotalCount());
        assertTrue(response.getResults().isEmpty());
    }

    @Test
    void search_blankQuery_returnsEmptyResults() {
        SearchResultsResponse response = searchService.search("  ", "ALL", 10);

        assertEquals(0, response.getTotalCount());
        assertTrue(response.getResults().isEmpty());
    }

    // --- type-specific searches ---

    @Test
    void search_projectType_onlySearchesProjects() {
        Query mockQuery = stubQuery();
        when(mockQuery.getResultList()).thenReturn(Collections.emptyList());

        SearchResultsResponse response = searchService.search("test query", "PROJECT", 10);

        assertEquals(0, response.getTotalCount());
        verify(em, times(1)).createNativeQuery(anyString());
    }

    @Test
    void search_testRunType_onlySearchesTestRuns() {
        Query mockQuery = stubQuery();
        when(mockQuery.getResultList()).thenReturn(Collections.emptyList());

        searchService.search("test query", "TEST_RUN", 10);

        verify(em, times(1)).createNativeQuery(contains("test_runs"));
    }

    @Test
    void search_defectType_onlySearchesDefects() {
        Query mockQuery = stubQuery();
        when(mockQuery.getResultList()).thenReturn(Collections.emptyList());

        searchService.search("test query", "DEFECT", 10);

        verify(em, times(1)).createNativeQuery(contains("defects"));
    }

    @Test
    void search_teamType_onlySearchesTeams() {
        Query mockQuery = stubQuery();
        when(mockQuery.getResultList()).thenReturn(Collections.emptyList());

        searchService.search("test query", "TEAM", 10);

        verify(em, times(1)).createNativeQuery(contains("teams"));
    }

    @Test
    void search_documentationType_onlySearchesDocs() {
        Query mockQuery = stubQuery();
        when(mockQuery.getResultList()).thenReturn(Collections.emptyList());

        searchService.search("test query", "DOCUMENTATION", 10);

        verify(em, times(1)).createNativeQuery(contains("documentation_assets"));
    }

    // --- ALL type ---

    @Test
    void search_allType_searchesAllFiveEntities() {
        Query mockQuery = stubQuery();
        when(mockQuery.getResultList()).thenReturn(Collections.emptyList());

        searchService.search("test query", "ALL", 25);

        verify(em, times(5)).createNativeQuery(anyString());
    }

    @Test
    void search_nullType_treatedAsAll() {
        Query mockQuery = stubQuery();
        when(mockQuery.getResultList()).thenReturn(Collections.emptyList());

        searchService.search("test query", null, 25);

        verify(em, times(5)).createNativeQuery(anyString());
    }

    @Test
    void search_blankType_treatedAsAll() {
        Query mockQuery = stubQuery();
        when(mockQuery.getResultList()).thenReturn(Collections.emptyList());

        searchService.search("test query", "  ", 25);

        verify(em, times(5)).createNativeQuery(anyString());
    }

    // --- result mapping ---

    @Test
    void search_mapsResultsCorrectly() {
        UUID id = UUID.randomUUID();
        Query mockQuery = stubQuery();
        when(mockQuery.getResultList()).thenReturn(rows(
                new Object[]{id, "My Project", "A description"}
        ));

        SearchResultsResponse response = searchService.search("project", "PROJECT", 10);

        assertEquals(1, response.getTotalCount());
        assertEquals(id, response.getResults().get(0).getId());
        assertEquals("My Project", response.getResults().get(0).getTitle());
        assertEquals("A description", response.getResults().get(0).getSubtitle());
        assertEquals("PROJECT", response.getResults().get(0).getType());
    }

    @Test
    void search_testRunType_linkContainsId() {
        UUID id = UUID.randomUUID();
        Query mockQuery = stubQuery();
        when(mockQuery.getResultList()).thenReturn(rows(
                new Object[]{id, "Test Run 1", null}
        ));

        SearchResultsResponse response = searchService.search("test", "TEST_RUN", 10);

        assertTrue(response.getResults().get(0).getLink().contains(id.toString()));
        assertNull(response.getResults().get(0).getSubtitle());
    }

    @Test
    void search_trimsTotalToLimit() {
        Query mockQuery = stubQuery();
        when(mockQuery.getResultList()).thenReturn(rows(
                new Object[]{UUID.randomUUID(), "Result 1", "desc"},
                new Object[]{UUID.randomUUID(), "Result 2", "desc"},
                new Object[]{UUID.randomUUID(), "Result 3", "desc"},
                new Object[]{UUID.randomUUID(), "Result 4", "desc"},
                new Object[]{UUID.randomUUID(), "Result 5", "desc"}
        ));

        // limit=3 but each of 5 types returns 5 results = 25, trimmed to 3
        SearchResultsResponse response = searchService.search("query", "ALL", 3);

        assertEquals(3, response.getResults().size());
        assertEquals(3, response.getTotalCount());
    }

    @Test
    void search_longSubtitle_truncated() {
        UUID id = UUID.randomUUID();
        String longDesc = "A".repeat(200);
        Query mockQuery = stubQuery();
        when(mockQuery.getResultList()).thenReturn(rows(
                new Object[]{id, "Title", longDesc}
        ));

        SearchResultsResponse response = searchService.search("test", "PROJECT", 10);

        assertEquals(123, response.getResults().get(0).getSubtitle().length());
        assertTrue(response.getResults().get(0).getSubtitle().endsWith("..."));
    }
}
