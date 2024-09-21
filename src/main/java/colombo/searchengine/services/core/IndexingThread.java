package colombo.searchengine.services.core;

import colombo.searchengine.dto.indexing.ForkIndexingException;
import colombo.searchengine.exceptions.Messages;
import colombo.searchengine.model.SiteEntity;
import colombo.searchengine.model.Status;
import colombo.searchengine.repositories.SiteRepository;
import colombo.searchengine.services.CrawlAndDataManagerService;
import colombo.searchengine.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import colombo.searchengine.repositories.PageRepository;

import java.time.LocalDateTime;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

public class IndexingThread extends Thread {

    private ForkIndexingException forkIndexingException;
    private AtomicBoolean isInterruptionRequested;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SiteEntity siteEntity;
    private final CrawlAndDataManagerService synchronizedService;
    private final ForkJoinPool forkJoinPool;

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexingThread.class);

    public IndexingThread(SiteRepository siteRepository, PageRepository pageRepository, CrawlAndDataManagerService synchronizedService, SiteEntity siteEntity) {
        setName(siteEntity.getUrl());
        this.synchronizedService = synchronizedService;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.siteEntity = siteEntity;
        isInterruptionRequested = new AtomicBoolean(false);
        forkIndexingException = new ForkIndexingException();
        forkJoinPool = new ForkJoinPool();
    }

    @Override
    public void run() {
        String rootDomain = Utils.checkUrlAndRetriveRootDomain(siteEntity.getUrl());
        if(rootDomain == null) {
            saveSiteWithMalformedUrl();
            forkJoinPool.shutdown();
            return;
        }

        Integer savedUrls = forkJoinPool.invoke(new ForkRecursiveIndexingTask(synchronizedService, forkJoinPool, rootDomain, siteEntity.getUrl(), siteEntity, forkIndexingException, isInterruptionRequested));
        forkJoinPool.shutdown();
        if(forkIndexingException.getHasError().get()) {
            siteRepository.updateStatusAndMessageAndTime(siteEntity.getId(), Status.FAILED.name(), forkIndexingException.getStackTrace());
        } else {
            siteRepository.updateStatusAndTime(siteEntity.getId(),Status.INDEXED.name());
        }
        LOGGER.info("Saved {} paths for {} url", savedUrls, siteEntity.getUrl());
    }

    private void saveSiteWithMalformedUrl() {
        LOGGER.error("Malformed URL: {}", siteEntity.getUrl());
        siteEntity.setStatus(Status.FAILED);
        siteEntity.setLastError("Malformed URL");
        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepository.saveAndFlush(siteEntity);
    }

    public void stopIndexing() {
        isInterruptionRequested.set(true);
        forkJoinPool.shutdownNow();
        siteRepository.updateStatusAndMessageAndTime(siteEntity.getId(), Status.FAILED.name(), Messages.INDEXING_INTERRUPTED_BY_USER);
        interrupt();
    }

}
