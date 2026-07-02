import { http, unwrap } from './http';
export const tagApi = {
    listTags(params) {
        return unwrap(http.get('/tags', { params }));
    },
    getTag(tagId) {
        return unwrap(http.get(`/tags/${tagId}`));
    },
    createTag(data) {
        return unwrap(http.post('/tags', data));
    },
    updateTag(tagId, data) {
        return unwrap(http.put(`/tags/${tagId}`, data));
    }
};
