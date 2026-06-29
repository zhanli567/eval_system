import { createRouter, createWebHistory } from 'vue-router';
import AppLayout from '../layouts/AppLayout.vue';
import DatasetDetailView from '../views/DatasetDetailView.vue';
import DatasetManagementView from '../views/DatasetManagementView.vue';
import EvaluatorEditorView from '../views/EvaluatorEditorView.vue';
import EvaluatorManagementView from '../views/EvaluatorManagementView.vue';
import TagManagementView from '../views/TagManagementView.vue';
import TaskAnnotationView from '../views/TaskAnnotationView.vue';
import TaskCreateView from '../views/TaskCreateView.vue';
import TaskDetailView from '../views/TaskDetailView.vue';
import TaskManagementView from '../views/TaskManagementView.vue';
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
                    component: EvaluatorManagementView,
                    meta: { moduleKey: 'evaluators' }
                },
                {
                    path: 'evaluators/create',
                    name: 'evaluator-create',
                    component: EvaluatorEditorView,
                    meta: { moduleKey: 'evaluators' }
                },
                {
                    path: 'evaluators/:evaluatorId',
                    name: 'evaluator-edit',
                    component: EvaluatorEditorView,
                    meta: { moduleKey: 'evaluators' }
                },
                {
                    path: 'tasks',
                    name: 'tasks',
                    component: TaskManagementView,
                    meta: { moduleKey: 'tasks' }
                },
                {
                    path: 'tasks/create',
                    name: 'task-create',
                    component: TaskCreateView,
                    meta: { moduleKey: 'tasks' }
                },
                {
                    path: 'tasks/:taskId',
                    name: 'task-detail',
                    component: TaskDetailView,
                    meta: { moduleKey: 'tasks' }
                },
                {
                    path: 'tasks/:taskId/items/:taskItemId/annotation',
                    name: 'task-annotation',
                    component: TaskAnnotationView,
                    meta: { moduleKey: 'tasks' }
                }
            ]
        }
    ]
});
export default router;
