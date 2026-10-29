<template>
  <div v-if="!originalDatabase">
    <MfEmpty variant="initial" :title="$t('source.tip.notSelectedNode')"/>
  </div>

  <template v-else>
    <!-- 面包屑 + 页级操作 -->
    <div class="mb-3 flex items-center justify-between gap-3">
      <nav class="flex min-w-0 items-center gap-1.5 text-sm text-content-tertiary">
        <Database :size="13" class="shrink-0"/>
        <router-link :to="`/source/${ originalSource }/d/${ originalDatabase }`"
                     class="transition-colors hover:text-primary">{{ $t('source.common.database') }}</router-link>
        <ChevronRight :size="13"/>
        <span>{{ $t('source.common.table') }}</span>
        <template v-if="tableInfo?.object_name">
          <ChevronRight :size="13"/>
          <span class="min-w-0 truncate font-mono text-content">{{ tableInfo.object_name }}</span>
        </template>
      </nav>

      <div class="flex shrink-0 items-center gap-2">
        <MfButton size="sm" @click="onRefresh">
          <RefreshCw :size="13"/>
          {{ $t('common.refresh') }}
        </MfButton>
        <MfButton size="sm" @click="onStructureCompare">
          <GitCompare :size="13"/>
          {{ $t('source.common.structureCompare') }}
          <ChevronDown :size="12"/>
        </MfButton>
        <MfDropdown trigger="click" placement="bottomRight">
          <MfButton size="sm">
            <Ellipsis :size="13"/>
            {{ $t('source.common.moreActions') }}
            <ChevronDown :size="12"/>
          </MfButton>
          <template #overlay>
            <MfMenu>
              <MfMenuItem @click="tableCreateVisible = true">
                <div class="flex items-center gap-2"><Table :size="14"/><span>{{ $t('source.common.menuNewTable') }}</span></div>
              </MfMenuItem>
              <MfMenuItem @click="tableExportVisible = true">
                <div class="flex items-center gap-2"><ArrowUpToLine :size="14"/><span>{{ $t('source.common.exportData') }}</span></div>
              </MfMenuItem>
              <MfMenuItem @click="tableTruncateVisible = true">
                <div class="flex items-center gap-2"><Eraser :size="14"/><span>{{ $t('source.common.truncateTable') }}</span></div>
              </MfMenuItem>
              <MfMenuItem @click="tableDropVisible = true">
                <div class="flex items-center gap-2"><Trash2 :size="14"/><span>{{ $t('source.common.dropTable') }}</span></div>
              </MfMenuItem>
            </MfMenu>
          </template>
        </MfDropdown>
      </div>
    </div>

    <!-- 表头：图标 + 表名 + 复制 + 注释 + 引擎/格式徽标 -->
    <div v-if="tableInfo" class="mb-4 flex items-start gap-3">
      <span class="inline-flex size-10 shrink-0 items-center justify-center rounded-mf-lg bg-primary/10 text-primary">
        <Table2 :size="20"/>
      </span>
      <div class="min-w-0 flex-1">
        <div class="flex items-center gap-2">
          <h2 class="m-0 truncate font-mono text-xl font-semibold text-content">{{ tableInfo.object_name }}</h2>
          <button type="button" class="text-content-tertiary transition-colors hover:text-primary"
                  :title="$t('source.common.tableName')" @click="onCopyName">
            <Copy :size="14"/>
          </button>
        </div>
        <p v-if="tableInfo.object_comment" class="mb-0 mt-0.5 text-sm text-content-secondary">{{ tableInfo.object_comment }}</p>
        <div class="mt-1.5 flex items-center gap-1.5">
          <MfTag v-if="tableInfo.object_engine" size="sm" variant="info">{{ tableInfo.object_engine }}</MfTag>
          <MfTag v-if="tableInfo.object_format" size="sm">{{ tableInfo.object_format }}</MfTag>
        </div>
      </div>
    </div>

    <MfTabs v-model:active-key="selectTab" @change="onChange">
    <MfTabPane item-key="info" :tab="$t('source.common.info')">
      <template #tab>
        <span class="inline-flex items-center gap-1.5">
          <Info :size="16" />
          {{ $t('source.common.info') }}
        </span>
      </template>
      <RouterView/>
    </MfTabPane>

    <MfTabPane item-key="structure" :tab="$t('source.common.structure')" :disabled="!originalTable">
      <template #tab>
        <span class="inline-flex items-center gap-1.5">
          <LayoutGrid :size="16" />
          {{ $t('source.common.structure') }}
        </span>
      </template>
      <RouterView/>
    </MfTabPane>

    <MfTabPane item-key="data" :tab="$t('source.common.tableData')" :disabled="!originalTable">
      <template #tab>
        <span class="inline-flex items-center gap-1.5">
          <Table :size="16" />
          {{ $t('source.common.tableData') }}
        </span>
      </template>
      <RouterView/>
    </MfTabPane>

    <MfTabPane item-key="indexes" :tab="$t('source.tab.indexes') + ` (${ indexCount })`" :disabled="!originalTable">
      <template #tab>
        <span class="inline-flex items-center gap-1.5">
          <Layers :size="16" />
          {{ $t('source.tab.indexes') }} ({{ indexCount }})
        </span>
      </template>
      <RouterView/>
    </MfTabPane>

    <MfTabPane item-key="foreignkeys" :tab="$t('source.tab.foreignKeys') + ` (${ foreignKeyCount })`" :disabled="!originalTable">
      <template #tab>
        <span class="inline-flex items-center gap-1.5">
          <GitBranch :size="16" />
          {{ $t('source.tab.foreignKeys') }} ({{ foreignKeyCount }})
        </span>
      </template>
      <RouterView/>
    </MfTabPane>

    <MfTabPane item-key="statement" :tab="$t('source.common.statement')" :disabled="!originalTable">
      <template #tab>
        <span class="inline-flex items-center gap-1.5">
          <Server :size="16" />
          {{ $t('source.common.statement') }}
        </span>
      </template>
      <RouterView/>
    </MfTabPane>

    <MfTabPane item-key="erDiagram" :tab="$t('source.common.erDiagram')" :disabled="!originalTable">
      <template #tab>
        <span class="inline-flex items-center gap-1.5">
          <Cloud :size="16" />
          {{ $t('source.common.erDiagram') }}
        </span>
      </template>
      <RouterView/>
    </MfTabPane>
  </MfTabs>
  </template>

  <TableCreate v-if="tableCreateVisible" :is-visible="tableCreateVisible" @close="tableCreateVisible = false"/>
  <TableExport v-if="tableExportVisible" :is-visible="tableExportVisible" @close="tableExportVisible = false"/>
  <TableTruncate v-if="tableTruncateVisible" :is-visible="tableTruncateVisible" @close="tableTruncateVisible = false"/>
  <TableDrop v-if="tableDropVisible" :is-visible="tableDropVisible" @close="tableDropVisible = false"/>
</template>

<script lang="ts" setup>
import { computed, inject, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import {
  ArrowUpToLine, ChevronDown, ChevronRight, Cloud, Copy, Database, Ellipsis, Eraser,
  GitBranch, GitCompare, Info, Layers, LayoutGrid, RefreshCw, Server, Table, Table2, Trash2
} from '@lucide/vue'
import { ObjectUtils } from '@/utils/object'
import TableCreate from '@/views/pages/admin/source/components/TableCreate.vue'
import TableExport from '@/views/pages/admin/source/components/TableExport.vue'
import TableTruncate from '@/views/pages/admin/source/components/TableTruncate.vue'
import TableDrop from '@/views/pages/admin/source/components/TableDrop.vue'

defineOptions({ name: 'MetadataContent' })

const { t } = useI18n()
const tableInfo = inject<import('vue').Ref<any | null>>('metadataTableInfo', ref(null))
const stats = inject<{ indexes: import('vue').Ref<any[]>; foreignKeys: import('vue').Ref<any[]> } | null>('metadataTableStats', null)
const refreshKey = inject<import('vue').Ref<number> | null>('metadataRefreshKey', null)

const indexCount = computed(() => {
  const rows = stats?.indexes.value || []
  return new Set(rows.map((row: any) => row.object_name)).size
})
const foreignKeyCount = computed(() => (stats?.foreignKeys.value || []).length)

const tableCreateVisible = ref(false)
const tableExportVisible = ref(false)
const tableTruncateVisible = ref(false)
const tableDropVisible = ref(false)

const onRefresh = () => {
  if (refreshKey) {
    refreshKey.value++
  }
}

const onStructureCompare = () => {
  MfMessage.info(t('common.developing'))
}

const onCopyName = () => {
  if (tableInfo.value?.object_name) {
    ObjectUtils.copy(String(tableInfo.value.object_name))
    MfMessage.success(t('query.tip.copySuccess'))
  }
}

const route = useRoute()
const router = useRouter()

const selectTab = ref('')
const originalSource = ref<string | null>(null)
const originalDatabase = ref<string | null>(null)
const originalTable = ref<string | undefined>(undefined)

const handleInitialize = () => {
  originalSource.value = String(route.params?.source || '')
  originalDatabase.value = String(route.params?.database || '')
  originalTable.value = route.params?.table ? String(route.params.table) : undefined
  selectTab.value = String(route.meta.type || '')
}

const onChange = () => router.push(`/source/${ originalSource.value }/d/${ originalDatabase.value }/t/${ selectTab.value }/${ originalTable.value }`)

watch(
    () => route.params.table,
    () => handleInitialize()
)

onMounted(() => handleInitialize())
</script>
