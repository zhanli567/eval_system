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
    startTask(taskId) {
        return unwrap(http.post(`/tasks/${taskId}/start`));
    },
    deleteTask(taskId) {
        return unwrap(http.delete(`/tasks/${taskId}`));
    },
    getAnnotation(taskId, taskItemId) {
        return unwrap(http.get(`/tasks/${taskId}/items/${taskItemId}/annotation`));
    },
    saveAnnotation(taskId, taskItemId, data) {
        return unwrap(http.put(`/tasks/${taskId}/items/${taskItemId}/annotation`, data));
    }
};
