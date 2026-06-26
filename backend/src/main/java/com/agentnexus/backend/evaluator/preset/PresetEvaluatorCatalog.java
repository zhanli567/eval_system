package com.agentnexus.backend.evaluator.preset;

import static com.agentnexus.backend.evaluator.preset.PresetEvaluatorDefinition.code;
import static com.agentnexus.backend.evaluator.preset.PresetEvaluatorDefinition.llm;
import static com.agentnexus.backend.evaluator.preset.PresetParamDefinition.param;

import java.util.List;

public final class PresetEvaluatorCatalog {
  public static final String CATEGORY_GENERAL_QUALITY = "general_quality";
  public static final String CATEGORY_AGENT = "agent";
  public static final String CATEGORY_TEXT_MATCH = "text_match";
  public static final String CATEGORY_TEXT_SIMILARITY = "text_similarity";
  public static final String CATEGORY_FORMAT_CHECK = "format_check";

  private static final List<PresetCategoryDefinition> CATEGORIES = List.of(
      new PresetCategoryDefinition(CATEGORY_GENERAL_QUALITY, "通用质量", 1),
      new PresetCategoryDefinition(CATEGORY_AGENT, "智能体", 2),
      new PresetCategoryDefinition(CATEGORY_TEXT_MATCH, "文本匹配", 3),
      new PresetCategoryDefinition(CATEGORY_TEXT_SIMILARITY, "文本相似度", 4),
      new PresetCategoryDefinition(CATEGORY_FORMAT_CHECK, "格式校验", 5)
  );

  private static final List<PresetEvaluatorDefinition> EVALUATORS = List.of(
      llm("answer_consistency", CATEGORY_GENERAL_QUALITY, "回复一致性")
          .description("检查回复与标准答案的一致性")
          .prompt("""
              你是一名专业的数据标注员，负责评估模型输出是否与提供的参考回答（reference response）一致。你的任务是根据以下标准进行评分：

              <评分标准>
              完美匹配参考回答的回答应该：
              - 与参考回答中的所有信息保持事实一致性。
              - 包含参考回答中与输入问题相关的关键点。
              - 在适当时与参考回答的风格、语气和格式相匹配。
              - 不与参考回答中的信息矛盾、歪曲或扭曲。
              - 在参考回答中正确地支撑声明，而不捏造细节。
              - 准确使用参考回答信息，不脱离上下文。
              - 在遵循参考回答和适当回答特定输入之间取得平衡。

              以下情况应扣分：
              - 与参考回答的事实矛盾。
              - 遗漏参考回答中存在的关键信息。
              - 歪曲或扭曲参考回答信息。
              - 在需要支撑时添加参考回答不支持的声明。
              - 在预期匹配时明显偏离参考回答风格/格式。
              - 脱离上下文使用参考回答信息。
              - 在需要原创综合时过度依赖参考回答。
              </评分标准>

              <评估步骤>
              - 仔细阅读参考回答以理解其关键事实、风格和内容。
              - 将输出中的每个声明与参考回答进行比较。
              - 检查输出是否适当地平衡使用参考回答信息和回答特定问题。
              - 评估输出是否与参考回答的细节水平和可信度相匹配。
              - 考虑输出是否正确地在参考回答中归因或支撑信息。
              - 评估输出是否添加了适当的综合还是仅仅转述。
              </评估步骤>

              <注意事项>
              目标是评估与参考回答的正确性，而不是一般质量。一个写得很好但与参考回答矛盾的回答应该得分低。一个简单但准确反映并正确使用参考回答的回答应该得分高。同时考虑准确性和参考回答的适当应用。
              </注意事项>

              <评分量表>
              - 5: 回答在事实、关键细节、逻辑和结论上与参考回答完全一致，允许措辞不同但语义等价。
              - 4: 回答的核心结论与参考回答一致，但存在非关键性省略、模糊表述或微小误差，不影响用户理解与使用。
              - 3: 回答包含部分正确信息，但遗漏关键点、包含可验证错误，或对参考内容有明显曲解。
              - 2: 回答的核心结论或关键事实与参考回答矛盾，仅含少量表面相关词，整体具有误导性。
              - 1: 回答与参考回答完全无关或直接矛盾。
              </评分量表>

              <查询>
              ${query}
              </查询>

              <上下文>
              ${context}
              </上下文>

              <参考回复>
              ${reference_response}
              </参考回复>

              <回复>
              ${response}
              </回复>

              请仅输出JSON对象，格式为：{"score": 1到5之间的数字, "reason": "评分理由"}。
              """)
          .params(
              param("query", "string", "", true, "用户查询"),
              param("context", "string", "", false, "上下文"),
              param("reference_response", "string", "", true, "参考回复"),
              param("response", "string", "", true, "待评估回复")
          )
          .score("1", "5", "3")
          .displayOrder(1)
          .build(),
      code("number_accuracy", CATEGORY_TEXT_MATCH, "数值准确性")
          .description("检查回复中的数值是否在定义的容差范围内准确")
          .params(
              param("reference_response", "string", "", true, "包含预期数值的参考文本"),
              param("response", "string", "", true, "待评估的回复文本"),
              param("tolerance", "number", "1e-6", false, "数值比较的容差")
          )
          .executeCode("""
              # -*- coding: utf-8 -*-
              '''
              Number Accuracy Grader

              检查回复中的数值是否在定义的容差范围内准确。

              评估逻辑：
              1. 从参考文本和回复文本中提取所有数值
              2. 按顺序比较两组数值
              3. 计算在容差范围内匹配的数值比例
              '''
              import re
              from typing import Any, Dict, List


              def extract_numbers(text: str) -> List[float]:
                  pattern = r"-?(?:\\d+\\.?\\d*|\\.\\d+)"
                  numbers = re.findall(pattern, text or "")
                  return [float(n) for n in numbers if n]


              def evaluate(params: Dict[str, Any]) -> Dict[str, Any]:
                  reference_response = params.get("reference_response", "")
                  response = params.get("response", "")
                  tolerance = float(params.get("tolerance", 1e-6))

                  response_numbers = extract_numbers(response)
                  reference_numbers = extract_numbers(reference_response)

                  if not reference_numbers:
                      return {"score": 0.0, "reason": "参考文本中没有可比较的数值"}
                  if not response_numbers:
                      return {"score": 0.0, "reason": "回复中没有找到数值"}

                  correct = 0
                  compared = min(len(response_numbers), len(reference_numbers))
                  for i in range(compared):
                      if abs(response_numbers[i] - reference_numbers[i]) <= tolerance:
                          correct += 1

                  accuracy = correct / len(reference_numbers)
                  reason_parts = [f"数值准确性: {correct}/{len(reference_numbers)} 个数值正确"]
                  if len(response_numbers) < len(reference_numbers):
                      reason_parts.append(f"（回复缺少 {len(reference_numbers) - len(response_numbers)} 个数值）")
                  elif len(response_numbers) > len(reference_numbers):
                      reason_parts.append(f"（回复多出 {len(response_numbers) - len(reference_numbers)} 个数值，未计入评分）")

                  return {"score": accuracy, "reason": "".join(reason_parts)}
              """)
          .score("0", "1", "0.5")
          .displayOrder(1)
          .build()
  );

  private PresetEvaluatorCatalog() {
  }

  public static List<PresetCategoryDefinition> categories() {
    return CATEGORIES;
  }

  public static List<PresetEvaluatorDefinition> evaluators() {
    return EVALUATORS;
  }
}
