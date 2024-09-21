package colombo.searchengine.repositories;

import colombo.searchengine.model.LemmaEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CustomRepository {

    @Modifying
    @Transactional
    int saveLemmas(String lemmas);

    @Modifying
    @Transactional
    int saveIndexes(String indexes);

    List<LemmaEntity> searchLemmas(Set<String> lemmas, String url);

    List<Integer> retrivePagesId(String lemma, String site, List<Integer> pagesId);

    Map<Integer, Integer> retrieveRankings(List<Integer> lemmasId, List<Integer> pageIds);

}
