import Aurora from './aurora';
import { SPACE_STORAGE_KEY } from '../utils/spaceSelection';

function apiPath(path) {
    const { hostname, port } = window.location;
    const prefix = port === '5173' || hostname === 'localhost' || hostname === '127.0.0.1' ? '/api' : '';
    return `${prefix}${path}`;
}

function withSpace(config = {}) {
    const spaceId = localStorage.getItem(SPACE_STORAGE_KEY);
    return {
        ...config,
        headers: {
            ...(config.headers || {}),
            ...(spaceId ? { 'x-space-id': spaceId } : {})
        }
    };
}

export const http = {
    get(path, config) {
        return Aurora.service.network.get(apiPath(path), withSpace(config));
    },
    post(path, data, config) {
        return Aurora.service.network.post(apiPath(path), data, withSpace(config));
    },
    put(path, data, config) {
        return Aurora.service.network.put(apiPath(path), data, withSpace(config));
    },
    patch(path, data, config) {
        return Aurora.service.network.patch(apiPath(path), data, withSpace(config));
    },
    delete(path, config) {
        return Aurora.service.network.delete(apiPath(path), withSpace(config));
    }
};

export function unwrap(request) {
    return request
        .then((res) => {
            if (res.data.code !== 0) {
                throw new Error(res.data.msg);
            }
            return res.data.data;
        })
        .catch((error) => {
            const message = error?.response?.data?.msg || error?.message || 'request failed';
            throw new Error(message);
        });
}
