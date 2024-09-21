package colombo.searchengine.services.core;

import colombo.searchengine.dto.indexing.ForkIndexingException;
import colombo.searchengine.model.PageEntity;
import colombo.searchengine.services.CrawlAndDataManagerService;
import colombo.searchengine.utils.Utils;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import colombo.searchengine.model.SiteEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class ForkRecursiveIndexingTask extends RecursiveTask<Integer> {

    private String url;
    private String rootDomain;
    private ForkIndexingException forkIndexingException;
    private SiteEntity siteEntity;
    private ForkJoinPool forkJoinPool;
    private AtomicBoolean isInterruptionRequested;

    private final CrawlAndDataManagerService crawlAndDataService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ForkRecursiveIndexingTask.class);


    public ForkRecursiveIndexingTask(CrawlAndDataManagerService commonService,
                                     ForkJoinPool forkJoinPool, String rootDomain, String url, SiteEntity siteEntity,
                                     ForkIndexingException forkIndexingException, AtomicBoolean isInterruptionRequested) {
        this.crawlAndDataService = commonService;
        this.forkJoinPool = forkJoinPool;
        this.url = url;
        this.rootDomain = rootDomain;
        this.siteEntity = siteEntity;
        this.forkIndexingException = forkIndexingException;
        this.isInterruptionRequested = isInterruptionRequested;
    }


    @Override
    protected Integer compute() {
        return (forkIndexingException.getHasError().get() || isInterruptionRequested.get()) ? 0 : executeIndexing();
    }

    private Integer executeIndexing() {
        Integer urlNumber = 0;
        List<ForkRecursiveIndexingTask> tasks = new ArrayList<>();
        try {
            Connection.Response response = crawlAndDataService.crawlResponse(url);
            Map<String, Document> urlsMap = crawlAndDataService.hrefHtmlParser(response, rootDomain, siteEntity);
            for(Map.Entry<String, Document> entry : urlsMap.entrySet()) {
                String newUrl = entry.getKey();
                String subDirectory = Utils.getSubDirectory(newUrl, rootDomain);
                String html = entry.getValue().html();
                PageEntity pageEntity = crawlAndDataService.checkPathAndSavePage(subDirectory, response.statusCode(), html, siteEntity);
                if(pageEntity != null) {
                    urlNumber++;
                    if(response.statusCode() == 200) {
                        crawlAndDataService.saveLemmaAndUpdateIndexing(html, siteEntity.getId(), pageEntity.getId());
                    }
                    ForkRecursiveIndexingTask fk = new ForkRecursiveIndexingTask(crawlAndDataService, forkJoinPool, rootDomain, newUrl, siteEntity, forkIndexingException, isInterruptionRequested);
                    fk.fork();
                    tasks.add(fk);
                }
            }
        }  catch (HttpStatusException e) {
            LOGGER.error("Error: ", e);
            boolean hasSaved = crawlAndDataService.checkPathAndSavePage(Utils.getSubDirectory(url, rootDomain), e.getStatusCode(), e.getMessage(), siteEntity) == null;
            if(hasSaved) {
                urlNumber++;
            }
        }  catch (Exception e) {
            LOGGER.error("Error: ", e);
            forkIndexingException.getHasError().set(true);
            forkIndexingException.setException(e);
        }
        for(ForkRecursiveIndexingTask t : tasks) {
            urlNumber += t.join();
        }
        return urlNumber;
    }

}
