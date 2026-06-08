package com.evalsystem.tag.mapper;

import com.evalsystem.tag.dto.TagConfig;
import com.evalsystem.tag.dto.TagOptionDto;
import com.evalsystem.tag.dto.TagSummary;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface TagMapper {
  List<TagSummary> listTags(
      @Param("tagType") String tagType,
      @Param("like") String like,
      @Param("size") int size,
      @Param("offset") int offset
  );

  long countTags(@Param("tagType") String tagType, @Param("like") String like);

  TagConfig findTagConfig(@Param("tagId") String tagId);

  List<TagOptionDto> listOptions(@Param("tagId") String tagId);

  @Select("SELECT tag_type FROM eval_tag WHERE id = #{tagId}")
  String findTagType(@Param("tagId") String tagId);

  @Select("SELECT COUNT(*) FROM eval_tag WHERE tag_name = #{tagName} AND id <> #{tagId}")
  int countSameNameExcept(@Param("tagName") String tagName, @Param("tagId") String tagId);

  @Select("SELECT COUNT(*) FROM eval_tag WHERE tag_name = #{tagName}")
  int countSameName(@Param("tagName") String tagName);

  @Insert("""
      INSERT INTO eval_tag
      (id, tag_name, tag_type, description, min_value, max_value, pass_threshold, created_at, updated_at)
      VALUES (#{tagId}, #{tagName}, #{tagType}, #{description}, #{minValue}, #{maxValue}, #{passThreshold}, #{now}, #{now})
      """)
  void insertTag(
      @Param("tagId") String tagId,
      @Param("tagName") String tagName,
      @Param("tagType") String tagType,
      @Param("description") String description,
      @Param("minValue") Integer minValue,
      @Param("maxValue") Integer maxValue,
      @Param("passThreshold") Integer passThreshold,
      @Param("now") String now
  );

  @Update("""
      UPDATE eval_tag
      SET tag_name = #{tagName},
          description = #{description},
          min_value = #{minValue},
          max_value = #{maxValue},
          pass_threshold = #{passThreshold},
          updated_at = #{now}
      WHERE id = #{tagId}
      """)
  void updateTag(
      @Param("tagId") String tagId,
      @Param("tagName") String tagName,
      @Param("description") String description,
      @Param("minValue") Integer minValue,
      @Param("maxValue") Integer maxValue,
      @Param("passThreshold") Integer passThreshold,
      @Param("now") String now
  );

  @Delete("DELETE FROM eval_tag_option WHERE tag_id = #{tagId}")
  void deleteOptions(@Param("tagId") String tagId);

  @Insert("""
      INSERT INTO eval_tag_option
      (id, tag_id, option_name, option_group, display_order, created_at, updated_at)
      VALUES (#{optionId}, #{tagId}, #{optionName}, #{optionGroup}, #{displayOrder}, #{now}, #{now})
      """)
  void insertOption(
      @Param("optionId") String optionId,
      @Param("tagId") String tagId,
      @Param("optionName") String optionName,
      @Param("optionGroup") String optionGroup,
      @Param("displayOrder") int displayOrder,
      @Param("now") String now
  );
}
