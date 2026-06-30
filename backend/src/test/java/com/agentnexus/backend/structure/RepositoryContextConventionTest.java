package com.agentnexus.backend.structure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class RepositoryContextConventionTest {
  @Test
  void repositoriesUseCurrentHoldersDirectly() throws IOException {
    Path backend = projectRoot().resolve("backend/src/main/java/com/agentnexus/backend");

    assertThat(backend.resolve("common/context/RepositoryContext.java")).doesNotExist();
    for (Path file : Files.walk(backend)
        .filter(Files::isRegularFile)
        .filter(path -> path.toString().endsWith(".java"))
        .toList()) {
      assertThat(Files.readString(file, StandardCharsets.UTF_8))
          .as(file.toString())
          .doesNotContain("RepositoryContext");
    }
  }

  private static Path projectRoot() {
    Path cwd = Paths.get("").toAbsolutePath();
    return "backend".equals(cwd.getFileName().toString()) ? cwd.getParent() : cwd;
  }
}
