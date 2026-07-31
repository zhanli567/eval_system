/**
 * 格式化Prompt展示文本，去掉文本块保留下来的行首缩进。
 * @param {string} value Prompt原始文本
 * @return {string} 格式化后的Prompt展示文本
 */
export function formatPromptBlock(value) {
    if (!value) {
        return '';
    } else {
        return String(value)
            .replace(/\r\n?/g, '\n')
            .split('\n')
            .map((line) => line.trimStart())
            .join('\n')
            .trim();
    }
}
