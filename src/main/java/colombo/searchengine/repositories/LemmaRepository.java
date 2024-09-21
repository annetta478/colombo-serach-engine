package colombo.searchengine.repositories;

import colombo.searchengine.model.LemmaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer>, CustomRepository {

    @Query(nativeQuery = true, value = "SELECT count(*) FROM lemma WHERE site_id = ?")
    int countBySiteId(Integer siteId);

}
