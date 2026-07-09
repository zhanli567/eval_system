import { http, unwrap } from './http';
export const datasetApi = {
    listDatasets(params) {
        return unwrap(http.get('/datasets', { params }));
    },
    createDataset(data) {
        return unwrap(http.post('/datasets', data));
    },
    deleteDataset(datasetId) {
        return unwrap(http.post(`/datasets/${datasetId}/delete`));
    },
    listVersions(datasetId) {
        return unwrap(http.get(`/datasets/${datasetId}/versions`));
    },
    getVersionDetail(versionId, params) {
        return unwrap(http.get(`/datasets/versions/${versionId}`, { params }));
    },
    replaceFields(versionId, fields) {
        return unwrap(http.post(`/datasets/versions/${versionId}/fields`, fields));
    },
    addRow(versionId, values) {
        return unwrap(http.post(`/datasets/versions/${versionId}/items`, { values }));
    },
    importRows(versionId, file) {
        const formData = new FormData();
        formData.append('file', file);
        return unwrap(http.post(`/datasets/versions/${versionId}/items/import`, formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        }));
    },
    coverRowsByExcel(versionId, file) {
        const formData = new FormData();
        formData.append('file', file);
        return unwrap(http.post(`/datasets/versions/${versionId}/items/import-cover`, formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        }));
    },
    updateRow(versionId, itemId, values) {
        return unwrap(http.post(`/datasets/versions/${versionId}/items/${itemId}`, { values }));
    },
    deleteRow(versionId, itemId) {
        return unwrap(http.post(`/datasets/versions/${versionId}/items/${itemId}/delete`));
    },
    publish(datasetId) {
        return unwrap(http.post(`/datasets/${datasetId}/publish`));
    },
    deleteVersion(versionId) {
        return unwrap(http.post(`/datasets/versions/${versionId}/delete`));
    },
    coverDraft(datasetId, versionId) {
        return unwrap(http.post(`/datasets/${datasetId}/versions/${versionId}/cover-draft`));
    }
};
