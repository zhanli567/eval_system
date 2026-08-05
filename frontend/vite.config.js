import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

const backendTarget = process.env.VITE_API_PROXY_TARGET || 'http://localhost:18080';

export default defineConfig({
    plugins: [vue()],
    server: {
        port: 5173,
        proxy: {
            '/api': {
                target: backendTarget,
                rewrite: (path) => path.replace(/^\/api/, '')
            }
        }
    }
});
