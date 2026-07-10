import Aurora from './aurora';
import { getCurrentSpaceId } from '../utils/spaceSelection';
import { getErrorMessage } from '../utils/composableHelpers';

function apiPath(path) {
    const { hostname, port } = window.location;
    const prefix = port === '5173' || hostname === 'localhost' || hostname === '127.0.0.1' ? '/api' : '';
    return `${prefix}${path}`;
}

function withSpace(config = {}) {
    const spaceId = getCurrentSpaceId();
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
    }
};

export async function unwrap(request) {
    try {
        const res = await request;
        if (res.data.code === 0) {
            return res.data.data;
        }
        throw new Error(res.data.msg);
    }
    catch (error) {
        throw new Error(getErrorMessage(error, 'request failed'));
    }
}
