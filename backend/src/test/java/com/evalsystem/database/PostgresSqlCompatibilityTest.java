package com.evalsystem.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PostgresSqlCompatibilityTest {
  @Test
  void ddlScriptsUsePostgresCompatibleSyntax() throws IOException {
    String ddl = readAll(projectRoot().resolve("DDL"), "*.sql");

    assertThat(ddl).doesNotContain("TINYINT", "LONGTEXT", "COMMENT='");
    assertThat(ddl).doesNotContainPattern("(?i)\\bCOMMENT\\s+'");
    assertThat(ddl).contains("SMALLINT", "TEXT", "COMMENT ON TABLE", "COMMENT ON COLUMN");
  }

  @Test
  void mapperSqlAvoidsMysqlOnlyQuotingAndNumericRounding() throws IOException {
    String mapperSql = readAll(projectRoot().resolve("backend/src/main/resources/mapper"), "*.xml");

    assertThat(mapperSql).doesNotContain("`");
    assertThat(mapperSql).doesNotContain(
        "ROUND(COALESCE(SUM(CASE WHEN r.pass_result = 'pass' THEN 1 ELSE 0 END), 0) * 100");
    assertThat(mapperSql).contains("CAST(COALESCE(SUM(CASE WHEN r.pass_result = 'pass' THEN 1 ELSE 0 END), 0) AS numeric)");
  }

  @Test
  void datasourceUsesPostgresDriverAndUrl() throws IOException {
    String application = Files.readString(
        projectRoot().resolve("backend/src/main/resources/application.yml"),
        StandardCharsets.UTF_8);

    assertThat(application).contains("jdbc:postgresql://localhost:5432/eval_system");
    assertThat(application).contains("driver-class-name: org.postgresql.Driver");
    assertThat(application).doesNotContain("jdbc:mysql", "com.mysql.cj.jdbc.Driver");
  }

  private static Path projectRoot() {
    Path cwd = Paths.get("").toAbsolutePath();
    if ("backend".equals(cwd.getFileName().toString())) {
      return cwd.getParent();
    }
    return cwd;
  }

  private static String readAll(Path directory, String glob) throws IOException {
    StringBuilder content = new StringBuilder();
    try (Stream<Path> paths = Files.walk(directory)) {
      for (Path path : paths.filter(Files::isRegularFile)
          .filter(path -> directory.getFileSystem().getPathMatcher("glob:" + glob).matches(path.getFileName()))
          .sorted()
          .toList()) {
        content.append(Files.readString(path, StandardCharsets.UTF_8)).append('\n');
      }
    }
    return content.toString();
  }
}
