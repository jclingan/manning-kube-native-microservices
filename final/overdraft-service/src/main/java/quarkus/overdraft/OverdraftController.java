package quarkus.overdraft;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/overdraft")
public class OverdraftController {
  private final OverDraftRepository overDraftRepository;

  public OverdraftController(OverDraftRepository overDraftRepository) {
    this.overDraftRepository = overDraftRepository;
  }

  @GetMapping
  public Iterable<OverDraft> allOverdrafts() {
    return overDraftRepository.findAll();
  }
}
