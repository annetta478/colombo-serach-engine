package colombo.searchengine.services;

import colombo.searchengine.config.Site;
import colombo.searchengine.config.SitesList;
import colombo.searchengine.dto.search.SearchSiteData;
import colombo.searchengine.exceptions.CustomException;
import colombo.searchengine.exceptions.Messages;
import colombo.searchengine.model.LemmaEntity;
import colombo.searchengine.model.PageEntity;
import colombo.searchengine.repositories.IndexRepository;
import colombo.searchengine.repositories.LemmaRepository;
import colombo.searchengine.repositories.PageRepository;
import colombo.searchengine.services.core.LemmaFinder;
import colombo.searchengine.utils.Constants;
import colombo.searchengine.utils.Utils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import colombo.searchengine.dto.search.SearchResponse;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchServiceImpl.class);


    @Value("${search-settings.max-snippet-length}")
    private int maxSnippetLength;
    @Value("${search-settings.max-lemma-distance}")
    private int maxLemmaDistance;
    @Value("${search-settings.max-lemma-distance}")
    private int contextLength;

    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;

    private final SitesList sites;

    private LemmaFinder lemmaFinder;

    @Autowired
    public SearchServiceImpl(PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository, SitesList sites) {
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.sites = sites;
        try {
            lemmaFinder = LemmaFinder.getInstance();
        } catch (IOException e) {
            LOGGER.error("Error in : ", e);
            throw new RuntimeException(e);
        }
    }


    public SearchResponse search(String site, String query, int offset, int limit) {
        if (query == null || query.isBlank()) {
            throw new CustomException(Messages.INVALID_QUERY, HttpStatus.BAD_REQUEST);
        }
        if (site != null) {
            site = checkUrl(site.trim());
        }
        List<Integer> pagesId = null;
        List<LemmaEntity> lemmas;
        try {
            Set<String> lemmasSet = lemmaFinder.getLemmaSet(query.trim());
            lemmas = lemmaRepository.searchLemmas(lemmasSet, site);

            if (lemmas.isEmpty()) {
                LOGGER.info("Lemmas not found: {}}", query);
                throw new CustomException(Messages.LEMMA_NOT_FOUND_IN_QUERY, HttpStatus.BAD_REQUEST);
            }

            Set<String> uniqueLemma = new HashSet<>();
            pagesId = indexRepository.retrivePagesId(lemmas.get(0).getLemma(), site, pagesId);
            if (lemmas.size() > 1) {
                uniqueLemma.add(lemmas.get(0).getLemma());
                pagesId = filterPagesId(lemmas, pagesId, 1, uniqueLemma, site);
            }
        } catch (Exception e) {
            LOGGER.error("Error : ", e);
            throw new CustomException(Messages.GENERIC_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return createResponse(lemmas, pagesId, offset, limit);
    }

    private SearchResponse createResponse(List<LemmaEntity> lemmas, List<Integer> pagesId, int offset, int limit) {
        SearchResponse response = new SearchResponse();
        if (pagesId.isEmpty()) {
            return response;
        }
        createAndSetSiteData(lemmas, pagesId, response);
        response.setCount(response.getData().size());
        response.orderDataByHighRelevance();
        response.setOffsetAndLimit(offset, limit);
        return response;
    }

    private void createAndSetSiteData(List<LemmaEntity> lemmas, List<Integer> pagesId, SearchResponse response) {
        Map<Integer, Integer> relevances = indexRepository.retrieveRankings(lemmas.stream().map(LemmaEntity::getId).toList(), pagesId);
        List<PageEntity> pages = pageRepository.findByIdIn(pagesId);
        Set<String> keywords = lemmas.stream().map(LemmaEntity::getLemma).collect(Collectors.toSet());
        int maxAbsRelevance = 0;
        try {
            for (PageEntity page : pages) {
                int absRelevance = relevances.get(page.getId());
                boolean isCreated = createSiteData(page, keywords, absRelevance, response.getData());
                if (isCreated && absRelevance > maxAbsRelevance) {
                    maxAbsRelevance = absRelevance;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error: ", e);
            throw new RuntimeException();
        }
        for (SearchSiteData data : response.getData()) {
            data.setRelevance((double) Math.round(((double) data.getAbsRelevance() / maxAbsRelevance) * 10_000) / 10_000);
        }
    }


    private List<Integer> filterPagesId(List<LemmaEntity> lemmas, List<Integer> pagesId, int index, Set<String> uniqueLemma, String site) {
        if (index == lemmas.size() || pagesId.isEmpty()) {
            return pagesId;
        }
        String lemma = lemmas.get(index).getLemma();
        if (!uniqueLemma.contains(lemma)) {
            pagesId = indexRepository.retrivePagesId(lemma, site, pagesId);
            uniqueLemma.add(lemma);
        }
        return filterPagesId(lemmas, pagesId, index + 1, uniqueLemma, site);
    }


    private String checkUrl(String inputUrl) {
        String rootDomain = Utils.checkUrlAndRetriveRootDomain(inputUrl);
        if (rootDomain == null) {
            throw new CustomException(Messages.INVALID_URL, HttpStatus.BAD_REQUEST);
        }
        String url = retrieveUrlIfIsNotExternal(rootDomain);
        if (url == null) {
            throw new CustomException(Messages.EXTERNAL_URL, HttpStatus.BAD_REQUEST);
        }
        return url;
    }


    private String retrieveUrlIfIsNotExternal(String rootDomain) {
        for (Site site : sites.getSites()) {
            if (site.getUrl().contains(rootDomain)) {
                return Utils.removeLastSlash(site.getUrl());
            }
        }
        return null;
    }


    private boolean createSiteData(PageEntity page, Set<String> keywords, int absRelevance, List<SearchSiteData> dataList) throws Exception {
        SearchSiteData data = new SearchSiteData();
        data.setSite(page.getSite().getUrl());
        data.setSiteName(page.getSite().getName());
        data.setUri(page.getPath());
        data.setAbsRelevance(absRelevance);

        Document document = Jsoup.parse(page.getContent());
        data.setTitle(document.title());
        String body = LemmaFinder.getInstance().removeHTMLTags(document.body().toString());

        Set<Integer> positionSet = retrivePositions(keywords, body);
        List<String> snippets = retriveSnippets(positionSet, body);
        if (snippets.isEmpty())
            return false;
        setSnippet(data, snippets, keywords);
        dataList.add(data);
        return true;
    }


    private Set<Integer> retrivePositions(Set<String> keywords, String body) {
        Set<Integer> positionSet = new TreeSet<>();
        for (String keyword : keywords) {
            int pos = body.indexOf(keyword);
            while (pos != -1) {
                positionSet.add(pos);
                pos = body.indexOf(keyword, pos + 1);
            }
        }
        return positionSet;
    }


    private List<String> retriveSnippets(Set<Integer> positionSet, String body) {
        List<String> snippets = new ArrayList<>();
        if (!positionSet.isEmpty()) {
            List<Integer> positions = new ArrayList<>(positionSet);
            int lastPosition = 0;
            for (int i = 0; i < positions.size(); i++) {
                int start = positions.get(i);
                if (start < lastPosition) {
                    continue;
                }
                int end = start;

                for (int j = i + 1; j < positions.size(); j++) {
                    if (positions.get(j) - end <= maxLemmaDistance) {
                        end = positions.get(j);
                    } else {
                        break;
                    }
                }

                int snippetStart = Math.max(0, start - contextLength);
                while (snippetStart > 0 && !Utils.isPunctuationChar(body.charAt(snippetStart))) {
                    snippetStart--;
                }
                int snippetEnd = Math.min(body.length(), end + contextLength);
                while (snippetEnd < body.length() && !Utils.isPunctuationChar(body.charAt(snippetEnd))) {
                    snippetEnd++;
                }

                lastPosition = snippetEnd;
                snippets.add(body.substring(snippetStart, snippetEnd).strip());
            }
        }
        return snippets;
    }


    private void setSnippet(SearchSiteData data, List<String> snippets, Set<String> keywords) {
        StringBuilder snippet = new StringBuilder();
        for (String s : snippets) {
            Set<String> words = Arrays.stream(s.split(Constants.PUNCTUATION_REGEX)).collect(Collectors.toSet());
            for (String word : words) {
                for (String keyword : keywords) {
                    if (word.contains(keyword)) {
                        s = s.replaceAll(word, ("<b>" + word + "</b>"));
                    }
                }
            }
            if (snippet.length() < maxSnippetLength) {
                snippet.append(s).append("... ");
            } else {
                break;
            }
        }
        data.setSnippet(snippet.toString());
    }

}
