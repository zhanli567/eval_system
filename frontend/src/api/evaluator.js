import { http, unwrap } from './http';
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
        return unwrap(http.post(`/evaluators/${evaluatorId}/delete`));
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
        return unwrap(http.post(`/evaluators/versions/${versionId}`, data));
    }
};
