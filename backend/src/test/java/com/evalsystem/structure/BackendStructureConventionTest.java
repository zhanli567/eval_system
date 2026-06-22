package com.evalsystem.structure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class BackendStructureConventionTest {
  private static final Pattern UNPREFIXED_TABLE_NAME = Pattern.compile("(?<!t_)\\beval_[a-z_]+\\b");

  @Test
  void databaseTableNamesUseTEvalPrefixEverywhere() throws IOException {
    List<Path> files = Stream.concat(
            Files.walk(projectRoot().resolve("DDL")),
            Files.walk(projectRoot().resolve("backend/src/main/resources/mapper")))
        .filter(Files::isRegularFile)
        .filter(path -> path.toString().endsWith(".sql") || path.toString().endsWith(".xml"))
        .toList();

    for (Path file : files) {
      String content = Files.readString(file, StandardCharsets.UTF_8);
      assertThat(UNPREFIXED_TABLE_NAME.matcher(content).results().map(match -> match.group()).distinct().toList())
          .as(file.toString())
          .isEmpty();
    }
  }

  @Test
  void entityTableNamesUseTEvalPrefix() throws Exception {
    for (String entityClass : List.of(
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
        "com.evalsystem.task.entity.EvalTaskTagResult")) {
      String tableName = (String) Class.forName(entityClass)
          .getAnnotation(com.baomidou.mybatisplus.annotation.TableName.class)
          .annotationType()
          .getMethod("value")
          .invoke(Class.forName(entityClass).getAnnotation(com.baomidou.mybatisplus.annotation.TableName.class));
      assertThat(tableName).startsWith("t_eval_");
    }
  }

  @Test
  void businessModulesUseUnifiedFoldersAndServiceClassOnly() throws Exception {
    Path sourceRoot = projectRoot().resolve("backend/src/main/java/com/evalsystem");
    for (String module : List.of("dataset", "tag", "evaluator", "task")) {
      Path moduleRoot = sourceRoot.resolve(module);
      assertThat(moduleRoot.resolve("api")).isDirectory();
      assertThat(moduleRoot.resolve("api/dto/request")).isDirectory();
      assertThat(moduleRoot.resolve("api/dto/response")).isDirectory();
      assertThat(moduleRoot.resolve("entity")).isDirectory();
      assertThat(moduleRoot.resolve("mapper")).isDirectory();
      assertThat(moduleRoot.resolve("repository")).isDirectory();
      assertThat(moduleRoot.resolve("service")).isDirectory();
      assertThat(moduleRoot.resolve("controller")).doesNotExist();
      assertThat(moduleRoot.resolve("dto")).doesNotExist();
      assertThat(moduleRoot.resolve("pojo")).doesNotExist();
      assertThat(moduleRoot.resolve("service/impl")).doesNotExist();
    }

    assertConcreteService("com.evalsystem.dataset.service.DatasetService");
    assertConcreteService("com.evalsystem.tag.service.TagService");
    assertConcreteService("com.evalsystem.evaluator.service.EvaluatorService");
    assertConcreteService("com.evalsystem.task.service.TaskService");
    assertConcreteService("com.evalsystem.integration.service.PlatformIntegrationService");
  }

  @Test
  void apiDtosAreSplitIntoRequestAndResponsePackages() throws Exception {
    for (String className : List.of(
        "com.evalsystem.dataset.api.DatasetController",
        "com.evalsystem.dataset.api.dto.request.CreateDatasetRequest",
        "com.evalsystem.dataset.api.dto.response.DatasetSummary",
        "com.evalsystem.tag.api.TagController",
        "com.evalsystem.tag.api.dto.request.TagInput",
        "com.evalsystem.tag.api.dto.response.TagSummary",
        "com.evalsystem.evaluator.api.EvaluatorController",
        "com.evalsystem.evaluator.api.dto.request.EvaluatorInput",
        "com.evalsystem.evaluator.api.dto.response.EvaluatorSummary",
        "com.evalsystem.task.api.TaskController",
        "com.evalsystem.task.api.dto.request.CreateTaskRequest",
        "com.evalsystem.task.api.dto.response.TaskSummary",
        "com.evalsystem.integration.api.PlatformIntegrationController",
        "com.evalsystem.integration.api.dto.request.PlatformModelChatRequest",
        "com.evalsystem.integration.api.dto.response.PlatformModelInfo")) {
      assertThat(Class.forName(className)).isNotNull();
    }
  }

  private static void assertConcreteService(String className) throws ClassNotFoundException {
    Class<?> serviceType = Class.forName(className);
    assertThat(serviceType.isInterface()).isFalse();
    assertThat(Modifier.isAbstract(serviceType.getModifiers())).isFalse();
    assertThat(serviceType.getSimpleName()).endsWith("Service");
  }

  private static Path projectRoot() {
    Path cwd = Paths.get("").toAbsolutePath();
    if ("backend".equals(cwd.getFileName().toString())) {
      return cwd.getParent();
    }
    return cwd;
  }
}
