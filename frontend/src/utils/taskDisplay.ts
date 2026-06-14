const APP_OUTPUT_KEYS = ['text', 'content', 'answer', 'error', 'rawText', 'reasoning', 'debug']

export function formatAppOutput(value?: string) {
  const text = pickAppOutputText(value || '')
  return cleanupDisplayText(text)
}

export function formatEvaluatorReason(value?: string) {
  return cleanupDisplayText(value || '')
}

export function compactText(value?: string, maxLength = 140) {
  const text = cleanupDisplayText(value || '').replace(/\s+/g, ' ')
  return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text
}

function pickAppOutputText(value: string) {
  const trimmed = value.trim()
  if (!trimmed) return ''
  if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
    try {
      const parsed = JSON.parse(trimmed) as Record<string, unknown>
      for (const key of APP_OUTPUT_KEYS) {
        const item = parsed[key]
        if (typeof item === 'string' && item.trim()) {
          return item
        }
      }
    } catch {
      return trimmed
    }
  }
  return trimmed
}

function cleanupDisplayText(value: string) {
  return value
    .replace(/\\r\\n/g, '\n')
    .replace(/\\n/g, '\n')
    .replace(/\\r/g, '\n')
    .replace(/\*\*([^*]+)\*\*/g, '$1')
    .replace(/__([^_]+)__/g, '$1')
    .replace(/[ \t]+\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}
