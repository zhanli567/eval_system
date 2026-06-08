import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '../layouts/AppLayout.vue'
import DatasetDetailView from '../views/DatasetDetailView.vue'
import DatasetManagementView from '../views/DatasetManagementView.vue'
import ModulePlaceholderView from '../views/ModulePlaceholderView.vue'
import TagManagementView from '../views/TagManagementView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/datasets'
    },
    {
      path: '/',
      component: AppLayout,
      children: [
        {
          path: 'datasets',
          name: 'datasets',
          component: DatasetManagementView,
          meta: { moduleKey: 'datasets' }
        },
        {
          path: 'datasets/:datasetId',
          name: 'dataset-detail',
          component: DatasetDetailView,
          meta: { moduleKey: 'datasets' }
        },
        {
          path: 'tags',
          name: 'tags',
          component: TagManagementView,
          meta: { moduleKey: 'tags' }
        },
        {
          path: 'evaluators',
          name: 'evaluators',
          component: ModulePlaceholderView,
          meta: { moduleKey: 'evaluators' }
        },
        {
          path: 'tasks',
          name: 'tasks',
          component: ModulePlaceholderView,
          meta: { moduleKey: 'tasks' }
        }
      ]
    }
  ]
})

export default router
