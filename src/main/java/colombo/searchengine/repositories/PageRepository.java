package colombo.searchengine.repositories;

import colombo.searchengine.model.PageEntity;
import colombo.searchengine.model.SiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    boolean existsByPathAndSite(String path, SiteEntity site);

    PageEntity findIdByPathAndSiteId(String path, Integer siteId);

    @Query(nativeQuery = true, value = "SELECT count(*) FROM page WHERE site_id = ?")
    int countBySiteId(Integer siteId);

    List<PageEntity> findByIdIn(List<Integer> ids);

}
