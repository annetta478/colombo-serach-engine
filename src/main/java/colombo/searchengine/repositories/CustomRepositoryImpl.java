package colombo.searchengine.repositories;

import colombo.searchengine.model.LemmaEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CustomRepositoryImpl implements CustomRepository {

    @Autowired
    private EntityManager entityManager;

    @Value("${search-settings.lower-filter-score}")
    private double lemmasLowerFilterScore;
    @Value("${search-settings.upper-filter-score}")
    private double lemmasUpperFilterScore;


    @Modifying
    @Transactional
    public int saveLemmas(String lemmas) {
        return entityManager.createNativeQuery(String.format("INSERT INTO lemma (lemma, site_id) VALUES %s ON DUPLICATE KEY UPDATE frequency = frequency + 1", lemmas)).executeUpdate();
    }


    @Modifying
    @Transactional
    public int saveIndexes(String indexes) {
        return entityManager.createNativeQuery(String.format("INSERT INTO indexes (lemma_id, page_id, ranking) VALUES %s", indexes)).executeUpdate();
    }


    public List<LemmaEntity> searchLemmas(Set<String> lemmas, String url) {
        String urlCondition = "";
        if (url != null) {
            urlCondition = " AND l.site.id = (SELECT s.id FROM SiteEntity s WHERE s.url = :url)";
        }
        String queryStr = String.format(
                "SELECT l FROM LemmaEntity l " +
                        "WHERE l.lemma IN :lemmas " +
                        "AND (100.0 / (SELECT COUNT(i) FROM IndexEntity i WHERE i.lemmaId = l.id)) BETWEEN :lowerScore AND :upperScore " +
                        "%s " +
                        "ORDER BY l.frequency", urlCondition);
        Query query = entityManager.createQuery(queryStr, LemmaEntity.class);
        query.setParameter("lemmas", lemmas);
        query.setParameter("lowerScore", lemmasLowerFilterScore);
        query.setParameter("upperScore", lemmasUpperFilterScore);
        if (url != null) {
            query.setParameter("url", url);
        }
        return query.getResultList();
    }


    public List<Integer> retrivePagesId(String lemma, String site, List<Integer> pagesId) {
        String baseQuery = "SELECT page_id FROM indexes WHERE lemma_id IN (SELECT id FROM lemma WHERE lemma = :lemma)";
        boolean isNotNullSite = site != null;
        boolean isNotNullPageIdList = pagesId != null;

        if (isNotNullSite) {
            baseQuery += " AND page_id IN (SELECT p.id FROM page p JOIN site s ON p.site_id = s.id WHERE s.url = :site)";
        }
        if(isNotNullPageIdList) {
            baseQuery += " AND page_id IN (:pagesId)";
        }
        Query query = entityManager.createNativeQuery(baseQuery)
                .setParameter("lemma", lemma);
        if(isNotNullPageIdList) {
            query.setParameter("pagesId", pagesId);
        }
        if (isNotNullSite) {
            query.setParameter("site", site);
        }
        return query.getResultList();
    }


    public Map<Integer, Integer> retrieveRankings(List<Integer> lemmasId, List<Integer> pageIds) {
        String sqlQuery = "SELECT i.page_id, SUM(i.ranking) as total_relevance FROM indexes i " +
                "WHERE i.page_id IN :pageIds AND i.lemma_id IN :lemmasId " +
                "GROUP BY i.page_id";

        List<Object[]> results = entityManager.createNativeQuery(sqlQuery)
                .setParameter("pageIds", pageIds)
                .setParameter("lemmasId", lemmasId)
                .getResultList();

        Map<Integer, Integer> rankingsMap = new HashMap<>();

        for (Object[] row : results) {
            Integer pageId = (Integer) row[0];
            Double totalRelevance = (Double) row[1];
            rankingsMap.put(pageId, totalRelevance != null ? totalRelevance.intValue() : 0);
        }

        for (Integer pageId : pageIds) {
            rankingsMap.putIfAbsent(pageId, 0);
        }

        return rankingsMap;
    }

}
