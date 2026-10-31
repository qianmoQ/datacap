<template>
  <div class="flex min-h-full flex-col p-2">
    <MfEmpty v-if="!source" class="flex-1" variant="initial" :title="$t('compare.chooseSource')">
      <MfSelect class="mt-3 w-72" :options="sourceOptions" :placeholder="$t('compare.chooseSource')" @update:model-value="value => openSource(String(value))"/>
    </MfEmpty>

    <section v-else class="flex min-h-0 flex-1 flex-col rounded-mf-lg border border-line bg-surface">
      <header class="shrink-0 border-b border-line px-6 py-4">
        <h1 class="m-0 text-xl font-semibold text-content">{{ sourceTable ? $t('compare.title', { table: sourceTable }) : $t('navigation.dataCompare') }}</h1>
      </header>
      <MfTabs :active-key="mode" class="compare-tabs shrink-0 px-6" @change="setMode">
        <MfTabPane item-key="structure" :tab="$t('compare.structure')"/>
        <MfTabPane item-key="data" :tab="$t('compare.data')"/>
      </MfTabs>

      <!-- Structure -->
      <div v-if="mode === 'structure'" class="flex min-h-0 flex-1 flex-col gap-4 p-6">
        <div class="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_auto]">
          <div class="rounded-mf-md border border-line p-4">
            <h3 class="m-0 mb-3 flex items-center gap-2 text-base font-semibold text-content"><Database :size="18" class="text-info"/>{{ $t('compare.sourceTable') }}</h3>
            <div class="flex flex-wrap items-center gap-4">
              <MfSelect class="w-72" searchable :model-value="sourceTable" :options="sourceTables" :placeholder="$t('compare.chooseTable')" @update:model-value="value => setSourceTable(String(value))"/>
              <span class="flex items-center gap-1.5 text-sm text-content-secondary"><Database :size="15"/>{{ database }}（{{ $t('compare.current') }}）</span>
            </div>
          </div>
          <div class="rounded-mf-md border border-line p-4">
            <h3 class="m-0 mb-3 flex items-center gap-2 text-base font-semibold text-content"><Circle :size="18" class="text-info"/>{{ $t('compare.targetTable') }}</h3>
            <div class="flex flex-wrap items-center gap-3">
              <MfSelect v-model="targetTable" class="w-72" searchable :options="targetTables" :placeholder="$t('compare.chooseTable')"/>
              <MfSelect class="w-56" searchable :model-value="targetDatabase" :options="databases" @update:model-value="value => setTargetDatabase(String(value))">
                <template #prefix><Database :size="15"/></template>
              </MfSelect>
            </div>
          </div>
          <div class="flex flex-col gap-3">
            <div class="flex justify-end gap-2">
              <MfButton :disabled="!ready" @click="swap"><ArrowLeftRight :size="15"/>{{ $t('compare.swap') }}</MfButton>
              <MfButton :disabled="!ready" :loading="structureLoading" @click="compareStructure"><RefreshCw :size="15"/>{{ $t('compare.again') }}</MfButton>
            </div>
            <div class="rounded-mf-md bg-success/10 px-4 py-3">
              <p class="m-0 mb-2 text-sm font-semibold text-success">{{ $t('compare.result') }}</p>
              <div class="grid grid-cols-4 divide-x divide-line text-center">
                <div v-for="item in SUMMARY" :key="item.key" class="px-3">
                  <p class="m-0 text-lg font-semibold tabular-nums" :class="item.tone">{{ structureCounts[item.key] }}</p>
                  <p class="m-0 text-xs" :class="item.tone">{{ $t(`compare.status.${item.key}`) }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="structureLoading && !structureRows.length" class="flex flex-1 items-center justify-center py-16"><MfSpinner/></div>
        <MfAlert v-else-if="structureError" variant="danger" :title="structureError"/>
        <MfEmpty v-else-if="!ready" class="flex-1 py-10" :title="$t('compare.chooseBoth')"/>
        <div v-else class="grid min-h-0 grid-cols-1 gap-3 lg:grid-cols-2" :class="structureLoading && 'opacity-60'">
          <div v-for="side in (['source', 'target'] as const)" :key="side" class="min-w-0 overflow-auto rounded-mf-md border border-line">
            <table class="w-full border-collapse text-sm">
              <thead class="bg-surface-hover text-left text-content-secondary">
                <tr><th v-for="column in STRUCTURE_COLUMNS" :key="column" class="whitespace-nowrap px-3 py-2 font-medium">{{ $t(`compare.column.${column}`) }}</th></tr>
              </thead>
              <tbody>
                <tr v-for="row in sides[side]" :key="row.name" class="border-t border-line" :class="rowTone(side, row.status)">
                  <td class="whitespace-nowrap px-3 py-2">
                    <span class="inline-flex items-center gap-2" :class="markTone(side, row.status)">
                      <component :is="markIcon(side, row.status)" v-if="markIcon(side, row.status)" :size="15"/>
                      <span :class="!markIcon(side, row.status) && 'pl-[23px]'">{{ row.name }}</span>
                    </span>
                  </td>
                  <td class="whitespace-nowrap px-3 py-2" :class="cellTone(side, row, 'type')">{{ row.column.type }}</td>
                  <td class="whitespace-nowrap px-3 py-2" :class="[cellTone(side, row, 'nullable'), !row.column.nullable && !cellTone(side, row, 'nullable') && 'text-danger']">
                    {{ row.column.nullable ? 'YES' : 'NO' }}
                  </td>
                  <td class="whitespace-nowrap px-3 py-2" :class="cellTone(side, row, 'defaultValue')">{{ row.column.defaultValue ?? (row.column.nullable ? 'NULL' : '-') }}</td>
                  <td class="px-3 py-2" :class="cellTone(side, row, 'comment')">
                    {{ row.column.comment || '-' }}<span v-if="onlyHere(side, row.status)">（{{ $t(`compare.only.${side}`) }}）</span>
                  </td>
                </tr>
                <tr v-if="!sides[side].length"><td :colspan="STRUCTURE_COLUMNS.length" class="px-3 py-6 text-center text-content-tertiary">{{ $t('compare.noRows') }}</td></tr>
              </tbody>
            </table>
          </div>
        </div>

        <footer class="mt-auto flex shrink-0 items-center justify-between gap-3">
          <MfSelect class="w-48" :model-value="onlyDifferent ? 'different' : 'all'" :options="filterOptions" @update:model-value="value => (onlyDifferent = value === 'different')">
            <template #prefix><Filter :size="15"/></template>
          </MfSelect>
          <div class="flex shrink-0 gap-2">
            <MfButton @click="back">{{ $t('common.cancel') }}</MfButton>
            <ExportMenu :disabled="!ready" :loading="exporting" :label="$t('compare.exportStructure')" @export="format => exportResult('structure', format)"/>
          </div>
        </footer>
      </div>

      <!-- Data -->
      <div v-else class="grid min-h-0 flex-1 grid-cols-1 lg:grid-cols-[22rem_minmax(0,1fr)]">
        <aside class="flex flex-col gap-4 border-line p-6 lg:border-r">
          <h3 class="m-0 text-base font-semibold text-content">{{ $t('compare.config') }}</h3>
          <div class="grid grid-cols-[5rem_minmax(0,1fr)] items-center gap-x-3 gap-y-4 text-sm">
            <span class="text-content-secondary">{{ $t('compare.table') }}</span>
            <span class="min-w-0 truncate text-content">{{ sourceTable || '-' }} → {{ targetDatabase }}.{{ targetTable || '-' }}</span>
            <span class="text-content-secondary">{{ $t('compare.pairBy') }}</span>
            <MfRadioGroup v-model="dataForm.mode" class="flex flex-wrap items-center gap-4">
              <MfRadio value="primary">{{ $t('compare.byPrimary') }}</MfRadio>
              <MfRadio value="fields"><span class="inline-flex items-center gap-1">{{ $t('compare.byFields') }}
                <MfTooltip :content="$t('compare.byFieldsHint')"><CircleHelp :size="14" class="text-content-tertiary"/></MfTooltip></span></MfRadio>
            </MfRadioGroup>
            <span class="text-content-secondary">{{ $t('compare.keyFields') }}</span>
            <MfSelect v-if="dataForm.mode === 'primary'" :model-value="primaryKeys.join(', ') || $t('compare.noPrimaryKey')" disabled :options="[]"/>
            <MfSelect v-else v-model="dataForm.keys" multiple searchable :options="columnNames.map(name => ({ label: name, value: name }))" :placeholder="$t('compare.chooseFields')"/>
            <span class="text-content-secondary">{{ $t('compare.scope') }}</span>
            <div class="flex flex-wrap items-center gap-3">
              <MfRadioGroup v-model="dataForm.scope" class="flex items-center gap-4">
                <MfRadio value="all">{{ $t('source.export.scopeAll') }}</MfRadio>
                <MfRadio value="range">{{ $t('compare.range') }}</MfRadio>
              </MfRadioGroup>
              <div v-if="dataForm.scope === 'range'" class="flex items-center gap-2">
                <MfInputNumber v-model="dataForm.start" :min="1" :precision="0" :controls="false" class="w-20" :aria-label="$t('compare.rangeStart')"/>
                <span class="text-content-tertiary">-</span>
                <MfInputNumber v-model="dataForm.end" :min="1" :precision="0" :controls="false" class="w-24" :aria-label="$t('compare.rangeEnd')"/>
              </div>
            </div>
            <span class="self-start text-content-secondary">{{ $t('compare.kinds') }}</span>
            <div class="flex flex-col gap-2.5">
              <MfCheckbox v-model="dataForm.onlyChanged">{{ $t('compare.onlyChanged') }}</MfCheckbox>
              <MfCheckbox v-model="dataForm.includeMissing">{{ $t('compare.includeMissing') }}</MfCheckbox>
            </div>
          </div>
          <div class="mt-auto flex gap-3 pt-2">
            <MfButton :disabled="dataLoading" @click="resetData">{{ $t('compare.reset') }}</MfButton>
            <MfButton variant="primary" :loading="dataLoading" :disabled="!ready || (dataForm.mode === 'fields' && !dataForm.keys.length)" @click="compareData">{{ $t('compare.start') }}</MfButton>
          </div>
        </aside>

        <div class="flex min-h-0 min-w-0 flex-col gap-3 p-6">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <div class="flex items-baseline gap-3">
              <h3 class="m-0 text-base font-semibold text-content">{{ $t('compare.result') }}</h3>
              <span v-if="dataResult" class="text-xs text-content-tertiary">{{ $t('compare.dataSummary', { count: dataResult.total }) }}</span>
            </div>
            <div class="flex items-center gap-4 text-xs text-content-secondary">
              <span v-for="item in LEGEND" :key="item.key" class="inline-flex items-center gap-1.5"><span class="h-3 w-5 rounded-mf-sm" :class="item.tone"/>{{ $t(`compare.legend.${item.key}`) }}</span>
            </div>
          </div>
          <div v-if="dataLoading && !dataResult" class="flex flex-1 items-center justify-center py-16"><MfSpinner/></div>
          <MfAlert v-else-if="dataError" variant="danger" :title="dataError"/>
          <MfEmpty v-else-if="!dataResult" class="flex-1 py-10" :title="$t(ready ? 'compare.startHint' : 'compare.chooseBoth')"/>
          <template v-else>
            <div class="min-h-0 flex-1 overflow-auto rounded-mf-md border border-line" :class="dataLoading && 'opacity-60'">
              <table class="w-full border-collapse text-sm">
                <thead class="sticky top-0 z-[1] bg-surface-hover text-left text-content-secondary">
                  <tr>
                    <th class="px-3 py-2 font-medium">#</th>
                    <th class="whitespace-nowrap px-3 py-2 font-medium">{{ $t('compare.diffType') }}</th>
                    <th v-for="column in dataResult.columns" :key="column" class="whitespace-nowrap px-3 py-2 font-medium">{{ column }}</th>
                    <th class="sticky right-0 whitespace-nowrap bg-surface-hover px-3 py-2 font-medium">{{ $t('common.action') }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, index) in dataResult.rows" :key="index" class="border-t border-line align-top">
                    <td class="px-3 py-2 tabular-nums text-content-secondary">{{ index + 1 }}</td>
                    <td class="whitespace-nowrap px-3 py-2" :class="TYPE_BG[row.type]">
                      <span class="inline-flex items-center gap-1.5" :class="TYPE_TEXT[row.type]"><component :is="TYPE_ICON[row.type]" :size="15"/>{{ $t(`compare.type.${row.type}`) }}</span>
                    </td>
                    <td v-for="(column, position) in dataResult.columns" :key="column" class="max-w-64 px-3 py-2"
                        :class="row.changed.includes(column) ? 'bg-danger/10' : TYPE_BG[row.type]">
                      <template v-if="row.changed.includes(column)">
                        <p class="m-0 truncate text-content">- {{ cellText(row.source?.[position]) }}</p>
                        <p class="m-0 truncate text-danger">- {{ cellText(row.target?.[position]) }}</p>
                      </template>
                      <span v-else class="block truncate text-content">{{ cellText((row.source || row.target)?.[position]) }}</span>
                    </td>
                    <td class="sticky right-0 whitespace-nowrap bg-surface px-3 py-2">
                      <button type="button" class="text-xs text-primary hover:underline" @click="detail = row">{{ $t('compare.detail') }}</button>
                    </td>
                  </tr>
                  <tr v-if="!dataResult.rows.length"><td :colspan="dataResult.columns.length + 3" class="px-3 py-6 text-center text-content-tertiary">{{ $t('compare.noDifference') }}</td></tr>
                </tbody>
              </table>
            </div>
            <p v-if="dataResult.truncated" class="m-0 text-xs text-warning">{{ $t('compare.truncated', { shown: dataResult.rows.length, total: dataResult.total }) }}</p>
          </template>
          <footer class="mt-auto flex shrink-0 justify-end gap-2">
            <MfButton @click="back">{{ $t('common.cancel') }}</MfButton>
            <ExportMenu :disabled="!ready || (dataForm.mode === 'fields' && !dataForm.keys.length)" :loading="exporting" :label="$t('compare.exportData')"
                        @export="format => exportResult('data', format)"/>
          </footer>
        </div>
      </div>
    </section>

    <MfModal :open="!!detail" width="720px" :title="$t('compare.detailTitle')" @update:open="value => !value && (detail = null)">
      <table v-if="detail && dataResult" class="w-full border-collapse text-sm">
        <thead class="bg-surface-hover text-left text-content-secondary">
          <tr><th class="px-3 py-2 font-medium">{{ $t('compare.column.name') }}</th><th class="px-3 py-2 font-medium">{{ $t('compare.left') }}</th><th class="px-3 py-2 font-medium">{{ $t('compare.right') }}</th></tr>
        </thead>
        <tbody>
          <tr v-for="(column, position) in dataResult.columns" :key="column" class="border-t border-line" :class="detail.changed.includes(column) && 'bg-danger/10'">
            <td class="whitespace-nowrap px-3 py-2 font-medium text-content">{{ column }}</td>
            <td class="break-all px-3 py-2 text-content">{{ detail.source ? cellText(detail.source[position]) : '—' }}</td>
            <td class="break-all px-3 py-2" :class="detail.changed.includes(column) ? 'text-danger' : 'text-content'">{{ detail.target ? cellText(detail.target[position]) : '—' }}</td>
          </tr>
        </tbody>
      </table>
      <template #footer><MfButton variant="primary" @click="detail = null">{{ $t('source.executed.close') }}</MfButton></template>
    </MfModal>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ArrowLeftRight, Circle, CircleHelp, CircleMinus, CirclePlus, Database, Filter, RefreshCw } from '@lucide/vue'
import SourceService from '@/services/source'
import { FilterModel } from '@/model/filter'
import { compareLocation } from '@/model/source-manage'
import { cellText, type DataType, type StructureSideRow, type StructureStatus } from '@/model/table-compare'
import { useTableCompare } from '@/composables/useTableCompare'
import ExportMenu from './components/CompareExportMenu.vue'

defineOptions({ name: 'CompareHome' })
const { t } = useI18n()
const router = useRouter()
const {
  source, database, mode, sourceTable, databases, sourceTables, targetTables, targetDatabase, targetTable, ready,
  structureLoading, structureRows, structureCounts, structureError, onlyDifferent, sides,
  dataForm, dataLoading, dataResult, dataError, columnNames, primaryKeys, detail, exporting,
  setMode, setSourceTable, setTargetDatabase, swap, back, compareStructure, compareData, resetData, exportResult,
} = useTableCompare()

const STRUCTURE_COLUMNS = ['name', 'type', 'nullable', 'defaultValue', 'comment'] as const
const SUMMARY: { key: StructureStatus; tone: string }[] = [
  { key: 'changed', tone: 'text-danger' }, { key: 'source_only', tone: 'text-success' }, { key: 'target_only', tone: 'text-warning' }, { key: 'same', tone: 'text-content' },
]
const LEGEND = [{ key: 'changed', tone: 'bg-danger/15' }, { key: 'source_only', tone: 'bg-success/15' }, { key: 'target_only', tone: 'bg-warning/15' }]
const TYPE_BG: Record<DataType, string> = { same: '', changed: '', source_only: 'bg-success/10', target_only: 'bg-warning/10' }
const TYPE_TEXT: Record<DataType, string> = { same: 'text-content-secondary', changed: 'text-danger', source_only: 'text-success', target_only: 'text-content' }
const TYPE_ICON: Record<DataType, unknown> = { same: Circle, changed: CircleMinus, source_only: CirclePlus, target_only: CircleMinus }
const filterOptions = computed(() => [{ label: t('compare.showAll'), value: 'all' }, { label: t('compare.showDifferent'), value: 'different' }])

// Source columns only on the left are additions; on the right, changes and target-only columns are marked.
const onlyHere = (side: 'source' | 'target', status: StructureStatus) => (side === 'source' && status === 'source_only') || (side === 'target' && status === 'target_only')
const rowTone = (side: 'source' | 'target', status: StructureStatus) =>
  onlyHere(side, status) ? (side === 'source' ? 'bg-success/10' : 'bg-warning/10') : side === 'target' && status === 'changed' ? 'bg-danger/10' : ''
const markTone = (side: 'source' | 'target', status: StructureStatus) =>
  onlyHere(side, status) ? (side === 'source' ? 'text-success' : 'text-warning') : side === 'target' && status === 'changed' ? 'text-danger' : 'text-content'
const markIcon = (side: 'source' | 'target', status: StructureStatus) =>
  side === 'source' && status === 'source_only' ? CirclePlus : (side === 'target' && (status === 'changed' || status === 'target_only')) ? CircleMinus : null
const cellTone = (side: 'source' | 'target', row: StructureSideRow, property: string) =>
  onlyHere(side, row.status) ? (side === 'source' ? 'text-success' : 'text-warning') : side === 'target' && row.changes.includes(property) ? 'text-danger' : 'text-content'

// Without a source in the address, pick one to start comparing in.
const sourceOptions = ref<{ label: string; value: string }[]>([])
if (!source.value) {
  SourceService.getAll(new FilterModel()).then(response => {
    if (response.status) sourceOptions.value = (response.data?.content || []).map((item: any) => ({ label: item.name, value: item.code }))
  })
}
const openSource = (code: string) => { if (code) router.replace(compareLocation({ source: code })) }
</script>

<style scoped>
.compare-tabs :deep([role='tablist']) { border-bottom: 0; }
</style>
