import axios from 'axios';
const http = axios.create({
    baseURL: '/api',
    timeout: 10000
});
function unwrap(request) {
    return request.then((res) => {
        if (res.data.code !== 0) {
            throw new Error(res.data.msg);
        }
        return res.data.data;
    });
}
export const datasetApi = {
    listDatasets(params) {
        return unwrap(http.get('/datasets', { params }));
    },
    createDataset(data) {
        return unwrap(http.post('/datasets', data));
    },
    deleteDataset(datasetId) {
        return unwrap(http.delete(`/datasets/${datasetId}`));
    },
    listVersions(datasetId) {
        return unwrap(http.get(`/datasets/${datasetId}/versions`));
    },
    getVersionDetail(versionId, params) {
        return unwrap(http.get(`/datasets/versions/${versionId}`, { params }));
    },
    replaceFields(versionId, fields) {
        return unwrap(http.put(`/datasets/versions/${versionId}/fields`, fields));
    },
    addRow(versionId, values) {
        return unwrap(http.post(`/datasets/versions/${versionId}/items`, { values }));
    },
    addRows(versionId, rows) {
        return unwrap(http.post(`/datasets/versions/${versionId}/items/batch`, { rows }));
    },
    importRows(versionId, file) {
        const formData = new FormData();
        formData.append('file', file);
        return unwrap(http.post(`/datasets/versions/${versionId}/items/import`, formData));
    },
    coverRowsByExcel(versionId, file) {
        const formData = new FormData();
        formData.append('file', file);
        return unwrap(http.post(`/datasets/versions/${versionId}/items/import-cover`, formData));
    },
    updateRow(versionId, itemId, values) {
        return unwrap(http.put(`/datasets/versions/${versionId}/items/${itemId}`, { values }));
    },
    deleteRow(versionId, itemId) {
        return unwrap(http.delete(`/datasets/versions/${versionId}/items/${itemId}`));
    },
    publish(datasetId) {
        return unwrap(http.post(`/datasets/${datasetId}/publish`));
    },
    deleteVersion(versionId) {
        return unwrap(http.delete(`/datasets/versions/${versionId}`));
    },
    coverDraft(datasetId, versionId) {
        return unwrap(http.post(`/datasets/${datasetId}/versions/${versionId}/cover-draft`));
    }
};
