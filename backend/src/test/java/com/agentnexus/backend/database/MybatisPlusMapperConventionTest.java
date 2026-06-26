package com.agentnexus.backend.database;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.agentnexus.backend.dataset.mapper.DatasetMapper;
import com.agentnexus.backend.dataset.entity.EvalDataset;
import com.agentnexus.backend.evaluator.mapper.EvaluatorMapper;
import com.agentnexus.backend.evaluator.entity.EvalEvaluator;
import com.agentnexus.backend.evaluator.service.EvaluatorService;
import com.agentnexus.backend.tag.mapper.TagMapper;
import com.agentnexus.backend.tag.entity.EvalTag;
import com.agentnexus.backend.tag.service.TagService;
import com.agentnexus.backend.dataset.service.DatasetService;
import com.agentnexus.backend.task.mapper.TaskMapper;
import com.agentnexus.backend.task.entity.EvalTask;
import com.agentnexus.backend.task.service.TaskService;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;

class MybatisPlusMapperConventionTest {
  @Test
  void mybatisPlusStarterUsesExpectedVersion() throws IOException {
    String pom = Files.readString(projectRoot().resolve("backend/pom.xml"), StandardCharsets.UTF_8);

    assertThat(pom).contains("<artifactId>mybatis-plus-spring-boot3-starter</artifactId>");
    assertThat(pom).contains("<version>3.5.10.1</version>");
  }

  @Test
  void domainMappersExposeBaseMapperCrudForTheirPrimaryTables() {
    assertBaseMapperEntity(DatasetMapper.class, EvalDataset.class);
    assertBaseMapperEntity(TagMapper.class, EvalTag.class);
    assertBaseMapperEntity(EvaluatorMapper.class, EvalEvaluator.class);
    assertBaseMapperEntity(TaskMapper.class, EvalTask.class);
  }

  @Test
  void childTableMappersExposeBaseMapperCrudForSimpleSingleTableOperations() throws ClassNotFoundException {
    assertBaseMapperEntity("com.agentnexus.backend.dataset.mapper.DatasetVersionMapper", "com.agentnexus.backend.dataset.entity.EvalDatasetVersion");
    assertBaseMapperEntity("com.agentnexus.backend.dataset.mapper.DatasetFieldMapper", "com.agentnexus.backend.dataset.entity.EvalDatasetField");
    assertBaseMapperEntity("com.agentnexus.backend.dataset.mapper.DatasetItemMapper", "com.agentnexus.backend.dataset.entity.EvalDatasetItem");
    assertBaseMapperEntity("com.agentnexus.backend.dataset.mapper.DatasetItemCellMapper", "com.agentnexus.backend.dataset.entity.EvalDatasetItemCell");
    assertBaseMapperEntity("com.agentnexus.backend.tag.mapper.TagOptionMapper", "com.agentnexus.backend.tag.entity.EvalTagOption");
    assertBaseMapperEntity("com.agentnexus.backend.evaluator.mapper.EvaluatorVersionMapper", "com.agentnexus.backend.evaluator.entity.EvalEvaluatorVersion");
    assertBaseMapperEntity("com.agentnexus.backend.evaluator.mapper.EvaluatorParamMapper", "com.agentnexus.backend.evaluator.entity.EvalEvaluatorParam");
    assertBaseMapperEntity("com.agentnexus.backend.task.mapper.TaskAppFieldMappingMapper", "com.agentnexus.backend.task.entity.EvalTaskAppFieldMapping");
    assertBaseMapperEntity("com.agentnexus.backend.task.mapper.TaskEvaluatorMapper", "com.agentnexus.backend.task.entity.EvalTaskEvaluator");
    assertBaseMapperEntity("com.agentnexus.backend.task.mapper.TaskEvaluatorParamMappingMapper", "com.agentnexus.backend.task.entity.EvalTaskEvaluatorParamMapping");
    assertBaseMapperEntity("com.agentnexus.backend.task.mapper.TaskTagMapper", "com.agentnexus.backend.task.entity.EvalTaskTag");
    assertBaseMapperEntity("com.agentnexus.backend.task.mapper.TaskItemMapper", "com.agentnexus.backend.task.entity.EvalTaskItem");
    assertBaseMapperEntity("com.agentnexus.backend.task.mapper.TaskEvaluatorResultMapper", "com.agentnexus.backend.task.entity.EvalTaskEvaluatorResult");
    assertBaseMapperEntity("com.agentnexus.backend.task.mapper.TaskTagResultMapper", "com.agentnexus.backend.task.entity.EvalTaskTagResult");
  }

  @Test
  void servicesDependOnRepositoriesInsteadOfMapperInterfaces() throws ClassNotFoundException {
    assertRepository("com.agentnexus.backend.dataset.repository.DatasetRepository");
    assertRepository("com.agentnexus.backend.tag.repository.TagRepository");
    assertRepository("com.agentnexus.backend.evaluator.repository.EvaluatorRepository");
    assertRepository("com.agentnexus.backend.task.repository.TaskRepository");

    assertServiceDoesNotInjectMapper(DatasetService.class);
    assertServiceDoesNotInjectMapper(TagService.class);
    assertServiceDoesNotInjectMapper(EvaluatorService.class);
    assertServiceDoesNotInjectMapper(TaskService.class);
  }

  private static void assertBaseMapperEntity(Class<?> mapperType, Class<?> entityType) {
    assertThat(BaseMapper.class).isAssignableFrom(mapperType);
    assertThat(mapperType.getGenericInterfaces())
        .anySatisfy(type -> assertThat(type)
            .isInstanceOfSatisfying(ParameterizedType.class, parameterized -> {
              assertThat(parameterized.getRawType()).isEqualTo(BaseMapper.class);
              assertThat(parameterized.getActualTypeArguments())
                  .containsExactly((Type) entityType);
            }));
  }

  private static void assertBaseMapperEntity(String mapperClassName, String entityClassName) throws ClassNotFoundException {
    assertBaseMapperEntity(Class.forName(mapperClassName), Class.forName(entityClassName));
  }

  private static void assertRepository(String className) throws ClassNotFoundException {
    assertThat(Class.forName(className).getAnnotation(Repository.class)).isNotNull();
  }

  private static void assertServiceDoesNotInjectMapper(Class<?> serviceType) {
    for (Constructor<?> constructor : serviceType.getDeclaredConstructors()) {
      assertThat(constructor.getParameterTypes())
          .noneMatch(parameterType -> parameterType.getPackageName().contains(".mapper"));
    }
  }

  private static Path projectRoot() {
    Path cwd = Paths.get("").toAbsolutePath();
    if ("backend".equals(cwd.getFileName().toString())) {
      return cwd.getParent();
    }
    return cwd;
  }
}
