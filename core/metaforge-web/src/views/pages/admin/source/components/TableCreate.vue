<template>
  <MfDrawer v-model:open="visible" width="min(1280px, 94vw)" :title="$t('source.common.menuNewTable')">
    <div v-if="loading" class="flex h-full min-h-96 items-center justify-center">
      <MfSpinner/>
    </div>

    <MfForm v-else ref="formRef" :model="formModel" class="flex flex-col gap-4">
      <!-- 步骤条 -->
      <div class="flex items-center gap-3 rounded-mf-lg border border-line bg-surface px-5 py-4">
        <template v-for="(step, index) in steps" :key="step.titleKey">
          <div class="flex items-center gap-3">
            <span class="inline-flex size-9 shrink-0 items-center justify-center rounded-mf-full text-sm font-semibold"
                  :class="currentStep >= index + 1 ? 'bg-primary text-on-primary' : 'border border-line bg-surface text-content-tertiary'">
              <Check v-if="currentStep > index + 1" :size="15"/>
              <template v-else>{{ index + 1 }}</template>
            </span>
            <div class="leading-tight">
              <div class="text-sm font-semibold" :class="currentStep >= index + 1 ? 'text-content' : 'text-content-tertiary'">{{ $t(step.titleKey) }}</div>
              <div class="mt-0.5 text-xs text-content-tertiary">{{ $t(step.descKey) }}</div>
            </div>
          </div>
          <span v-if="index < steps.length - 1" class="h-px flex-1 bg-line"/>
        </template>
      </div>

      <div class="grid grid-cols-[minmax(0,1fr)_360px] items-start gap-4">
        <!-- 左列 -->
        <div class="flex min-w-0 flex-col gap-4">
          <!-- 基本信息 -->
          <section class="rounded-mf-lg border border-line bg-surface p-4">
            <h3 class="m-0 mb-3 text-base font-semibold text-content">{{ $t('source.create.basicInfo') }}</h3>
            <div class="grid grid-cols-2 gap-3">
              <MfFormItem name="name"
                          :label="$t('source.common.tableName')"
                          :rules="[
                              { required: true, message: $t('source.validator.tableName.required') },
                              { pattern: /^[A-Za-z][A-Za-z0-9_-]*$/, message: $t('source.validator.tableName.pattern') }
                          ]">
                <MfInput v-model="formState.name" placeholder="user"/>
              </MfFormItem>
              <MfFormItem :label="$t('source.create.sourceDb')">
                <MfInput :model-value="databaseName" disabled/>
              </MfFormItem>
            </div>
          </section>

          <!-- 字段定义 -->
          <section class="rounded-mf-lg border border-line bg-surface p-4">
            <div class="mb-3 flex items-center justify-between gap-2">
              <h3 class="m-0 text-base font-semibold text-content">{{ $t('source.create.fieldDefine') }}</h3>
              <div class="flex items-center gap-2">
                <MfButton size="sm" @click="onAdd"><Plus :size="13"/>{{ $t('source.structure.addField') }}</MfButton>
                <MfButton size="sm" :disabled="selectedRows.size === 0" @click="onMoveSelected(-1)"><ArrowUp :size="13"/>{{ $t('source.structure.moveUp') }}</MfButton>
                <MfButton size="sm" :disabled="selectedRows.size === 0" @click="onMoveSelected(1)"><ArrowDown :size="13"/>{{ $t('source.structure.moveDown') }}</MfButton>
                <MfButton size="sm" variant="danger" :disabled="selectedRows.size === 0" @click="onBatchDelete"><Trash2 :size="13"/>{{ $t('source.create.batchDelete') }}</MfButton>
              </div>
            </div>

            <div class="max-h-[224px] overflow-y-auto">
              <MfTable :columns="fieldColumns" :data-source="formState.columns" row-key="__row" density="compact" :bordered="false" empty-text=" ">
              <template #header-__check__>
                <MfCheckbox :model-value="allSelected" :indeterminate="partialSelected" :aria-label="$t('source.data.selectAll')"
                            @update:model-value="value => toggleAll(!!value)"/>
              </template>
              <template #cell-__check__="{ index }">
                <span class="inline-flex" @click.stop>
                  <MfCheckbox :model-value="selectedRows.has(index)" :aria-label="$t('source.data.selectRow')"
                              @update:model-value="value => toggleRow(index, !!value)"/>
                </span>
              </template>
              <template #cell-name="{ index }">
                <MfFormItem :name="`columns.${index}.name`"
                            :rules="[
                                { required: true, message: $t('source.validator.columnName.required') },
                                { pattern: /^[A-Za-z][A-Za-z0-9_-]*$/, message: $t('source.validator.columnName.pattern') }
                            ]">
                  <MfInput v-model="formState.columns[index].name" size="sm"/>
                </MfFormItem>
              </template>
              <template #cell-type="{ index }">
                <MfFormItem :name="`columns.${index}.type`"
                            :rules="[{ required: true, message: $t('source.validator.columnType.required') }]">
                  <MfSelect v-model="formState.columns[index].type" size="sm" :options="dataTypeOptions"/>
                </MfFormItem>
              </template>
              <template #cell-length="{ index }">
                <MfInput v-model="formState.columns[index].length" size="sm"/>
              </template>
              <template #cell-defaultValue="{ index }">
                <MfInput v-model="formState.columns[index].defaultValue" size="sm"/>
              </template>
              <template #cell-nullable="{ index }">
                <MfCheckbox v-model="formState.columns[index].nullable"/>
              </template>
              <template #cell-primaryKey="{ index }">
                <MfCheckbox v-model="formState.columns[index].primaryKey"/>
              </template>
              <template #cell-autoIncrement="{ index }">
                <MfCheckbox v-model="formState.columns[index].autoIncrement"/>
              </template>
              <template #cell-unique="{ index }">
                <MfCheckbox v-model="formState.columns[index].unique"/>
              </template>
              <template #cell-comment="{ index }">
                <MfInput v-model="formState.columns[index].comment" size="sm"/>
              </template>
              <template #cell-action="{ index }">
                <div class="flex items-center gap-2 text-xs">
                  <button type="button" class="text-primary hover:underline disabled:opacity-50" :disabled="index === 0" @click="onMoveColumn(index, -1)">{{ $t('source.structure.moveUp') }}</button>
                  <button type="button" class="text-primary hover:underline disabled:opacity-50" :disabled="index === formState.columns.length - 1" @click="onMoveColumn(index, 1)">{{ $t('source.structure.moveDown') }}</button>
                  <button type="button" class="text-danger hover:underline" @click="onRemove(index)">{{ $t('common.delete') }}</button>
                </div>
              </template>
            </MfTable>
            </div>
          </section>

          <!-- 索引与约束 -->
          <section class="rounded-mf-lg border border-line bg-surface p-4">
            <div class="mb-3 flex items-center justify-between gap-2">
              <h3 class="m-0 text-base font-semibold text-content">{{ $t('source.create.indexCard') }}</h3>
              <MfButton size="sm" :disabled="!canAddIndex" @click="onAddIndex">
                <Plus :size="13"/>{{ $t('source.create.addIndexConstraint') }}
              </MfButton>
            </div>

            <div class="flex items-center gap-1 border-b border-line">
              <button v-for="tab in indexTabs" :key="tab.key" type="button"
                      class="whitespace-nowrap border-b-2 px-3 py-2 text-sm font-medium transition-colors"
                      :class="indexTab === tab.key ? 'border-primary text-primary' : 'border-transparent text-content-secondary hover:text-content'"
                      @click="indexTab = tab.key">
                {{ $t(tab.labelKey) }} ({{ indexCounts[tab.key] }})
              </button>
            </div>

            <MfTable :columns="indexColumns" :data-source="visibleIndexRows" row-key="__key" density="compact" :bordered="false" empty-text=" ">
              <template #cell-name="{ row }">
                <MfFormItem v-if="row.editing"
                            :name="`indexes.${indexRows.indexOf(row)}.name`"
                            :rules="[{ required: true, message: $t('source.create.indexNameRequired') }]">
                  <MfInput v-model="row.name" size="sm"/>
                </MfFormItem>
                <span v-else class="font-mono text-xs text-content">{{ row.name }}</span>
              </template>
              <template #cell-kind="{ row }">
                <MfTag size="sm" :variant="row.kind === 'primary' ? 'success' : row.kind === 'unique' ? 'info' : 'default'">
                  {{ $t(row.kind === 'primary' ? 'source.create.tabPrimary' : row.kind === 'unique' ? 'source.create.tabUnique' : 'source.create.tabNormal') }}
                </MfTag>
              </template>
              <template #cell-columnsText="{ row }">
                <MfFormItem v-if="row.editing"
                            :name="`indexes.${indexRows.indexOf(row)}.columnsText`"
                            :rules="[{ required: true, message: $t('source.create.indexColumnsRequired') }]">
                  <MfInput v-model="row.columnsText" size="sm"/>
                </MfFormItem>
                <span v-else class="font-mono text-xs text-content">{{ row.columnsText }}</span>
              </template>
              <template #cell-method="{ row }">
                <MfSelect v-model="row.method" size="sm" :options="methodOptions" :disabled="!row.editing"/>
              </template>
              <template #cell-comment="{ row }">
                <MfInput v-if="row.editing" v-model="row.comment" size="sm"/>
                <span v-else class="text-xs text-content-secondary">{{ row.comment || '-' }}</span>
              </template>
              <template #cell-action="{ row }">
                <div class="flex items-center gap-2 text-xs">
                  <template v-if="row.editing">
                    <button type="button" class="text-primary hover:underline" @click="onSaveIndexRow(row)">{{ $t('common.confirm') }}</button>
                    <button type="button" class="text-content-tertiary hover:underline" @click="onCancelIndexRow(row)">{{ $t('common.cancel') }}</button>
                  </template>
                  <template v-else-if="row.kind !== 'primary'">
                    <button type="button" class="text-primary hover:underline" @click="row.editing = true">{{ $t('common.edit') }}</button>
                    <button type="button" class="text-danger hover:underline" @click="onDeleteIndexRow(row)">{{ $t('common.delete') }}</button>
                  </template>
                  <template v-else>
                    <span class="text-content-tertiary">-</span>
                  </template>
                </div>
              </template>
            </MfTable>
          </section>

          <!-- 高级选项 -->
          <section class="rounded-mf-lg border border-line bg-surface p-4">
            <h3 class="m-0 mb-3 text-base font-semibold text-content">{{ $t('source.create.advanced') }}</h3>
            <div class="flex flex-wrap items-center gap-6">
              <MfCheckbox v-model="advanced.addComment">{{ $t('source.create.optAddComment') }}</MfCheckbox>
              <MfCheckbox v-model="advanced.viewStructure">{{ $t('source.create.optViewStructure') }}</MfCheckbox>
              <MfCheckbox v-model="advanced.continueIndex">{{ $t('source.create.optContinueIndex') }}</MfCheckbox>
            </div>
          </section>
        </div>

        <!-- 右列 -->
        <div class="flex flex-col gap-4">
          <!-- 生成的 SQL -->
          <section class="rounded-mf-lg border border-line bg-surface p-4">
            <h3 class="m-0 mb-3 text-base font-semibold text-content">{{ $t('source.create.sqlCard') }}</h3>
            <div class="mb-3 flex items-center gap-1 border-b border-line">
              <button v-for="tab in sqlTabs" :key="tab.kind" type="button"
                      class="whitespace-nowrap border-b-2 px-2.5 py-1.5 text-xs font-medium transition-colors"
                      :class="sqlTab === tab.kind ? 'border-primary text-primary' : 'border-transparent text-content-secondary hover:text-content'"
                      @click="sqlTab = tab.kind">
                {{ $t(tab.labelKey) }}
              </button>
            </div>
            <div v-if="sqlLoading" class="flex h-56 items-center justify-center rounded-mf-md border border-line"><MfSpinner size="sm"/></div>
            <SqlCodeBlock v-else :code="sqlContent" :copyable="true" :placeholder="$t('common.noData')" max-height="max-h-80"/>
            <div class="mt-3 flex items-center justify-center gap-2">
              <MfButton size="sm" :disabled="!sqlContent" @click="onCopySql"><Copy :size="13"/>{{ $t('source.executed.copy') }} SQL</MfButton>
              <MfButton size="sm" :loading="formatting" :disabled="!sqlContent" @click="onFormatSql"><Scan :size="13"/>{{ $t('source.ddl.format') }}</MfButton>
              <MfButton size="sm" :disabled="!sqlContent" @click="onDownloadSql"><Download :size="13"/>{{ $t('source.common.downloadFile') }}</MfButton>
            </div>
          </section>

          <!-- 表选项 -->
          <section class="rounded-mf-lg border border-line bg-surface p-4">
            <h3 class="m-0 mb-3 text-base font-semibold text-content">{{ $t('source.create.optionsCard') }}</h3>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="mb-1 block text-xs text-content-secondary">{{ $t('source.create.engine') }}</label>
                <MfSelect v-model="formState.engine" size="sm" :options="engineOptions"/>
              </div>
              <div>
                <label class="mb-1 block text-xs text-content-secondary">{{ $t('source.structure.charset') }}</label>
                <MfSelect v-model="options.charset" size="sm" :options="charsetOptions"/>
              </div>
              <div>
                <label class="mb-1 block text-xs text-content-secondary">{{ $t('source.common.collation') }}</label>
                <MfSelect v-model="options.collate" size="sm" :options="collateOptions"/>
              </div>
              <div>
                <label class="mb-1 block text-xs text-content-secondary">{{ $t('source.create.rowFormat') }}</label>
                <MfSelect v-model="options.rowFormat" size="sm" :options="rowFormatOptions"/>
              </div>
              <div>
                <label class="mb-1 block text-xs text-content-secondary">{{ $t('source.create.autoIncStart') }}</label>
                <MfInput v-model="options.autoIncrement" size="sm" placeholder="1"/>
              </div>
              <div>
                <label class="mb-1 block text-xs text-content-secondary">{{ $t('source.create.dataDirectory') }}</label>
                <MfInput v-model="options.dataDirectory" size="sm"/>
              </div>
            </div>
            <div class="mt-3">
              <label class="mb-1 block text-xs text-content-secondary">{{ $t('source.common.comment') }}</label>
              <MfTextarea v-model="formState.comment" :rows="3" :maxlength="500"/>
              <div class="mt-1 text-right text-[11px] text-content-tertiary">
                {{ $t('source.structure.commentCounter', { count: (formState.comment || '').length }) }}
              </div>
            </div>
          </section>
        </div>
      </div>
    </MfForm>

    <template #footer>
      <div class="flex w-full items-center justify-end gap-2">
        <MfButton @click="onCancel">{{ $t('common.cancel') }}</MfButton>
        <MfButton variant="primary" :disabled="loading" @click="onNext">
          {{ $t('source.create.nextPreview') }}
          <ArrowRight :size="14"/>
        </MfButton>
      </div>
    </template>

    <!-- 预览确认（第三步） -->
    <MfModal v-model:open="previewVisible" :title="$t('source.create.step3')" width="720px">
      <p class="m-0 mb-3 text-sm text-content-secondary">{{ $t('source.create.step3Desc') }}</p>
      <SqlCodeBlock :code="sqlContent" :copyable="true" :placeholder="$t('common.noData')" max-height="max-h-96"/>
      <div class="mt-4 flex justify-end gap-2">
        <MfButton @click="previewVisible = false">{{ $t('common.cancel') }}</MfButton>
        <MfButton variant="primary" :loading="saving" :disabled="!sqlContent" @click="onSubmit">
          <Check :size="14"/>
          {{ $t('source.create.confirmCreate') }}
        </MfButton>
      </div>
    </MfModal>
  </MfDrawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useSourceManageRoute as useRoute } from '@/composables/useSourceManageRoute'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata'
import FormatService from '@/services/format'
import HttpUtils from '@/utils/http'
import { ObjectUtils } from '@/utils/object'
import {
  ArrowDown, ArrowRight, ArrowUp, Check, Copy, Download, Plus, Scan, Trash2
} from '@lucide/vue'
import SqlCodeBlock from '@/views/components/sql/SqlCodeBlock.vue'
import { debounce } from 'lodash'

defineOptions({ name: 'TableCreate' })

const props = withDefaults(defineProps<{ isVisible?: boolean }>(), { isVisible: false })
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const databaseName = String(route.params.database || '')
const loading = ref(false)
const saving = ref(false)
const engines = ref<any[]>([])
const dataTypes = ref<any[]>([])
const enginesFallback = ['InnoDB', 'MyISAM', 'MEMORY', 'ARCHIVE']
const sourceCode = route.params.source as string
const code = sourceCode

const formState = ref<any>({
  name: '',
  engine: 'InnoDB',
  comment: '',
  columns: []
})

const options = ref({
  charset: 'utf8mb4',
  collate: 'utf8mb4_0900_ai_ci',
  rowFormat: 'DYNAMIC',
  autoIncrement: '',
  dataDirectory: ''
})

const advanced = ref({ addComment: true, viewStructure: true, continueIndex: false })

interface IndexRow { __key: string; kind: 'primary' | 'normal' | 'unique'; name: string; columnsText: string; method: string; comment: string; editing: boolean }
const indexRows = ref<IndexRow[]>([])
const indexTab = ref<'primary' | 'normal' | 'unique' | 'foreign' | 'check'>('primary')
let indexSeq = 0

const selectedRows = ref<Set<number>>(new Set())
const allSelected = computed(() => formState.value.columns.length > 0 && formState.value.columns.every((_column: any, index: number) => selectedRows.value.has(index)))
const partialSelected = computed(() => selectedRows.value.size > 0 && !allSelected.value)

const toggleAll = (checked: boolean) => {
  selectedRows.value = checked
      ? new Set(formState.value.columns.map((_column: any, index: number) => index))
      : new Set()
}

const toggleRow = (index: number, checked: boolean) => {
  const next = new Set(selectedRows.value)
  if (checked) next.add(index)
  else next.delete(index)
  selectedRows.value = next
}

const steps = [
  { titleKey: 'source.create.step1', descKey: 'source.create.step1Desc' },
  { titleKey: 'source.create.step2', descKey: 'source.create.step2Desc' },
  { titleKey: 'source.create.step3', descKey: 'source.create.step3Desc' }
]
const currentStep = computed(() => (previewVisible.value ? 3 : 1))
const previewVisible = ref(false)

const indexTabs = [
  { key: 'primary', labelKey: 'source.create.tabPrimary' },
  { key: 'normal', labelKey: 'source.create.tabNormal' },
  { key: 'unique', labelKey: 'source.create.tabUnique' },
  { key: 'foreign', labelKey: 'source.create.tabForeign' },
  { key: 'check', labelKey: 'source.create.tabCheck' }
] as const

const primaryRows = computed<IndexRow[]>(() => {
  const keys = formState.value.columns.filter((column: any) => column.primaryKey && column.name?.trim())
  if (keys.length === 0) return []
  return [{
    __key: '__primary__', kind: 'primary',
    name: 'PRIMARY', columnsText: keys.map((column: any) => column.name.trim()).join(', '),
    method: 'BTREE', comment: '', editing: false
  }]
})

const autoUniqueRows = computed<IndexRow[]>(() => formState.value.columns
    .filter((column: any) => column.unique && column.name?.trim())
    .map((column: any) => ({
      __key: `__auto_${ column.name.trim() }`, kind: 'unique',
      name: `uk_${ column.name.trim() }`, columnsText: column.name.trim(),
      method: 'BTREE', comment: '', editing: false
    })))

const indexCounts = computed(() => ({
  primary: primaryRows.value.length,
  normal: indexRows.value.filter(row => row.kind === 'normal').length,
  unique: indexRows.value.filter(row => row.kind === 'unique').length + autoUniqueRows.value.length,
  foreign: 0,
  check: 0
}))

const visibleIndexRows = computed<IndexRow[]>(() => {
  if (indexTab.value === 'primary') return [...primaryRows.value, ...indexRows.value.filter(row => row.kind === 'primary')]
  if (indexTab.value === 'unique') return [...autoUniqueRows.value, ...indexRows.value.filter(row => row.kind === 'unique')]
  if (indexTab.value === 'normal') return indexRows.value.filter(row => row.kind === 'normal')
  return []
})

const canAddIndex = computed(() => indexTab.value === 'normal' || indexTab.value === 'unique')

const engineOptions = computed(() => {
  const list = engines.value.length ? engines.value : enginesFallback
  return list.map((engine: any) => ({ label: typeof engine === 'string' ? engine : engine.name || String(engine), value: typeof engine === 'string' ? engine : engine.name || String(engine) }))
})
const dataTypeOptions = computed(() => dataTypes.value.map((dataType: any) => ({ label: dataType, value: dataType })))

const CHARSETS = ['utf8mb4', 'utf8', 'latin1', 'gbk']
const COLLATES = ['utf8mb4_0900_ai_ci', 'utf8mb4_general_ci', 'utf8mb4_bin', 'utf8_general_ci', 'latin1_swedish_ci', 'gbk_chinese_ci']
const ROW_FORMATS = ['DYNAMIC', 'COMPACT', 'REDUNDANT', 'COMPRESSED']
const charsetOptions = CHARSETS.map(value => ({ label: value, value }))
const collateOptions = COLLATES.map(value => ({ label: value, value }))
const rowFormatOptions = ROW_FORMATS.map(value => ({ label: value, value }))
const methodOptions = ['BTREE', 'HASH'].map(value => ({ label: value, value }))

const sqlTabs = [
  { kind: 'full', labelKey: 'source.create.tabFullSql' },
  { kind: 'columns', labelKey: 'source.create.tabColumnsOnly' },
  { kind: 'indexes', labelKey: 'source.create.tabIndexesOnly' },
  { kind: 'options', labelKey: 'source.create.tabOptionsOnly' }
] as const
const sqlTab = ref<'full' | 'columns' | 'indexes' | 'options'>('full')
const sqlContent = ref('')
const sqlLoading = ref(false)
const formatting = ref(false)

const fieldColumns = [
  { title: '', dataIndex: '__check__', width: 44 },
  { title: t('common.id'), dataIndex: '__index__', width: 48 },
  { title: t('source.structure.fieldName'), dataIndex: 'name' },
  { title: t('source.structure.type'), dataIndex: 'type', width: 140 },
  { title: t('source.create.lengthValue'), dataIndex: 'length', width: 90 },
  { title: t('source.common.defaultValue'), dataIndex: 'defaultValue', width: 140 },
  { title: t('source.structure.nullable'), dataIndex: 'nullable', width: 60 },
  { title: t('source.create.primary'), dataIndex: 'primaryKey', width: 60 },
  { title: t('source.create.autoIncCol'), dataIndex: 'autoIncrement', width: 60 },
  { title: t('source.create.uniqueCol'), dataIndex: 'unique', width: 60 },
  { title: t('source.structure.comment'), dataIndex: 'comment', width: 150 },
  { title: t('common.action'), dataIndex: 'action', fixed: 'right' as const }
]

const indexColumns = [
  { title: t('common.id'), dataIndex: '__index__', width: 48 },
  { title: t('common.name'), dataIndex: 'name' },
  { title: t('source.index.indexType'), dataIndex: 'kind', width: 110 },
  { title: t('source.create.indexColumns'), dataIndex: 'columnsText' },
  { title: t('source.create.indexMethod'), dataIndex: 'method', width: 130 },
  { title: t('source.structure.comment'), dataIndex: 'comment', width: 150 },
  { title: t('common.action'), dataIndex: '__action__', fixed: 'right' as const }
]

const buildDefinition = () => ({
  name: formState.value.name?.trim(),
  comment: advanced.value.addComment ? (formState.value.comment || undefined) : undefined,
  engine: formState.value.engine,
  charset: options.value.charset,
  collate: options.value.collate,
  format: options.value.rowFormat,
  autoIncrement: Number(options.value.autoIncrement) || 0,
  dataDirectory: options.value.dataDirectory || undefined,
  columns: formState.value.columns
      .filter((column: any) => column.name?.trim())
      .map((column: any) => ({
        name: column.name.trim(),
        type: column.type,
        length: Number(column.length) > 0 ? Number(column.length) : undefined,
        defaultValue: column.defaultValue || undefined,
        nullable: !!column.nullable,
        primaryKey: !!column.primaryKey,
        autoIncrement: !!column.autoIncrement,
        comment: column.comment || undefined
      })),
  indexes: [
    ...indexRows.value
        .filter(row => (row.kind === 'normal' || row.kind === 'unique') && row.name?.trim() && !row.editing)
        .map(row => ({ name: row.name.trim(), columns: row.columnsText.split(',').map(value => value.trim()).filter(Boolean), unique: row.kind === 'unique', comment: row.comment || undefined })),
    ...formState.value.columns
        .filter((column: any) => column.unique && column.name?.trim())
        .map((column: any) => ({ name: `uk_${ column.name.trim() }`, columns: [column.name.trim()], unique: true }))
  ]
})

const refreshPreview = async () => {
  const definition = buildDefinition()
  if (!definition.name || !sourceCode) {
    sqlContent.value = ''
    return
  }
  sqlLoading.value = true
  try {
    const response = await MetadataService.previewCreateTable(sourceCode, { ...definition, database: databaseName }, sqlTab.value)
    if (response.status && response.data?.isSuccessful !== false) {
      sqlContent.value = String(response.data?.content || '')
    }
    else {
      sqlContent.value = ''
      MfMessage.error(response.data?.message || response.message)
    }
  }
  catch {
    sqlContent.value = ''
  }
  finally {
    sqlLoading.value = false
  }
}

const schedulePreview = debounce(refreshPreview, 500)
watch([formState, options, advanced, sqlTab], () => schedulePreview(), { deep: true })
watch(sqlTab, () => refreshPreview())

const formRef = ref<any | null>(null)

// MfForm 嵌套路径模型：columns.0.name / indexes.0.name 由代理解析到对应数组行
const formModel = new Proxy({} as Record<string, unknown>, {
  get(_target, prop: string) {
    const segments = String(prop).split('.')
    if (segments[0] === 'indexes') {
      const row = indexRows.value[Number(segments[1])]
      return segments.length > 2 ? row?.[segments[2] as keyof IndexRow] : row
    }
    return segments.reduce((obj: any, key) => (obj ? obj[key] : undefined), formState.value)
  }
})

const onAdd = () => {
  formState.value.columns.push({
    name: '', type: dataTypes.value[0] || 'VARCHAR', length: '', defaultValue: '',
    nullable: false, primaryKey: false, autoIncrement: false, unique: false, comment: ''
  })
}

const onRemove = (index: number) => {
  formState.value.columns.splice(index, 1)
  const next = new Set<number>()
  selectedRows.value.forEach(value => { if (value !== index) next.add(value > index ? value - 1 : value) })
  selectedRows.value = next
}

const onMoveColumn = (index: number, direction: -1 | 1) => {
  const target = index + direction
  const list = formState.value.columns
  if (target < 0 || target >= list.length) return
  ;[list[index], list[target]] = [list[target], list[index]]
}

const onMoveSelected = (direction: -1 | 1) => {
  const indexes = [...selectedRows.value].sort((a, b) => a - b)
  if (!indexes.length) return
  const ordered = direction === -1 ? indexes : [...indexes].reverse()
  ordered.forEach(index => onMoveColumn(index, direction))
}

const onBatchDelete = () => {
  if (selectedRows.value.size === 0) {
    MfMessage.warning(t('source.create.selectRows'))
    return
  }
  formState.value.columns = formState.value.columns.filter((_column, index) => !selectedRows.value.has(index))
  selectedRows.value = new Set()
}

const onAddIndex = () => {
  indexSeq += 1
  indexRows.value.push({
    __key: `idx_${ indexSeq }`, kind: indexTab.value === 'unique' ? 'unique' : 'normal',
    name: indexTab.value === 'unique' ? `uk_${ formState.value.name || 'table' }_${ indexSeq }` : `idx_${ formState.value.name || 'table' }_${ indexSeq }`,
    columnsText: '', method: 'BTREE', comment: '', editing: true
  })
}

const onSaveIndexRow = async (row: IndexRow) => {
  try {
    await formRef.value?.validate()
  }
  catch {
    return
  }
  row.editing = false
  MfMessage.success(t('source.create.indexSaved'))
}

const onCancelIndexRow = (row: IndexRow) => {
  if (row.__key.startsWith('idx_')) {
    indexRows.value = indexRows.value.filter(item => item !== row)
  }
  else {
    row.editing = false
  }
}

const onDeleteIndexRow = (row: IndexRow) => {
  indexRows.value = indexRows.value.filter(item => item !== row)
  MfMessage.success(t('source.create.indexRemoved'))
}

const onCopySql = () => {
  ObjectUtils.copy(sqlContent.value)
  MfMessage.success(t('query.tip.copySuccess'))
}

const onFormatSql = () => {
  formatting.value = true
  FormatService.formatSql({ sql: sqlContent.value })
                 .then(response => {
                   if (response.status && typeof response.data === 'string') {
                     sqlContent.value = response.data
                   }
                   else {
                     MfMessage.error(response.message)
                   }
                 })
                 .finally(() => (formatting.value = false))
}

const onDownloadSql = () => {
  const blob = new Blob([sqlContent.value], { type: 'text/sql;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `${ formState.value.name || 'table' }.sql`
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 4000)
}

const onNext = async () => {
  try {
    await formRef.value?.validate()
  }
  catch {
    return
  }
  refreshPreview()
  previewVisible.value = true
}

const onCancel = () => {
  visible.value = false
}

const onSubmit = () => {
  saving.value = true
  const database = route.params.database as string
  const definition = buildDefinition()

  MetadataService.createTable(code, database, definition)
                 .then(response => {
                   if (response.status && response.data && response.data.isSuccessful) {
                     MfMessage.success(t('source.tip.createTableSuccess').replace('$VALUE', String(definition.name)))
                     previewVisible.value = false
                     visible.value = false
                     if (advanced.value.continueIndex) {
                       router.push(`/source/${ code }/d/${ database }/t/indexes/${ definition.name }`)
                     }
                     else if (advanced.value.viewStructure) {
                       router.push(`/source/${ code }/d/${ database }/t/structure/${ definition.name }`)
                     }
                   }
                   else {
                     MfMessage.error(response.data?.message || response.message)
                   }
                 })
                 .finally(() => (saving.value = false))
}

loading.value = true
HttpUtils.all([MetadataService.getEngines(code), MetadataService.getDataTypes(code)])
         .then(HttpUtils.spread((...responses: any[]) => {
           const [enginesResponse, dataTypesResponse] = responses

           if (enginesResponse.status && enginesResponse.data && enginesResponse.data.isSuccessful) {
             engines.value = enginesResponse.data.columns
           }

           if (dataTypesResponse.status && dataTypesResponse.data && dataTypesResponse.data.isSuccessful) {
             dataTypes.value = dataTypesResponse.data.columns
           }
         }))
         .finally(() => (loading.value = false))

onAdd()
</script>
