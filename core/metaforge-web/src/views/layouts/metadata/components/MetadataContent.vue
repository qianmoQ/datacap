<template>
  <div class="metadata-workspace-content flex h-full min-h-0 min-w-0 overflow-hidden">
    <div class="isolate flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden px-3 py-2">
      <div v-if="capabilityLoading" class="flex flex-1 items-center justify-center"><MfSpinner/></div>
      <div v-else-if="capabilityError"><MfAlert variant="danger" :title="capabilityError"/><MfButton size="sm" class="mt-2" @click="onRefresh"><RefreshCw :size="14"/>{{ $t('common.refresh') }}</MfButton></div>
      <MfEmpty v-else-if="!originalDatabase && profile?.kind === 'relational' && selectTab !== 'sql'" variant="initial" :title="$t('source.tip.notSelectedNode')"/>
      <template v-else>
        <div class="mb-2 flex min-h-8 shrink-0 flex-wrap items-center justify-between gap-2">
          <nav v-if="profile?.kind === 'relational'" class="flex min-w-0 items-center gap-2 text-xs text-content-tertiary" :aria-label="$t('source.common.database')">
            <Database :size="14" class="shrink-0"/>
            <router-link :to="databaseLocation" class="hover:text-primary">{{ $t('source.common.database') }}</router-link>
            <ChevronRight :size="12"/>
            <span>{{ originalTable ? $t('source.common.table') : originalDatabase }}</span>
            <template v-if="originalTable"><ChevronRight :size="12"/><span class="truncate font-medium text-content">{{ tableName }}</span></template>
          </nav>
          <span v-else class="text-sm font-medium text-content-secondary">{{ $t('source.manage.kind.' + (profile?.kind || 'unknown')) }}</span>
          <div class="flex shrink-0 items-center gap-2">
            <MfButton size="sm" @click="onRefresh"><RefreshCw :size="14"/>{{ $t('common.refresh') }}</MfButton>
            <MfButton v-if="can('column:read')" size="sm" :disabled="!originalTable" @click="onStructureCompare"><GitCompare :size="14"/>{{ $t('source.common.structureCompare') }}<ChevronDown :size="12"/></MfButton>
            <MfDropdown v-if="can('table:create') || can('table:export') || can('table:drop') || can('table:truncate') || can('tab:er-diagram')" trigger="click" placement="bottomRight">
              <MfButton size="sm">{{ $t('source.common.moreActions') }}<ChevronDown :size="12"/></MfButton>
              <template #overlay>
                <MfMenu>
                  <MfMenuItem v-if="can('table:create')" @click="tableCreateVisible = true"><span class="flex items-center gap-2"><Table :size="14"/>{{ $t('source.common.menuNewTable') }}</span></MfMenuItem>
                  <MfMenuItem v-if="can('table:export')" :disabled="!originalTable" @click="tableExportVisible = true"><span class="flex items-center gap-2"><ArrowUpToLine :size="14"/>{{ $t('source.common.exportData') }}</span></MfMenuItem>
                  <MfMenuItem v-if="can('table:truncate')" :disabled="!originalTable" @click="tableTruncateVisible = true"><span class="flex items-center gap-2"><Eraser :size="14"/>{{ $t('source.common.truncateTable') }}</span></MfMenuItem>
                  <MfMenuItem v-if="can('table:drop')" :disabled="!originalTable" @click="tableDropVisible = true"><span class="flex items-center gap-2"><Trash2 :size="14"/>{{ $t('source.common.dropTable') }}</span></MfMenuItem>
                  <MfMenuItem v-if="can('tab:er-diagram')" :disabled="!originalTable" @click="onChange('erDiagram')"><span class="flex items-center gap-2"><GitBranch :size="14"/>{{ $t('source.common.erDiagram') }}</span></MfMenuItem>
                </MfMenu>
              </template>
            </MfDropdown>
          </div>
        </div>

        <div v-if="originalTable && profile?.kind === 'relational'" class="mb-1.5 flex shrink-0 items-start gap-3 pt-1">
          <Table2 :size="32" class="mt-1 shrink-0 text-content-secondary"/>
          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-2">
              <h1 class="m-0 truncate text-xl font-semibold tracking-tight text-content">{{ tableName }}</h1>
              <MfTooltip :content="$t('source.prototype.copyTableName')"><button type="button" class="inline-flex size-6 items-center justify-center text-content-secondary hover:text-primary" :aria-label="$t('source.prototype.copyTableName')" @click="onCopyName"><Copy :size="14"/></button></MfTooltip>
            </div>
            <p v-if="tableInfo?.object_comment" class="mb-0 mt-1 line-clamp-2 text-sm text-content-tertiary">{{ tableInfo.object_comment }}</p>
            <div class="mt-1 flex items-center gap-2">
              <MfTag v-if="tableInfo?.object_engine" size="sm" variant="info">{{ tableInfo.object_engine }}</MfTag>
              <MfTag v-if="tableInfo?.object_format" size="sm" variant="info">{{ tableInfo.object_format }}</MfTag>
            </div>
          </div>
        </div>
        <MfAlert v-if="metadataError" variant="danger" class="mb-3 shrink-0" :title="metadataError"/>
        <MfTabs v-if="(originalTable || ['sql','keys','objects'].includes(selectTab)) && selectTab !== 'erDiagram'" fill class="metadata-tabs" :active-key="selectTab" @change="onChange">
          <MfTabPane v-for="tab in tabs" :key="tab.key" :item-key="tab.key" :tab="tab.label">
            <template #tab><span class="inline-flex items-center gap-2 whitespace-nowrap text-sm"><component :is="tabIcons[tab.key]" :size="15"/>{{ tab.label }}</span></template>
            <component :is="paneComponent" v-if="selectTab === tab.key" :key="paneKey"/>
          </MfTabPane>
        </MfTabs>
        <div v-if="(!originalTable && !['sql','keys','objects'].includes(selectTab)) || selectTab === 'erDiagram'" class="min-h-0 flex-1 overflow-auto"><component :is="paneComponent" v-if="paneComponent" :key="paneKey"/><MfEmpty v-else variant="initial" :title="$t('source.tip.notSelectedNode')"/></div>
      </template>
    </div>
  </div>
  <TableCreate v-if="tableCreateVisible" :is-visible="tableCreateVisible" @close="tableCreateVisible = false"/>
  <TableExport v-if="tableExportVisible" :is-visible="tableExportVisible" @close="tableExportVisible = false"/>
  <TableTruncate v-if="tableTruncateVisible" :is-visible="tableTruncateVisible" @close="tableTruncateVisible = false"/>
  <TableDrop v-if="tableDropVisible" :is-visible="tableDropVisible" @close="tableDropVisible = false"/>
</template>

<script setup lang="ts">
import { ArrowUpToLine, ChevronDown, ChevronRight, Copy, Database, Eraser, GitBranch, GitCompare, Layers, LayoutGrid, RefreshCw, Settings, SquarePlay, Table, Table2, Trash2, Zap, Code, KeyRound, FolderOpen } from '@lucide/vue'
import TableCreate from '@/views/pages/admin/source/components/TableCreate.vue'
import TableExport from '@/views/pages/admin/source/components/TableExport.vue'
import TableTruncate from '@/views/pages/admin/source/components/TableTruncate.vue'
import TableDrop from '@/views/pages/admin/source/components/TableDrop.vue'
import { useMetadataContent } from '@/composables/useMetadataContent'

defineOptions({ name: 'MetadataContent' })
const tabIcons: Record<string, unknown> = { keys: KeyRound, objects: FolderOpen, structure: LayoutGrid, data: Table2, sql: SquarePlay, indexes: Layers, foreignkeys: GitBranch, triggers: Zap, info: Settings, statement: Code }
const { can, profile, capabilityLoading, capabilityError, paneComponent, paneKey, databaseLocation, tableInfo, tableName, selectTab, tabs, metadataError,
  originalDatabase, originalTable, onChange, onRefresh, onStructureCompare, onCopyName,
  tableCreateVisible, tableExportVisible, tableTruncateVisible, tableDropVisible } = useMetadataContent()
</script>

<style scoped>
.metadata-tabs :deep([role='tablist']) { overflow-x: auto; scrollbar-width: thin; }
.metadata-tabs :deep([role='tab']) { padding-left: 11px; padding-right: 11px; }
</style>
