<template>
  <div class="source-data flex h-full min-h-0 flex-col gap-3 overflow-hidden">
    <SourceDataFilter :conditions="conditions" :columns="columnNames" :type-of="typeOf" :loading="loading"
                      @add="addCondition()" @clear="clearConditions" @remove="removeCondition" @search="onSearch" @reset="onReset"
                      @change-column="changeColumn" @change-operator="changeOperator"/>

    <div class="flex shrink-0 flex-wrap items-center justify-between gap-2">
      <div class="flex flex-wrap items-center gap-2">
        <span class="mr-2 text-sm text-content-tertiary">{{ $t('source.data.total', { count: total.toLocaleString() }) }}</span>
        <MfButton v-if="can('data:insert')" size="sm" :disabled="saving" @click="openEditor('add')"><Plus :size="14"/>{{ $t('source.data.add') }}</MfButton>
        <MfButton v-if="can('data:modify')" size="sm" :disabled="saving || selectedRows.length !== 1" @click="openEditor('edit', selectedRows)"><Pencil :size="14"/>{{ $t('common.edit') }}</MfButton>
        <MfButton v-if="can('data:delete')" size="sm" variant="danger" :disabled="saving || !selectedRows.length" @click="requestDelete(selectedRows)"><Trash2 :size="14"/>{{ $t('common.delete') }}</MfButton>
        <MfButton v-if="can('data:insert')" size="sm" :disabled="saving || !selectedRows.length" @click="openEditor('copy', selectedRows)"><Copy :size="14"/>{{ $t('source.data.copy') }}</MfButton>
        <MfDropdown trigger="click">
          <MfButton size="sm" :disabled="!rows.length"><Download :size="14"/>{{ $t('source.data.export') }}<ChevronDown :size="12"/></MfButton>
          <template #overlay>
            <MfMenu>
              <MfMenuItem @click="exportRows('csv')">{{ $t('source.data.exportCsv') }}</MfMenuItem>
              <MfMenuItem @click="exportRows('json')">{{ $t('source.data.exportJson') }}</MfMenuItem>
            </MfMenu>
          </template>
        </MfDropdown>
      </div>
      <div class="flex items-center gap-2">
        <MfDropdown trigger="click" placement="bottomRight">
          <MfButton size="sm" :disabled="!columnNames.length"><Columns3 :size="14"/>{{ $t('source.data.columnSettings') }}</MfButton>
          <template #overlay>
            <div class="flex max-h-80 min-w-48 flex-col gap-1 overflow-y-auto p-2" @click.stop>
              <MfCheckbox v-for="name in columnNames" :key="name" :model-value="!hidden.has(name)"
                          :disabled="!hidden.has(name) && visibleColumns.length === 1" @update:model-value="value => toggleColumn(name, !!value)">{{ name }}</MfCheckbox>
            </div>
          </template>
        </MfDropdown>
        <MfDropdown trigger="click" placement="bottomRight">
          <MfButton size="sm" :disabled="!columnNames.length"><ArrowUpDown :size="14"/>{{ order ? `${order.name} ${order.order.toUpperCase()}` : $t('source.data.sort') }}<ChevronDown :size="12"/></MfButton>
          <template #overlay>
            <MfMenu>
              <MfMenuItem :disabled="!order" @click="setOrder(null)">{{ $t('source.data.noSort') }}</MfMenuItem>
              <template v-for="name in columnNames" :key="name">
                <MfMenuItem @click="setOrder(name, 'asc')"><span class="inline-flex items-center gap-2"><ArrowUp :size="13"/>{{ name }}</span></MfMenuItem>
                <MfMenuItem @click="setOrder(name, 'desc')"><span class="inline-flex items-center gap-2"><ArrowDown :size="13"/>{{ name }}</span></MfMenuItem>
              </template>
            </MfMenu>
          </template>
        </MfDropdown>
        <MfDropdown trigger="click" placement="bottomRight">
          <MfButton size="sm" :aria-label="$t('source.data.settings')"><Settings :size="14"/></MfButton>
          <template #overlay>
            <MfMenu>
              <MfMenuItem :disabled="!sql" @click="sqlVisible = true">{{ $t('source.data.viewSql') }}</MfMenuItem>
              <MfMenuItem @click="refresh">{{ $t('common.refresh') }}</MfMenuItem>
            </MfMenu>
          </template>
        </MfDropdown>
      </div>
    </div>

    <div class="flex min-h-0 flex-1 flex-col overflow-hidden rounded-mf-md border border-line">
      <MfTable fill :columns="tableColumns" :data-source="rows" row-key="__key" density="compact" :bordered="false" :loading="loading" :empty-text="$t('common.noData')"
               :row-class="rowClass" class="cursor-default" @row-click="onRowClick" @mousedown="onTableMouseDown">
        <template #header-__check__><MfCheckbox :model-value="allSelected" :indeterminate="partialSelected" :aria-label="$t('source.data.selectAll')" @update:model-value="value => toggleAll(!!value)"/></template>
        <template #cell-__check__="{ record, index }"><span class="inline-flex" @click.stop><MfCheckbox :model-value="selected.has(record.__key)" :aria-label="$t('source.data.selectRow')" @update:model-value="value => onCheck(record, index, !!value)"/></span></template>
        <template #cell-__index__="{ index }"><span class="tabular-nums text-content-secondary">{{ (page - 1) * size + index + 1 }}</span></template>
        <template v-for="name in visibleColumns" :key="name" #[`cell-${name}`]="{ record }">
          <span v-if="record[name] === null || record[name] === undefined" class="text-content-tertiary">NULL</span>
          <div v-else class="max-w-72"><EllipsisCell :text="cellText(record[name])"/></div>
        </template>
        <template #cell-__action__="{ record }">
          <div class="flex items-center gap-4 text-xs" @click.stop>
            <button v-if="can('data:modify')" type="button" class="text-primary hover:underline disabled:opacity-50" :disabled="saving" @click="openEditor('edit', [record])">{{ $t('common.edit') }}</button>
            <button v-if="can('data:delete')" type="button" class="text-danger hover:underline disabled:opacity-50" :disabled="saving" @click="requestDelete([record])">{{ $t('common.delete') }}</button>
          </div>
        </template>
      </MfTable>
    </div>

    <div class="relative flex shrink-0 items-center justify-center pb-1">
      <MfPagination :current="page" :page-size="size" :total="total" :size-text="$t('source.data.pageSize', { size: '{size}' })" @change="onPageChange"/>
      <label v-if="total > size" class="absolute right-0 flex items-center gap-2 whitespace-nowrap text-sm text-content-secondary">
        <span>{{ $t('source.data.jumpTo') }}</span>
        <div class="w-16 shrink-0"><MfInput v-model="jump" size="sm" @keydown.enter="onJump"/></div>
        <span>{{ $t('source.data.jumpPage') }}</span>
      </label>
    </div>

    <SourceRowEditor v-if="editorVisible" v-model="editorValues" v-model:json="editorJson" v-model:continue-adding="continueAdding" :open="editorVisible"
                     :mode="editorMode" :tab="editorTab" :form-disabled="editorMultiple" :fields="fields" :issues="editorIssues" :json-error="jsonError" :saving="saving"
                     :width="fieldPanelWidth" :container="fieldPanelHost" @close="closeEditor()" @tab="setEditorTab" @save="submitEditor"/>
    <TableRowDelete v-if="deleteTargets.length" :is-visible="!!deleteTargets.length" :columns="deleteRows" @close="closeDelete"/>
    <SqlInfo v-if="sqlVisible" :is-visible="sqlVisible" :content="sql" @close="sqlVisible = false"/>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowDown, ArrowUp, ArrowUpDown, ChevronDown, Columns3, Copy, Download, Pencil, Plus, Settings, Trash2 } from '@lucide/vue'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import SqlInfo from '@/views/components/sql/SqlInfo.vue'
import TableRowDelete from './components/TableRowDelete.vue'
import SourceDataFilter from './components/SourceDataFilter.vue'
import SourceRowEditor from './components/SourceRowEditor.vue'
import { useSourceTableData } from '@/composables/useSourceTableData'

defineOptions({ name: 'SourceTableData' })
const { t } = useI18n()
const {
  can, loading, saving, rows, total, page, size, sql, sqlVisible, columnNames, visibleColumns, hidden, order, typeOf,
  conditions, addCondition, changeColumn, changeOperator, removeCondition, clearConditions, onSearch, onReset,
  onPageChange, setOrder, toggleColumn, selected, selectedRows, allSelected, partialSelected, toggleAll, onRowClick, onCheck, rowClass,
  editorVisible, editorMode, editorValues, editorTab, editorJson, editorIssues, jsonError, continueAdding, editorMultiple, fields, openEditor, closeEditor, setEditorTab, submitEditor, fieldPanelHost, fieldPanelWidth,
  deleteTargets, deleteRows, requestDelete, closeDelete, exportRows, refresh,
} = useSourceTableData()

const tableColumns = computed(() => [
  { title: '', dataIndex: '__check__', width: 44, fixed: 'left' as const },
  { title: '#', dataIndex: '__index__', width: 56 },
  ...visibleColumns.value.map(name => ({ title: name, dataIndex: name })),
  ...(can('data:modify') || can('data:delete') ? [{ title: t('common.action'), dataIndex: '__action__', fixed: 'right' as const }] : []),
])
const cellText = (value: unknown) => typeof value === 'object' ? JSON.stringify(value) : String(value)
// Shift-click extends the row selection; keep the browser from selecting text across rows.
const onTableMouseDown = (event: MouseEvent) => { if (event.shiftKey) event.preventDefault() }
const jump = ref('')
function onJump() {
  const target = Math.trunc(Number(jump.value))
  const pages = Math.max(1, Math.ceil(total.value / size.value))
  if (target >= 1 && target <= pages) onPageChange(target, size.value)
  jump.value = ''
}
</script>
