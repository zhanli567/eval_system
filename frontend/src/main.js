import { createApp } from 'vue';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import App from './App.vue';
import OverflowTooltip from './components/OverflowTooltip.vue';
import SortableHeader from './components/SortableHeader.vue';
import router from './router';
import { tableOverflowTooltipOptions } from './utils/tableOverflowTooltip';
import './styles.css';
const app = createApp(App);
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component);
}
app.component('OverflowTooltip', OverflowTooltip);
app.component('SortableHeader', SortableHeader);
app.use(ElementPlus, {
    table: {
        tooltipOptions: tableOverflowTooltipOptions
    }
});
app.use(router);
app.mount('#app');
