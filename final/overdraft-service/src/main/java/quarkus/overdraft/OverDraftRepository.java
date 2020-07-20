package quarkus.overdraft;

import org.springframework.data.repository.CrudRepository;

public interface OverDraftRepository extends CrudRepository<OverDraft, Long> {
}
