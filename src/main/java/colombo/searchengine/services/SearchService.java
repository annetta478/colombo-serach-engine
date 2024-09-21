package colombo.searchengine.services;

import colombo.searchengine.dto.search.SearchResponse;

public interface SearchService {

    SearchResponse search(String site, String query, int offset, int limit);

}
