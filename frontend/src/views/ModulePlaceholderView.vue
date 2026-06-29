<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { getModuleMeta } from '../config/appModules';
const route = useRoute();
const moduleMeta = computed(() => getModuleMeta(String(route.meta.moduleKey ?? 'datasets')));
</script>

<template>
  <header class="topbar">
    <div>
      <p class="eyebrow">{{ moduleMeta.eyebrow }}</p>
      <h1>{{ moduleMeta.title }}</h1>
    </div>
  </header>

  <section class="module-placeholder">
    <div>
      <p class="eyebrow">{{ moduleMeta.eyebrow }}</p>
      <h2>{{ moduleMeta.title }}</h2>
      <span class="meta">{{ moduleMeta.description }}</span>
    </div>
    <el-empty description="该模块后续实现，当前先完成导航骨架" />
  </section>
</template>
