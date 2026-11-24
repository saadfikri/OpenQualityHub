package com.openqualityhub.service;

import com.openqualityhub.dto.response.SearchResultsResponse;

public interface SearchService {

    SearchResultsResponse search(String query, String type, int limit);
}
