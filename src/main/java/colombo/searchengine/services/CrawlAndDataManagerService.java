package colombo.searchengine.services;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import colombo.searchengine.model.PageEntity;
import colombo.searchengine.model.SiteEntity;
import colombo.searchengine.repositories.IndexRepository;
import colombo.searchengine.repositories.LemmaRepository;
import colombo.searchengine.repositories.PageRepository;
import colombo.searchengine.repositories.SiteRepository;
import colombo.searchengine.services.core.LemmaFinder;
import colombo.searchengine.utils.Constants;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CrawlAndDataManagerService {

    @Value("${indexing-settings.thread-sleep-time}")
    private long threadSleep;
    @Value("${indexing-settings.number-of-attempts}")
    private int numberOfAttempts;
    @Value("${jsoup-settings.userAgent}")
    private String userAgent;
    @Value("${jsoup-settings.referrer}")
    private String referrer;

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlAndDataManagerService.class);

    private static final String HREF = "href";
    private static final String HREF_TAG = "a[href]";
    private static final int MAX_QUERY_SIZE = 20_000;

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private LemmaFinder lemmaFinder;

    private Pattern pattern = Pattern.compile(Constants.URL_PATTERN);

    @Autowired
    public CrawlAndDataManagerService(SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        try {
            lemmaFinder = LemmaFinder.getInstance();
        } catch (IOException e) {
            LOGGER.error("Error in : ", e);
            throw new RuntimeException(e);
        }
    }

    public synchronized PageEntity checkPathAndSavePage(String subPath, Integer statusCode, String content, SiteEntity siteEntity) {
        if (!pageRepository.existsByPathAndSite(subPath, siteEntity)) {
            PageEntity result = pageRepository.saveAndFlush(new PageEntity(subPath, statusCode, content, siteEntity));
            siteRepository.updateStatusTime(siteEntity.getId());
            return result;
        }
        LOGGER.info("The {} URL was not saved on the db as it is already present", siteEntity.getUrl() + subPath);
        return null;
    }

    public void saveLemmaAndUpdateIndexing(String html, Integer siteId, Integer pageId) {
        Map<String, Integer> lemmas = lemmaFinder.collectLemmas(lemmaFinder.removeHTMLTags(html));
        StringBuilder valuesLemmas = new StringBuilder();
        StringBuilder valuesIndexes = new StringBuilder();
        for (String lemma : lemmas.keySet()) {
            if (valuesIndexes.length() > MAX_QUERY_SIZE) {
                saveLemmasAndIndexes(valuesLemmas, valuesIndexes);
                valuesLemmas = new StringBuilder();
                valuesIndexes = new StringBuilder();
            }
            valuesLemmas.append(String.format("('%s', %s),", lemma, siteId));
            valuesIndexes.append(String.format("((Select id from lemma where lemma.lemma = '%s' and lemma.site_id = %s), %s, %s), ", lemma, siteId, pageId, lemmas.get(lemma).floatValue()));
        }
        if (!valuesIndexes.isEmpty()) {
            saveLemmasAndIndexes(valuesLemmas, valuesIndexes);
        }
    }

    private synchronized void saveLemmasAndIndexes(StringBuilder valuesLemmas, StringBuilder valuesIndexes) {
        lemmaRepository.saveLemmas(removeLastComma(valuesLemmas));
        indexRepository.saveIndexes(removeLastComma(valuesIndexes));
    }

    private String removeLastComma(StringBuilder input) {
        int i = input.lastIndexOf(",");
        return input.replace(i, i + 1, "").toString();
    }

    public Connection.Response crawlResponse(String url, int attempts) throws Exception {
        try {
            Thread.sleep(threadSleep * attempts);
            return Jsoup.connect(url).ignoreContentType(true).userAgent(userAgent).referrer(referrer).execute();
        } catch (HttpStatusException e) {
            throw e;
        } catch (Exception e) {
            if (attempts <= numberOfAttempts) {
                return crawlResponse(url, ++attempts);
            }
            throw e;
        }
    }

    public Connection.Response crawlResponse(String url) throws Exception {
        return crawlResponse(url, 1);
    }

    public Map<String, Document> hrefHtmlParser(Connection.Response response, String rootDomain, SiteEntity siteEntity) throws Exception {
        try {
            Document document = response.parse();
            Elements elements = document.select(HREF_TAG);
            Map<String, Document> urlsMap = new HashMap<>();
            for (Element element : elements) {
                String href = element.attr(HREF).trim().toLowerCase();
                Matcher matcher = pattern.matcher(href);
                boolean isURL = matcher.matches();
                boolean isPath = !isURL && href.startsWith("/");
                boolean isValidRoot = isURL && matcher.group(2).endsWith(rootDomain) && !href.endsWith(matcher.group(2));
                if (isPath || isValidRoot) {
                    href = isValidRoot ? href : siteEntity.getUrl() + href;
                    urlsMap.put(href, document);
                }
            }
            return urlsMap;
        } catch (Exception e) {
            throw e;
        }
    }

}
