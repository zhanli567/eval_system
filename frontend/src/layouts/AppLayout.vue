<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink, RouterView, useRoute } from 'vue-router';
import { remoteCallApi } from '../api/remoteCall';
import { appModules } from '../config/appModules';
import { activeSpaces, findSelectedSpace, resolveSpaceSelection, SPACE_STORAGE_KEY } from '../utils/spaceSelection';

const route = useRoute();
const activeModuleKey = computed(() => String(route.meta.moduleKey ?? 'datasets'));
const spaces = ref([]);
const spacesReady = ref(false);
const spaceLoading = ref(false);
const currentSpaceId = ref(localStorage.getItem(SPACE_STORAGE_KEY) || '');

const spaceOptions = computed(() => activeSpaces(spaces.value));
const selectedSpace = computed(() => findSelectedSpace(spaces.value, currentSpaceId.value));

onMounted(loadSpaces);

async function loadSpaces() {
    spaceLoading.value = true;
    try {
        spaces.value = await remoteCallApi.listSpaces(20, 1);
        persistSpaceId(resolveSpaceSelection(spaces.value, localStorage.getItem(SPACE_STORAGE_KEY) || ''));
    } catch (error) {
        spaces.value = [];
    } finally {
        spaceLoading.value = false;
        spacesReady.value = true;
    }
}

function handleSpaceChange(spaceId) {
    persistSpaceId(spaceId);
    window.location.reload();
}

function persistSpaceId(spaceId) {
    currentSpaceId.value = spaceId || '';
    if (currentSpaceId.value) {
        localStorage.setItem(SPACE_STORAGE_KEY, currentSpaceId.value);
    } else {
        localStorage.removeItem(SPACE_STORAGE_KEY);
    }
}
</script>

<template>
  <main class="app-shell">
    <header class="app-header">
      <div class="brand-space-row">
        <div class="brand-block">
          <span>智能体平台</span>
          <strong>评测中心</strong>
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
            @change="handleSpaceChange"
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
            <span>{{ item.title }}</span>
            <small>{{ item.eyebrow }}</small>
          </RouterLink>
        </nav>
      </aside>

      <section class="workspace">
        <RouterView v-if="spacesReady" />
        <div v-else class="workspace-loading">正在加载空间信息...</div>
      </section>
    </div>
  </main>
</template>
