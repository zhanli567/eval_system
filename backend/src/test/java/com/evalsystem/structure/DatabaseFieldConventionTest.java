package com.evalsystem.structure;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.annotation.TableName;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class DatabaseFieldConventionTest {
  private static final Pattern TABLE_BLOCK = Pattern.compile(
      "CREATE TABLE IF NOT EXISTS (t_eval_[a-z_]+) \\((.*?)\\);",
      Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final List<String> ENTITY_CLASSES = List.of(
      "com.evalsystem.dataset.entity.EvalDataset",
      "com.evalsystem.dataset.entity.EvalDatasetVersion",
      "com.evalsystem.dataset.entity.EvalDatasetField",
      "com.evalsystem.dataset.entity.EvalDatasetItem",
      "com.evalsystem.dataset.entity.EvalDatasetItemCell",
      "com.evalsystem.tag.entity.EvalTag",
      "com.evalsystem.tag.entity.EvalTagOption",
      "com.evalsystem.evaluator.entity.EvalEvaluator",
      "com.evalsystem.evaluator.entity.EvalEvaluatorVersion",
      "com.evalsystem.evaluator.entity.EvalEvaluatorParam",
      "com.evalsystem.task.entity.EvalTask",
      "com.evalsystem.task.entity.EvalTaskAppFieldMapping",
      "com.evalsystem.task.entity.EvalTaskEvaluator",
      "com.evalsystem.task.entity.EvalTaskEvaluatorParamMapping",
      "com.evalsystem.task.entity.EvalTaskTag",
      "com.evalsystem.task.entity.EvalTaskItem",
      "com.evalsystem.task.entity.EvalTaskEvaluatorResult",
      "com.evalsystem.task.entity.EvalTaskTagResult");

  @Test
  void ddlTablesUseUnifiedResourceColumns() throws IOException {
    Map<String, String> tableColumns = ddlTableColumns();
    assertThat(tableColumns).hasSize(18);

    for (Map.Entry<String, String> table : tableColumns.entrySet()) {
      String columns = table.getValue().toLowerCase();
      assertThat(columns).as(table.getKey()).contains("id varchar(36)");
      assertThat(columns).as(table.getKey()).contains("space_id varchar(36)");
      assertThat(columns).as(table.getKey()).contains("created_by_name varchar(100)");
      assertThat(columns).as(table.getKey()).contains("created_by varchar(36)");
      assertThat(columns).as(table.getKey()).contains("created_date timestamp default current_timestamp");
      assertThat(columns).as(table.getKey()).contains("last_updated_by varchar(36)");
      assertThat(columns).as(table.getKey()).contains("last_updated_by_name varchar(100)");
      assertThat(columns).as(table.getKey()).contains("last_updated_date timestamp");
    }
  }

  @Test
  void legacyUpdatedAtColumnAndEntityFieldAreRemoved() throws Exception {
    for (Path file : Files.walk(projectRoot().resolve("DDL"))
        .filter(Files::isRegularFile)
        .filter(path -> path.toString().endsWith(".sql"))
        .toList()) {
      assertThat(Files.readString(file, StandardCharsets.UTF_8))
          .as(file.toString())
          .doesNotContain("updated_at");
    }

    for (String className : ENTITY_CLASSES) {
      assertThat(Class.forName(className).getDeclaredFields())
          .as(className)
          .noneMatch(field -> "updatedAt".equals(field.getName()));
    }
  }

  @Test
  void entitiesExposeUnifiedResourceFields() throws Exception {
    for (String className : ENTITY_CLASSES) {
      Class<?> entityClass = Class.forName(className);
      assertThat(entityClass.getAnnotation(TableName.class)).as(className).isNotNull();
      assertField(entityClass, "spaceId", String.class);
      assertField(entityClass, "createdByName", String.class);
      assertField(entityClass, "createdBy", String.class);
      assertField(entityClass, "createdDate", java.time.LocalDateTime.class);
      assertField(entityClass, "lastUpdatedBy", String.class);
      assertField(entityClass, "lastUpdatedByName", String.class);
      assertField(entityClass, "lastUpdatedDate", java.time.LocalDateTime.class);
    }
  }

  private static void assertField(Class<?> entityClass, String fieldName, Class<?> fieldType) throws NoSuchFieldException {
    Field field = entityClass.getDeclaredField(fieldName);
    assertThat(field.getType()).as(entityClass.getName() + "." + fieldName).isEqualTo(fieldType);
  }

  private static Map<String, String> ddlTableColumns() throws IOException {
    Map<String, String> tables = new LinkedHashMap<>();
    for (Path file : Files.list(projectRoot().resolve("DDL"))
        .filter(path -> path.toString().endsWith(".sql"))
        .sorted()
        .toList()) {
      Matcher matcher = TABLE_BLOCK.matcher(Files.readString(file, StandardCharsets.UTF_8));
      while (matcher.find()) {
        tables.put(matcher.group(1), matcher.group(2));
      }
    }
    return tables;
  }

  private static Path projectRoot() {
    Path cwd = Paths.get("").toAbsolutePath();
    if ("backend".equals(cwd.getFileName().toString())) {
      return cwd.getParent();
    }
    return cwd;
  }
}
