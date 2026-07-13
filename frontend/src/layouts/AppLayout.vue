<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink, RouterView, useRoute } from 'vue-router';
import { Collection, DataAnalysis, Finished, PriceTag } from '@element-plus/icons-vue';
import { remoteCallApi } from '../api/remoteCall';
import { appModules } from '../config/appModules';
import { activeSpaces, currentSpaceId, findSelectedSpace, resolveSpaceSelection } from '../utils/spaceSelection';

const route = useRoute();
const activeModuleKey = computed(() => String(route.meta.moduleKey ?? 'datasets'));
const brandLogoUrl = '';
const moduleIcons = {
    datasets: Collection,
    tags: PriceTag,
    evaluators: Finished,
    tasks: DataAnalysis
};
const spaces = ref([]);
const spacesReady = ref(false);
const spaceLoading = ref(false);

const spaceOptions = computed(() => activeSpaces(spaces.value));
const selectedSpace = computed(() => findSelectedSpace(spaces.value, currentSpaceId.value));

onMounted(loadSpaces);

async function loadSpaces() {
    spaceLoading.value = true;
    try {
        spaces.value = await remoteCallApi.listSpaces(20, 1);
        currentSpaceId.value = resolveSpaceSelection(spaces.value, currentSpaceId.value);
    } catch (error) {
        spaces.value = [];
    } finally {
        spaceLoading.value = false;
        spacesReady.value = true;
    }
}
</script>

<template>
  <main class="app-shell">
    <header class="app-header">
      <div class="brand-space-row">
        <div class="brand-logo-slot" aria-label="品牌图片占位">
          <img v-if="brandLogoUrl" :src="brandLogoUrl" alt="评测中心" />
        </div>

        <div class="space-switcher">
          <span class="space-switcher-label">当前空间</span>
          <el-select
            v-model="currentSpaceId"
            class="space-select"
            :loading="spaceLoading"
            :disabled="spaceLoading || !spaceOptions.length"
            placeholder="暂无可用空间"
            filterable
          >
            <el-option
              v-for="space in spaceOptions"
              :key="space.id"
              :label="`${space.name || space.id} (${space.id})`"
              :value="space.id"
            />
          </el-select>
          <span class="space-meta">
            {{ selectedSpace ? selectedSpace.name : '未选择空间' }}
            <em>ID: {{ currentSpaceId || '-' }}</em>
          </span>
        </div>
      </div>
      <div class="header-right" aria-hidden="true" />
    </header>

    <div class="app-body">
      <aside class="side-nav" aria-label="应用评测模块">
        <nav class="nav-list">
          <RouterLink
            v-for="item in appModules"
            :key="item.key"
            :to="item.path"
            class="nav-item"
            :class="{ active: activeModuleKey === item.key }"
          >
            <el-icon class="nav-icon"><component :is="moduleIcons[item.key]" /></el-icon>
            <span>{{ item.title }}</span>
          </RouterLink>
        </nav>
      </aside>

      <section class="workspace">
        <RouterView v-if="spacesReady" :key="currentSpaceId" />
        <div v-else class="workspace-loading">正在加载空间信息...</div>
      </section>
    </div>
  </main>
</template>
