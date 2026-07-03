import axios from 'axios';

// Company project: replace this fallback with `import Aurora from '@aurora/core'; export default Aurora;`.
const fallbackNetwork = axios.create({
    withCredentials: true
});

const fallbackAurora = {
    service: {
        network: fallbackNetwork
    }
};

export default globalThis.Aurora || fallbackAurora;
