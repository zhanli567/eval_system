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
