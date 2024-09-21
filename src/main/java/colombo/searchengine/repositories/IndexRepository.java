package colombo.searchengine.repositories;

import colombo.searchengine.model.IndexEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer>, CustomRepository {


}
