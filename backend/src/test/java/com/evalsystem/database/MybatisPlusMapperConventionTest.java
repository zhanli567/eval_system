package com.evalsystem.database;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evalsystem.dataset.mapper.DatasetMapper;
import com.evalsystem.dataset.entity.EvalDataset;
import com.evalsystem.evaluator.mapper.EvaluatorMapper;
import com.evalsystem.evaluator.entity.EvalEvaluator;
import com.evalsystem.evaluator.service.EvaluatorService;
import com.evalsystem.tag.mapper.TagMapper;
import com.evalsystem.tag.entity.EvalTag;
import com.evalsystem.tag.service.TagService;
import com.evalsystem.dataset.service.DatasetService;
import com.evalsystem.task.mapper.TaskMapper;
import com.evalsystem.task.entity.EvalTask;
import com.evalsystem.task.service.TaskService;
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
    assertBaseMapperEntity("com.evalsystem.dataset.mapper.DatasetVersionMapper", "com.evalsystem.dataset.entity.EvalDatasetVersion");
    assertBaseMapperEntity("com.evalsystem.dataset.mapper.DatasetFieldMapper", "com.evalsystem.dataset.entity.EvalDatasetField");
    assertBaseMapperEntity("com.evalsystem.dataset.mapper.DatasetItemMapper", "com.evalsystem.dataset.entity.EvalDatasetItem");
    assertBaseMapperEntity("com.evalsystem.dataset.mapper.DatasetItemCellMapper", "com.evalsystem.dataset.entity.EvalDatasetItemCell");
    assertBaseMapperEntity("com.evalsystem.tag.mapper.TagOptionMapper", "com.evalsystem.tag.entity.EvalTagOption");
    assertBaseMapperEntity("com.evalsystem.evaluator.mapper.EvaluatorVersionMapper", "com.evalsystem.evaluator.entity.EvalEvaluatorVersion");
    assertBaseMapperEntity("com.evalsystem.evaluator.mapper.EvaluatorParamMapper", "com.evalsystem.evaluator.entity.EvalEvaluatorParam");
    assertBaseMapperEntity("com.evalsystem.task.mapper.TaskAppFieldMappingMapper", "com.evalsystem.task.entity.EvalTaskAppFieldMapping");
    assertBaseMapperEntity("com.evalsystem.task.mapper.TaskEvaluatorMapper", "com.evalsystem.task.entity.EvalTaskEvaluator");
    assertBaseMapperEntity("com.evalsystem.task.mapper.TaskEvaluatorParamMappingMapper", "com.evalsystem.task.entity.EvalTaskEvaluatorParamMapping");
    assertBaseMapperEntity("com.evalsystem.task.mapper.TaskTagMapper", "com.evalsystem.task.entity.EvalTaskTag");
    assertBaseMapperEntity("com.evalsystem.task.mapper.TaskItemMapper", "com.evalsystem.task.entity.EvalTaskItem");
    assertBaseMapperEntity("com.evalsystem.task.mapper.TaskEvaluatorResultMapper", "com.evalsystem.task.entity.EvalTaskEvaluatorResult");
    assertBaseMapperEntity("com.evalsystem.task.mapper.TaskTagResultMapper", "com.evalsystem.task.entity.EvalTaskTagResult");
  }

  @Test
  void servicesDependOnRepositoriesInsteadOfMapperInterfaces() throws ClassNotFoundException {
    assertRepository("com.evalsystem.dataset.repository.DatasetRepository");
    assertRepository("com.evalsystem.tag.repository.TagRepository");
    assertRepository("com.evalsystem.evaluator.repository.EvaluatorRepository");
    assertRepository("com.evalsystem.task.repository.TaskRepository");

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
