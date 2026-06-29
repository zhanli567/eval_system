import axios from 'axios';
const http = axios.create({
    baseURL: '/api',
    timeout: 10000
});
function unwrap(request) {
    return request
        .then((res) => {
        if (res.data.code !== 0) {
            throw new Error(res.data.msg);
        }
        return res.data.data;
    })
        .catch((error) => {
        const message = error?.response?.data?.msg || error?.message || '请求失败';
        throw new Error(message);
    });
}
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
