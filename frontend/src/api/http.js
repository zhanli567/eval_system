import axios from 'axios';
import { SPACE_STORAGE_KEY } from '../utils/spaceSelection';

export const http = axios.create({
    baseURL: '/api',
    timeout: 10000,
    withCredentials: true
});

http.interceptors.request.use((config) => {
    const url = String(config.url || '');
    if (!url.startsWith('/integration/spaces')) {
        const spaceId = localStorage.getItem(SPACE_STORAGE_KEY);
        if (spaceId) {
            if (typeof config.headers?.set === 'function') {
                config.headers.set('x-space-id', spaceId);
            } else {
                config.headers = { ...(config.headers || {}), 'x-space-id': spaceId };
            }
        }
    }
    return config;
});

export function unwrap(request) {
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
