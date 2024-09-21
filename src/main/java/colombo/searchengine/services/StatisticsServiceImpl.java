package colombo.searchengine.services;

import colombo.searchengine.repositories.LemmaRepository;
import colombo.searchengine.repositories.SiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import colombo.searchengine.dto.statistics.DetailedStatisticsItem;
import colombo.searchengine.dto.statistics.StatisticsData;
import colombo.searchengine.dto.statistics.StatisticsResponse;
import colombo.searchengine.dto.statistics.TotalStatistics;
import colombo.searchengine.model.SiteEntity;
import colombo.searchengine.repositories.PageRepository;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;

    @Autowired
    public StatisticsServiceImpl(PageRepository pageRepository, SiteRepository siteRepository, LemmaRepository lemmaRepository) {
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
    }

    @Override
    public StatisticsResponse getStatistics() {

        TotalStatistics total = new TotalStatistics();
        total.setSites(siteRepository.countRecords());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<SiteEntity> sitesList = siteRepository.findAll();
        for(int i = 0; i < sitesList.size(); i++) {
            SiteEntity site = sitesList.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            int pages = pageRepository.countBySiteId(site.getId());
            int lemmas = lemmaRepository.countBySiteId(site.getId());
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(site.getStatus().toString());
            String error = site.getLastError();
            item.setError(error);
            item.setStatusTime(ZonedDateTime.of(site.getStatusTime(), ZoneId.systemDefault()).toInstant().toEpochMilli());
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
