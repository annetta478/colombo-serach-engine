package colombo.searchengine.controllers;

import colombo.searchengine.dto.indexing.IndexingResponse;
import colombo.searchengine.dto.search.SearchResponse;
import colombo.searchengine.dto.statistics.StatisticsResponse;
import colombo.searchengine.services.SearchServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import colombo.searchengine.services.IndexingServiceImpl;
import colombo.searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingServiceImpl indexingService;
    private final SearchServiceImpl searchService;

    @Autowired
    public ApiController(StatisticsService statisticsService, IndexingServiceImpl indexingService, SearchServiceImpl searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        indexingService.startIndexing();
        return ResponseEntity.ok(new IndexingResponse());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        indexingService.stopIndexing();
        return ResponseEntity.ok(new IndexingResponse());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestParam(name = "url") String url) {
        indexingService.indexPage(url);
        return ResponseEntity.ok(new IndexingResponse());
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam(value = "site", required = false) String site,
                                                 @RequestParam(value = "query", required = true) String query,
                                                 @RequestParam(name = "offset", required = false, defaultValue = "0") int offset,
                                                 @RequestParam(name = "limit", required = false, defaultValue = "20") int limit) {
        return ResponseEntity.ok(searchService.search(site, query, offset, limit));
    }

}
