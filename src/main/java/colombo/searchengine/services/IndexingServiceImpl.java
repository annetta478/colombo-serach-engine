package colombo.searchengine.services;

import colombo.searchengine.config.Site;
import colombo.searchengine.config.SitesList;
import colombo.searchengine.exceptions.CustomException;
import colombo.searchengine.exceptions.Messages;
import colombo.searchengine.model.PageEntity;
import colombo.searchengine.model.SiteEntity;
import colombo.searchengine.model.Status;
import colombo.searchengine.repositories.SiteRepository;
import colombo.searchengine.services.core.IndexingThread;
import colombo.searchengine.utils.Utils;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import colombo.searchengine.repositories.PageRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class IndexingServiceImpl implements IndexingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexingServiceImpl.class);
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final CrawlAndDataManagerService crawlAndDataService;


    public void startIndexing() {

        if(isIndexingStarted()) {
            throw new CustomException(Messages.INDEXING_ALREADY_STARTED, HttpStatus.BAD_REQUEST);
        }

        List<String> urls = new ArrayList<>();
        List<SiteEntity> entities = new ArrayList<>();
        List<SiteEntity> siteEntityList = null;

        sites.getSites().forEach(x -> {
            String url = Utils.removeLastSlash(x.getUrl());
            urls.add(url);
            entities.add(new SiteEntity(Status.INDEXING, LocalDateTime.now(), url, x.getName()));
        });

        siteRepository.deleteAll();
        siteEntityList = siteRepository.saveAllAndFlush(entities);

        siteEntityList.forEach(site -> {
            IndexingThread t = new IndexingThread(siteRepository, pageRepository, crawlAndDataService, site);
            t.start();
        });
    }

    public void stopIndexing() {
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        int numberOfInterruptedThreads = 0;
        for(Site s : sites.getSites()) {
            for(Thread t : threads) {
                if(s.getUrl().startsWith(t.getName())) {
                    IndexingThread indexThread = (IndexingThread) t;
                    indexThread.stopIndexing();
                    numberOfInterruptedThreads++;
                }
            };
        }
        if(numberOfInterruptedThreads == 0) {
            throw new CustomException(Messages.INDEXING_NOT_ALREADY_STARTED, HttpStatus.BAD_REQUEST);
        }
    }

    public void indexPage(String url) {
        LOGGER.info("Received URL: {}", url);
        String rootDomain = Utils.checkUrlAndRetriveRootDomain(url);
        if(rootDomain == null) {
            throw new CustomException(Messages.INVALID_URL, HttpStatus.BAD_REQUEST);
        }
        if(isExternalUrl(rootDomain)) {
            throw new CustomException(Messages.EXTERNAL_URL, HttpStatus.BAD_REQUEST);
        }

        SiteEntity siteEntity = siteRepository.findByRootDomain(rootDomain);
        String subDirectory = Utils.getSubDirectory(url, siteEntity.getUrl());
        PageEntity page = pageRepository.findIdByPathAndSiteId(subDirectory, siteEntity.getId());
        if(page != null) {
            pageRepository.delete(page);
            LOGGER.info("Deleted page with id = {} for path = {} and site_id = {}", page.getId(), subDirectory, siteEntity.getId());
        }
        try {
            Connection.Response response = crawlAndDataService.crawlResponse(url);
            String html = response.parse().html();
            PageEntity pageEntity = crawlAndDataService.checkPathAndSavePage(subDirectory, response.statusCode(), html, siteEntity);
            if(response.statusCode() == 200) {
                crawlAndDataService.saveLemmaAndUpdateIndexing(html, siteEntity.getId(), pageEntity.getId());
            }
        }  catch (HttpStatusException e) {
            LOGGER.error("Error: ", e);
            crawlAndDataService.checkPathAndSavePage(subDirectory, e.getStatusCode(), e.getMessage(), siteEntity);
        }  catch (Exception e) {
            LOGGER.error("Error: ", e);
            throw new CustomException(Messages.GENERIC_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isExternalUrl(String rootDomain) {
        for(Site site : sites.getSites()) {
            if(site.getUrl().contains(rootDomain)) {
                return false;
            }
        }
        return true;
    }

    private boolean isIndexingStarted() {
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        for(Site s : sites.getSites()) {
            for(Thread t : threads) {
                if(s.getUrl().contains(t.getName())) {
                   return true;
                }
            };
        }
        return false;
    }

}
