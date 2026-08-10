<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { BarChart, GaugeChart, PieChart } from 'echarts/charts';
import { GridComponent, LegendComponent, TitleComponent, TooltipComponent } from 'echarts/components';
import { init, use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';

use([
    BarChart,
    GaugeChart,
    PieChart,
    GridComponent,
    LegendComponent,
    TitleComponent,
    TooltipComponent,
    CanvasRenderer
]);

const props = defineProps({
    option: {
        type: Object,
        required: true
    }
});

const chartRef = ref();
let chart;
let resizeObserver;

function resizeChart() {
    chart?.resize();
}

function renderChart() {
    if (!chartRef.value) {
        return;
    }
    if (!chart) {
        chart = init(chartRef.value);
    }
    chart.setOption(props.option, true);
}

watch(() => props.option, renderChart, { deep: true });

onMounted(() => {
    renderChart();
    if (window.ResizeObserver) {
        resizeObserver = new ResizeObserver(resizeChart);
        resizeObserver.observe(chartRef.value);
    } else {
        window.addEventListener('resize', resizeChart);
    }
});

onBeforeUnmount(() => {
    if (resizeObserver) {
        resizeObserver.disconnect();
    } else {
        window.removeEventListener('resize', resizeChart);
    }
    chart?.dispose();
    chart = undefined;
});
</script>

<template>
  <div ref="chartRef" class="echart-view" />
</template>
