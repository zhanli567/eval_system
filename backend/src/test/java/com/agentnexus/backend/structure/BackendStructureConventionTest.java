package com.agentnexus.backend.structure;

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
        "com.agentnexus.backend.dataset.entity.EvalDataset",
        "com.agentnexus.backend.dataset.entity.EvalDatasetVersion",
        "com.agentnexus.backend.dataset.entity.EvalDatasetField",
        "com.agentnexus.backend.dataset.entity.EvalDatasetItem",
        "com.agentnexus.backend.dataset.entity.EvalDatasetItemCell",
        "com.agentnexus.backend.tag.entity.EvalTag",
        "com.agentnexus.backend.tag.entity.EvalTagOption",
        "com.agentnexus.backend.evaluator.entity.EvalEvaluator",
        "com.agentnexus.backend.evaluator.entity.EvalEvaluatorVersion",
        "com.agentnexus.backend.evaluator.entity.EvalEvaluatorParam",
        "com.agentnexus.backend.task.entity.EvalTask",
        "com.agentnexus.backend.task.entity.EvalTaskAppFieldMapping",
        "com.agentnexus.backend.task.entity.EvalTaskEvaluator",
        "com.agentnexus.backend.task.entity.EvalTaskEvaluatorParamMapping",
        "com.agentnexus.backend.task.entity.EvalTaskTag",
        "com.agentnexus.backend.task.entity.EvalTaskItem",
        "com.agentnexus.backend.task.entity.EvalTaskEvaluatorResult",
        "com.agentnexus.backend.task.entity.EvalTaskTagResult")) {
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
    Path sourceRoot = projectRoot().resolve("backend/src/main/java/com/agentnexus/backend");
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

    assertConcreteService("com.agentnexus.backend.dataset.service.DatasetService");
    assertConcreteService("com.agentnexus.backend.tag.service.TagService");
    assertConcreteService("com.agentnexus.backend.evaluator.service.EvaluatorService");
    assertConcreteService("com.agentnexus.backend.task.service.TaskService");
    assertConcreteService("com.agentnexus.backend.remoteCall.service.PlatformIntegrationService");
  }

  @Test
  void apiDtosAreSplitIntoRequestAndResponsePackages() throws Exception {
    for (String className : List.of(
        "com.agentnexus.backend.dataset.api.DatasetController",
        "com.agentnexus.backend.dataset.api.dto.request.CreateDatasetRequest",
        "com.agentnexus.backend.dataset.api.dto.response.DatasetSummary",
        "com.agentnexus.backend.tag.api.TagController",
        "com.agentnexus.backend.tag.api.dto.request.TagInput",
        "com.agentnexus.backend.tag.api.dto.response.TagSummary",
        "com.agentnexus.backend.evaluator.api.EvaluatorController",
        "com.agentnexus.backend.evaluator.api.dto.request.EvaluatorInput",
        "com.agentnexus.backend.evaluator.api.dto.response.EvaluatorSummary",
        "com.agentnexus.backend.task.api.TaskController",
        "com.agentnexus.backend.task.api.dto.request.CreateTaskRequest",
        "com.agentnexus.backend.task.api.dto.response.TaskSummary",
        "com.agentnexus.backend.remoteCall.api.PlatformIntegrationController",
        "com.agentnexus.backend.remoteCall.api.dto.request.PlatformModelChatRequest",
        "com.agentnexus.backend.remoteCall.api.dto.response.PlatformModelInfo")) {
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
