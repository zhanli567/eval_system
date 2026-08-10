import { http, unwrap } from './http';
import { buildResourceFieldPatch } from '../utils/resourceFields';

export const resourceFieldApi = {
    updateFields(resourceType, resourceId, fields) {
        const type = encodeURIComponent(resourceType);
        const id = encodeURIComponent(resourceId);
        return unwrap(http.post(`/resources/${type}/${id}/fields`, buildResourceFieldPatch(fields)));
    }
};
