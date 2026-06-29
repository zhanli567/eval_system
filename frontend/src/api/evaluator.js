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
export const evaluatorApi = {
    listEvaluators(params) {
        return unwrap(http.get('/evaluators', { params }));
    },
    listPresetCategories() {
        return unwrap(http.get('/evaluators/presets/categories'));
    },
    listPresetEvaluators(params) {
        return unwrap(http.get('/evaluators/presets', { params }));
    },
    getPresetEvaluator(presetId) {
        return unwrap(http.get(`/evaluators/presets/${presetId}`));
    },
    createEvaluator(data) {
        return unwrap(http.post('/evaluators', data));
    },
    deleteEvaluator(evaluatorId) {
        return unwrap(http.delete(`/evaluators/${evaluatorId}`));
    },
    listVersions(evaluatorId) {
        return unwrap(http.get(`/evaluators/${evaluatorId}/versions`));
    },
    publish(evaluatorId) {
        return unwrap(http.post(`/evaluators/${evaluatorId}/publish`));
    },
    getVersion(versionId) {
        return unwrap(http.get(`/evaluators/versions/${versionId}`));
    },
    updateDraft(versionId, data) {
        return unwrap(http.put(`/evaluators/versions/${versionId}`, data));
    }
};
