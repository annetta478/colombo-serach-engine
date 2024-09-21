package colombo.searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import colombo.searchengine.model.SiteEntity;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    @Query("SELECT s FROM SiteEntity s WHERE s.url LIKE CONCAT('%', :rootDomain, '%')")
    SiteEntity findByRootDomain(@Param("rootDomain") String rootDomain);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "UPDATE site SET status_time = CURRENT_TIMESTAMP WHERE id = ?")
    int updateStatusTime(Integer id);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "UPDATE site SET status_time = CURRENT_TIMESTAMP, status = ?2 WHERE id = ?1")
    int updateStatusAndTime(Integer id, String status);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "UPDATE site SET status_time = CURRENT_TIMESTAMP, last_error = ?3, status = ?2 WHERE id = ?1")
    int updateStatusAndMessageAndTime(Integer id, String status, String message);

    @Query(nativeQuery = true, value = "SELECT count(*) FROM site")
    int countRecords();

}
