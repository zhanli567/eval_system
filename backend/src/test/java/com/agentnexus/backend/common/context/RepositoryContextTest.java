package com.agentnexus.backend.common.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentnexus.backend.common.security.CurrentUser;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RepositoryContextTest {

  @AfterEach
  void tearDown() {
    CurrentSpaceHolder.clear();
    CurrentUserHolder.clear();
  }

  @Test
  void fillsAuditFieldsFromCurrentHolders() {
    CurrentSpaceHolder.set("space-1");
    CurrentUserHolder.set(new CurrentUser("user-1", "User One", Set.of("space-1")));
    Resource resource = new Resource();

    RepositoryContext.fillCreated(resource);

    assertThat(resource.spaceId).isEqualTo("space-1");
    assertThat(resource.createdBy).isEqualTo("user-1");
    assertThat(resource.createdByName).isEqualTo("User One");
    assertThat(resource.lastUpdatedBy).isEqualTo("user-1");
    assertThat(resource.lastUpdatedByName).isEqualTo("User One");
  }

  @Test
  void fallsBackToEmptyStringsWhenHoldersAreMissing() {
    Resource resource = new Resource();

    RepositoryContext.fillCreated(resource);

    assertThat(resource.spaceId).isEmpty();
    assertThat(resource.createdBy).isEmpty();
    assertThat(resource.createdByName).isEmpty();
  }

  static class Resource {
    String spaceId;
    String createdByName;
    String createdBy;
    String lastUpdatedBy;
    String lastUpdatedByName;

    public void setSpaceId(String spaceId) {
      this.spaceId = spaceId;
    }

    public void setCreatedByName(String createdByName) {
      this.createdByName = createdByName;
    }

    public void setCreatedBy(String createdBy) {
      this.createdBy = createdBy;
    }

    public void setLastUpdatedBy(String lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
    }

    public void setLastUpdatedByName(String lastUpdatedByName) {
      this.lastUpdatedByName = lastUpdatedByName;
    }
  }
}
