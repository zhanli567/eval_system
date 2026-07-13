package com.agentnexus.backend.evaluator.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.agentnexus.backend.common.context.CurrentSpaceHolder;
import com.agentnexus.backend.common.context.CurrentUserHolder;
import com.agentnexus.backend.common.security.CurrentUser;
import com.agentnexus.backend.evaluator.api.dto.response.EvaluatorConfigBase;
import com.agentnexus.backend.evaluator.api.dto.response.EvaluatorParamDto;
import com.agentnexus.backend.evaluator.api.dto.response.EvaluatorSummary;
import com.agentnexus.backend.evaluator.api.dto.response.EvaluatorVersionDto;
import com.agentnexus.backend.evaluator.mapper.EvaluatorMapper;
import com.agentnexus.backend.evaluator.mapper.EvaluatorParamMapper;
import com.agentnexus.backend.evaluator.mapper.EvaluatorVersionMapper;
import com.agentnexus.backend.evaluator.entity.EvalEvaluator;
import com.agentnexus.backend.evaluator.entity.EvalEvaluatorParam;
import com.agentnexus.backend.evaluator.entity.EvalEvaluatorVersion;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class EvaluatorRepository {
  private final EvaluatorMapper evaluatorMapper;
  private final EvaluatorVersionMapper versionMapper;
  private final EvaluatorParamMapper paramMapper;

  public EvaluatorRepository(
      EvaluatorMapper evaluatorMapper,
      EvaluatorVersionMapper versionMapper,
      EvaluatorParamMapper paramMapper
  ) {
    this.evaluatorMapper = evaluatorMapper;
    this.versionMapper = versionMapper;
    this.paramMapper = paramMapper;
  }

  public List<EvaluatorSummary> listEvaluators(String evaluatorType, String like, String orderColumn, String orderDirection, int size, int offset) {
    return CurrentSpaceHolder.callWithSpace(currentSpaceId(), () ->
        evaluatorMapper.listEvaluators(currentSpaceId(), evaluatorType, like, orderColumn, orderDirection, size, offset));
  }

  public long countEvaluators(String evaluatorType, String like) {
    return evaluatorMapper.selectCount(new LambdaQueryWrapper<EvalEvaluator>()
        .eq(EvalEvaluator::getSpaceId, currentSpaceId())
        .eq(StringUtils.hasText(evaluatorType), EvalEvaluator::getEvaluatorType, evaluatorType)
        .like(hasLikeText(like), EvalEvaluator::getEvaluatorName, likeText(like)));
  }

  public boolean existsEvaluatorName(String evaluatorName) {
    return evaluatorMapper.selectCount(new LambdaQueryWrapper<EvalEvaluator>()
        .eq(EvalEvaluator::getSpaceId, currentSpaceId())
        .eq(EvalEvaluator::getEvaluatorName, evaluatorName)) > 0;
  }

  public void insertEvaluator(
      String evaluatorId,
      String evaluatorName,
      String evaluatorType,
      String description,
      String latestVersionId,
      String now
  ) {
    EvalEvaluator evaluator = new EvalEvaluator();
    evaluator.setId(evaluatorId);
    evaluator.setEvaluatorName(evaluatorName);
    evaluator.setEvaluatorType(evaluatorType);
    evaluator.setDescription(description);
    evaluator.setLatestVersionId(latestVersionId);
    evaluator.setLastUpdatedDate(toLastUpdatedDate(now));
    fillCreated(evaluator);
    evaluatorMapper.insert(evaluator);
  }

  public void updateEvaluatorBase(String evaluatorId, String evaluatorName, String description, String now) {
    evaluatorMapper.update(null, new LambdaUpdateWrapper<EvalEvaluator>()
        .eq(EvalEvaluator::getSpaceId, currentSpaceId())
        .eq(EvalEvaluator::getId, evaluatorId)
        .set(EvalEvaluator::getEvaluatorName, evaluatorName)
        .set(EvalEvaluator::getDescription, description)
        .set(EvalEvaluator::getLastUpdatedBy, currentUserId())
        .set(EvalEvaluator::getLastUpdatedByName, currentUserName())
        .set(EvalEvaluator::getLastUpdatedDate, toLastUpdatedDate(now)));
  }

  public void updateLatestVersion(String evaluatorId, String versionId, String now) {
    evaluatorMapper.update(null, new LambdaUpdateWrapper<EvalEvaluator>()
        .eq(EvalEvaluator::getSpaceId, currentSpaceId())
        .eq(EvalEvaluator::getId, evaluatorId)
        .set(EvalEvaluator::getLatestVersionId, versionId)
        .set(EvalEvaluator::getLastUpdatedBy, currentUserId())
        .set(EvalEvaluator::getLastUpdatedByName, currentUserName())
        .set(EvalEvaluator::getLastUpdatedDate, toLastUpdatedDate(now)));
  }

  public void deleteEvaluator(String evaluatorId) {
    List<String> versionIds = versionMapper.selectList(new LambdaQueryWrapper<EvalEvaluatorVersion>()
            .select(EvalEvaluatorVersion::getId)
            .eq(EvalEvaluatorVersion::getSpaceId, currentSpaceId())
            .eq(EvalEvaluatorVersion::getEvaluatorId, evaluatorId))
        .stream()
        .map(EvalEvaluatorVersion::getId)
        .toList();
    versionIds.forEach(versionId -> deleteParams("version", versionId));
    versionMapper.delete(new LambdaQueryWrapper<EvalEvaluatorVersion>()
        .eq(EvalEvaluatorVersion::getSpaceId, currentSpaceId())
        .eq(EvalEvaluatorVersion::getEvaluatorId, evaluatorId));
    evaluatorMapper.delete(new LambdaQueryWrapper<EvalEvaluator>()
        .eq(EvalEvaluator::getSpaceId, currentSpaceId())
        .eq(EvalEvaluator::getId, evaluatorId));
  }

  public void insertVersion(
      String versionId,
      String evaluatorId,
      int versionNo,
      String modelId,
      String modelName,
      String prompt,
      String executeCode,
      BigDecimal scoreMin,
      BigDecimal scoreMax,
      BigDecimal passThreshold,
      String now
  ) {
    EvalEvaluatorVersion version = new EvalEvaluatorVersion();
    version.setId(versionId);
    version.setEvaluatorId(evaluatorId);
    version.setVersionNo(versionNo);
    version.setModelId(modelId);
    version.setModelName(modelName);
    version.setPrompt(prompt);
    version.setExecuteCode(executeCode);
    version.setScoreMin(scoreMin);
    version.setScoreMax(scoreMax);
    version.setPassThreshold(passThreshold);
    version.setLastUpdatedDate(toLastUpdatedDate(now));
    fillCreated(version);
    versionMapper.insert(version);
  }

  public void updateDraftVersion(
      String versionId,
      String modelId,
      String modelName,
      String prompt,
      String executeCode,
      BigDecimal scoreMin,
      BigDecimal scoreMax,
      BigDecimal passThreshold,
      String now
  ) {
    versionMapper.update(null, new LambdaUpdateWrapper<EvalEvaluatorVersion>()
        .eq(EvalEvaluatorVersion::getSpaceId, currentSpaceId())
        .eq(EvalEvaluatorVersion::getId, versionId)
        .eq(EvalEvaluatorVersion::getVersionNo, 0)
        .set(EvalEvaluatorVersion::getModelId, modelId)
        .set(EvalEvaluatorVersion::getModelName, modelName)
        .set(EvalEvaluatorVersion::getPrompt, prompt)
        .set(EvalEvaluatorVersion::getExecuteCode, executeCode)
        .set(EvalEvaluatorVersion::getScoreMin, scoreMin)
        .set(EvalEvaluatorVersion::getScoreMax, scoreMax)
        .set(EvalEvaluatorVersion::getPassThreshold, passThreshold)
        .set(EvalEvaluatorVersion::getLastUpdatedBy, currentUserId())
        .set(EvalEvaluatorVersion::getLastUpdatedByName, currentUserName())
        .set(EvalEvaluatorVersion::getLastUpdatedDate, toLastUpdatedDate(now)));
  }

  public String findDraftVersionId(String evaluatorId) {
    EvalEvaluatorVersion version = versionMapper.selectOne(new LambdaQueryWrapper<EvalEvaluatorVersion>()
        .select(EvalEvaluatorVersion::getId)
        .eq(EvalEvaluatorVersion::getSpaceId, currentSpaceId())
        .eq(EvalEvaluatorVersion::getEvaluatorId, evaluatorId)
        .eq(EvalEvaluatorVersion::getVersionNo, 0)
        .last("LIMIT 1"));
    return version == null ? null : version.getId();
  }

  public int nextVersionNo(String evaluatorId) {
    EvalEvaluatorVersion version = versionMapper.selectOne(new QueryWrapper<EvalEvaluatorVersion>()
        .select("COALESCE(MAX(version_no), 0) + 1 AS version_no")
        .eq("space_id", currentSpaceId())
        .eq("evaluator_id", evaluatorId));
    return version == null || version.getVersionNo() == null ? 1 : version.getVersionNo();
  }

  public String findEvaluatorType(String evaluatorId) {
    EvalEvaluator evaluator = evaluatorMapper.selectOne(new LambdaQueryWrapper<EvalEvaluator>()
        .select(EvalEvaluator::getEvaluatorType)
        .eq(EvalEvaluator::getSpaceId, currentSpaceId())
        .eq(EvalEvaluator::getId, evaluatorId)
        .last("LIMIT 1"));
    return evaluator == null ? null : evaluator.getEvaluatorType();
  }

  public List<EvaluatorVersionDto> listVersions(String evaluatorId) {
    return versionMapper.selectList(new LambdaQueryWrapper<EvalEvaluatorVersion>()
            .eq(EvalEvaluatorVersion::getSpaceId, currentSpaceId())
            .eq(EvalEvaluatorVersion::getEvaluatorId, evaluatorId)
            .orderByAsc(EvalEvaluatorVersion::getVersionNo))
        .stream()
        .map(this::toVersionDto)
        .toList();
  }

  public long countVersionTaskBindings(String versionId) {
    return versionMapper.countTaskBindings(currentSpaceId(), versionId);
  }

  public void deleteVersion(String versionId) {
    deleteParams("version", versionId);
    versionMapper.delete(new LambdaQueryWrapper<EvalEvaluatorVersion>()
        .eq(EvalEvaluatorVersion::getSpaceId, currentSpaceId())
        .eq(EvalEvaluatorVersion::getId, versionId));
  }

  public EvaluatorConfigBase findVersionConfig(String versionId) {
    return CurrentSpaceHolder.callWithSpace(currentSpaceId(), () ->
        evaluatorMapper.findVersionConfig(currentSpaceId(), versionId));
  }

  public void insertParam(
      String paramId,
      String targetType,
      String targetId,
      String paramName,
      String dataType,
      String defaultValue,
      Boolean required,
      String description,
      int displayOrder,
      String now
  ) {
    EvalEvaluatorParam param = new EvalEvaluatorParam();
    param.setId(paramId);
    param.setTargetType(targetType);
    param.setTargetId(targetId);
    param.setParamName(paramName);
    param.setDataType(dataType);
    param.setDefaultValue(defaultValue);
    param.setIsRequired(Boolean.TRUE.equals(required) ? 1 : 0);
    param.setDescription(description);
    param.setDisplayOrder(displayOrder);
    param.setLastUpdatedDate(toLastUpdatedDate(now));
    fillCreated(param);
    paramMapper.insert(param);
  }

  public void deleteParams(String targetType, String targetId) {
    paramMapper.delete(new LambdaQueryWrapper<EvalEvaluatorParam>()
        .eq(EvalEvaluatorParam::getSpaceId, currentSpaceId())
        .eq(EvalEvaluatorParam::getTargetType, targetType)
        .eq(EvalEvaluatorParam::getTargetId, targetId));
  }

  public List<EvaluatorParamDto> listParams(String targetType, String targetId) {
    return paramMapper.selectList(new LambdaQueryWrapper<EvalEvaluatorParam>()
            .eq(EvalEvaluatorParam::getSpaceId, currentSpaceId())
            .eq(EvalEvaluatorParam::getTargetType, targetType)
            .eq(EvalEvaluatorParam::getTargetId, targetId)
            .orderByAsc(EvalEvaluatorParam::getDisplayOrder))
        .stream()
        .map(this::toParamDto)
        .toList();
  }

  private EvaluatorVersionDto toVersionDto(EvalEvaluatorVersion version) {
    int versionNo = version.getVersionNo() == null ? 0 : version.getVersionNo();
    return new EvaluatorVersionDto(
        version.getId(),
        version.getEvaluatorId(),
        versionNo,
        versionNo == 0 ? "\u8349\u7a3f" : "V" + versionNo,
        versionNo == 0,
        version.getCreatedByName(),
        version.getCreatedDate(),
        version.getLastUpdatedDate());
  }

  private EvaluatorParamDto toParamDto(EvalEvaluatorParam param) {
    return new EvaluatorParamDto(
        param.getId(),
        param.getTargetType(),
        param.getTargetId(),
        param.getParamName(),
        param.getDataType(),
        param.getDefaultValue(),
        param.getIsRequired() != null && param.getIsRequired() != 0,
        param.getDescription(),
        param.getDisplayOrder());
  }

  private void fillCreated(EvalEvaluator evaluator) {
    evaluator.setSpaceId(currentSpaceId());
    evaluator.setCreatedBy(currentUserId());
    evaluator.setCreatedByName(currentUserName());
    evaluator.setLastUpdatedBy(currentUserId());
    evaluator.setLastUpdatedByName(currentUserName());
  }

  private void fillCreated(EvalEvaluatorVersion version) {
    version.setSpaceId(currentSpaceId());
    version.setCreatedBy(currentUserId());
    version.setCreatedByName(currentUserName());
    version.setLastUpdatedBy(currentUserId());
    version.setLastUpdatedByName(currentUserName());
  }

  private void fillCreated(EvalEvaluatorParam param) {
    param.setSpaceId(currentSpaceId());
    param.setCreatedBy(currentUserId());
    param.setCreatedByName(currentUserName());
    param.setLastUpdatedBy(currentUserId());
    param.setLastUpdatedByName(currentUserName());
  }

  private String currentSpaceId() {
    return Objects.toString(CurrentSpaceHolder.get(), "");
  }

  private String currentUserId() {
    CurrentUser user = CurrentUserHolder.get();
    return user == null ? "" : Objects.toString(user.userId(), "");
  }

  private String currentUserName() {
    CurrentUser user = CurrentUserHolder.get();
    return user == null ? "" : Objects.toString(user.displayName(), "");
  }

  private LocalDateTime toLastUpdatedDate(String now) {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(now)), ZoneId.systemDefault());
  }

  private boolean hasLikeText(String like) {
    return StringUtils.hasText(like) && !"%%".equals(like);
  }

  private String likeText(String like) {
    return hasLikeText(like) && like.length() > 1 ? like.substring(1, like.length() - 1) : "";
  }
}
