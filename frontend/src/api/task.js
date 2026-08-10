import { http, unwrap } from './http';
export const taskApi = {
    listTasks(params) {
        return unwrap(http.get('/tasks', { params }));
    },
    createTask(data) {
        return unwrap(http.post('/tasks', data));
    },
    getTask(taskId, params) {
        return unwrap(http.get(`/tasks/${taskId}`, { params }));
    },
    getMetricOverview(taskId) {
        return unwrap(http.get(`/tasks/${taskId}/metrics/overview`));
    },
    getMetricScoreSummary(taskId) {
        return unwrap(http.get(`/tasks/${taskId}/metrics/score-summary`));
    },
    getMetricItemDistribution(taskId) {
        return unwrap(http.get(`/tasks/${taskId}/metrics/item-distribution`));
    },
    getTaskCopyConfig(taskId) {
        return unwrap(http.get(`/tasks/${taskId}/copy-config`));
    },
    startTask(taskId) {
        return unwrap(http.post(`/tasks/${taskId}/start`));
    },
    stopTask(taskId) {
        return unwrap(http.post(`/tasks/${taskId}/stop`));
    },
    addTaskTag(taskId, tagId) {
        return unwrap(http.post(`/tasks/${taskId}/tags/${tagId}`));
    },
    deleteTaskTag(taskId, taskTagId) {
        return unwrap(http.post(`/tasks/${taskId}/tags/${taskTagId}/delete`));
    },
    deleteTask(taskId) {
        return unwrap(http.post(`/tasks/${taskId}/delete`));
    },
    getAnnotation(taskId, taskItemId) {
        return unwrap(http.get(`/tasks/${taskId}/items/${taskItemId}/annotation`));
    },
    saveAnnotation(taskId, taskItemId, data) {
        return unwrap(http.post(`/tasks/${taskId}/items/${taskItemId}/annotation`, data));
    }
};
