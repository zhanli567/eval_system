<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { appModules } from '../config/appModules'

const route = useRoute()
const activeModuleKey = computed(() => String(route.meta.moduleKey ?? 'datasets'))
</script>

<template>
  <main class="app-shell">
    <aside class="side-nav" aria-label="应用评测模块">
      <div class="brand-block">
        <span>智能体平台</span>
        <strong>评测中心</strong>
      </div>
      <nav class="nav-list">
        <RouterLink
          v-for="item in appModules"
          :key="item.key"
          :to="item.path"
          class="nav-item"
          :class="{ active: activeModuleKey === item.key }"
        >
          <span>{{ item.title }}</span>
          <small>{{ item.eyebrow }}</small>
        </RouterLink>
      </nav>
    </aside>

    <section class="workspace">
      <RouterView />
    </section>
  </main>
</template>
