package com.evalsystem.database;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evalsystem.dataset.mapper.DatasetMapper;
import com.evalsystem.dataset.pojo.EvalDataset;
import com.evalsystem.evaluator.mapper.EvaluatorMapper;
import com.evalsystem.evaluator.pojo.EvalEvaluator;
import com.evalsystem.evaluator.service.impl.EvaluatorServiceImpl;
import com.evalsystem.tag.mapper.TagMapper;
import com.evalsystem.tag.pojo.EvalTag;
import com.evalsystem.tag.service.impl.TagServiceImpl;
import com.evalsystem.dataset.service.impl.DatasetServiceImpl;
import com.evalsystem.task.mapper.TaskMapper;
import com.evalsystem.task.pojo.EvalTask;
import com.evalsystem.task.service.impl.TaskServiceImpl;
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
    assertBaseMapperEntity("com.evalsystem.dataset.mapper.DatasetVersionMapper", "com.evalsystem.dataset.pojo.EvalDatasetVersion");
    assertBaseMapperEntity("com.evalsystem.dataset.mapper.DatasetFieldMapper", "com.evalsystem.dataset.pojo.EvalDatasetField");
    assertBaseMapperEntity("com.evalsystem.dataset.mapper.DatasetItemMapper", "com.evalsystem.dataset.pojo.EvalDatasetItem");
    assertBaseMapperEntity("com.evalsystem.dataset.mapper.DatasetItemCellMapper", "com.evalsystem.dataset.pojo.EvalDatasetItemCell");
    assertBaseMapperEntity("com.evalsystem.tag.mapper.TagOptionMapper", "com.evalsystem.tag.pojo.EvalTagOption");
    assertBaseMapperEntity("com.evalsystem.evaluator.mapper.EvaluatorVersionMapper", "com.evalsystem.evaluator.pojo.EvalEvaluatorVersion");
    assertBaseMapperEntity("com.evalsystem.evaluator.mapper.EvaluatorParamMapper", "com.evalsystem.evaluator.pojo.EvalEvaluatorParam");
    assertBaseMapperEntity("com.evalsystem.task.mapper.TaskAppFieldMappingMapper", "com.evalsystem.task.pojo.EvalTaskAppFieldMapping");
    assertBaseMapperEntity("com.evalsystem.task.mapper.TaskEvaluatorMapper", "com.evalsystem.task.pojo.EvalTaskEvaluator");
    assertBaseMapperEntity("com.evalsystem.task.mapper.TaskEvaluatorParamMappingMapper", "com.evalsystem.task.pojo.EvalTaskEvaluatorParamMapping");
    assertBaseMapperEntity("com.evalsystem.task.mapper.TaskTagMapper", "com.evalsystem.task.pojo.EvalTaskTag");
    assertBaseMapperEntity("com.evalsystem.task.mapper.TaskItemMapper", "com.evalsystem.task.pojo.EvalTaskItem");
    assertBaseMapperEntity("com.evalsystem.task.mapper.TaskEvaluatorResultMapper", "com.evalsystem.task.pojo.EvalTaskEvaluatorResult");
    assertBaseMapperEntity("com.evalsystem.task.mapper.TaskTagResultMapper", "com.evalsystem.task.pojo.EvalTaskTagResult");
  }

  @Test
  void servicesDependOnRepositoriesInsteadOfMapperInterfaces() throws ClassNotFoundException {
    assertRepository("com.evalsystem.dataset.repository.DatasetRepository");
    assertRepository("com.evalsystem.tag.repository.TagRepository");
    assertRepository("com.evalsystem.evaluator.repository.EvaluatorRepository");
    assertRepository("com.evalsystem.task.repository.TaskRepository");

    assertServiceDoesNotInjectMapper(DatasetServiceImpl.class);
    assertServiceDoesNotInjectMapper(TagServiceImpl.class);
    assertServiceDoesNotInjectMapper(EvaluatorServiceImpl.class);
    assertServiceDoesNotInjectMapper(TaskServiceImpl.class);
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
