export interface AppModuleMeta {
  key: string
  path: string
  title: string
  eyebrow: string
  description: string
}

export const appModules: AppModuleMeta[] = [
  {
    key: 'datasets',
    path: '/datasets',
    title: '评测集管理',
    eyebrow: '应用评测',
    description: '维护评测集表头、草稿数据和发布版本。'
  },
  {
    key: 'tags',
    path: '/tags',
    title: '标签管理',
    eyebrow: '人工评测',
    description: '维护分类、布尔值、数字和文本评分标签。'
  },
  {
    key: 'evaluators',
    path: '/evaluators',
    title: '评估器管理',
    eyebrow: '评估配置',
    description: '后续用于维护LLM评估器、提示词和评分规则。'
  },
  {
    key: 'tasks',
    path: '/tasks',
    title: '评测任务',
    eyebrow: '运行评测',
    description: '后续用于创建任务、查看执行结果和评分明细。'
  }
]

export function getModuleMeta(key?: string) {
  return appModules.find((item) => item.key === key) ?? appModules[0]
}
