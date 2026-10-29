<template>
  <div class="relative">
    <MfSkeleton v-if="loading"/>

    <template v-else>
      <!-- 表头：图标 + 表名 + 说明 + 引擎徽标 -->
      <div class="mb-4 flex items-start gap-3">
        <span class="inline-flex size-10 shrink-0 items-center justify-center rounded-mf-lg bg-primary/10 text-primary">
          <Table2 :size="20"/>
        </span>
        <div class="min-w-0 flex-1">
          <div class="flex items-center gap-2">
            <h2 class="m-0 truncate font-mono text-xl font-semibold text-content">{{ tableName }}</h2>
            <button type="button" class="text-content-tertiary transition-colors hover:text-primary"
                    :title="$t('source.common.tableName')" @click="onCopyName">
              <Copy :size="14"/>
            </button>
          </div>
          <p v-if="tableComment" class="mb-0 mt-0.5 text-sm text-content-secondary">{{ tableComment }}</p>
          <div class="mt-1.5 flex items-center gap-1.5">
            <MfTag v-if="tableEngine" size="sm" variant="info">{{ tableEngine }}</MfTag>
            <MfTag v-if="tableFormat" size="sm">{{ tableFormat }}</MfTag>
          </div>
        </div>
        <MfTooltip :content="$t('common.refresh')">
          <MfButton shape="circle" @click="handleInitialize">
            <RefreshCw :size="14"/>
          </MfButton>
        </MfTooltip>
      </div>

      <!-- 工具栏 -->
      <div class="mb-3 flex flex-wrap items-center gap-2">
        <MfButton variant="primary" @click="openPanel('add', null)">
          <Plus :size="14"/>
          {{ $t('source.structure.addField') }}
        </MfButton>
        <MfButton @click="onInsertField">
          <ListPlus :size="14"/>
          {{ $t('source.structure.insertField') }}
        </MfButton>
        <MfButton :disabled="selectedNames.size === 0" @click="dropVisible = true">
          <Trash2 :size="14"/>
          {{ $t('source.structure.dropTitle') }}
        </MfButton>
        <MfButton :disabled="!detailColumn" @click="onMove(-1)">
          <ArrowUp :size="14"/>
          {{ $t('source.structure.moveUp') }}
        </MfButton>
        <MfButton :disabled="!detailColumn" @click="onMove(1)">
          <ArrowDown :size="14"/>
          {{ $t('source.structure.moveDown') }}
        </MfButton>
      </div>

      <!-- 字段表 -->
      <MfTable :columns="fieldHeaders"
               :data-source="columns"
               row-key="name"
               empty-text=" ">
        <template #header-__check__>
          <MfCheckbox :model-value="allSelected" :indeterminate="partialSelected"
                      @update:model-value="(checked: any) => toggleAll(!!checked)"/>
        </template>
        <template #cell-__check__="{ record }">
          <MfCheckbox :model-value="selectedNames.has(record.name)"
                      @update:model-value="(checked: any) => toggleSelect(record.name, !!checked)"/>
        </template>
        <template #cell-name="{ record }">
          <button type="button" class="inline-flex items-center gap-1 font-mono text-content transition-colors hover:text-primary"
                  @click="detailName = record.name">
            {{ record.name }}
            <KeyRound v-if="primaryNames.has(record.name)" :size="12" class="text-warning"/>
          </button>
        </template>
        <template #cell-type="{ record }">
          <MfTag size="sm" :variant="typeTone(record.type)">{{ record.type }}</MfTag>
        </template>
        <template #cell-length="{ record }">
          {{ record.length && record.length > 0 ? record.length : '-' }}
        </template>
        <template #cell-nullable="{ record }">
          <span :class="record.nullable === 'NO' ? 'text-danger' : 'text-content'">
            {{ record.nullable === 'NO' ? $t('common.no') : $t('common.yes') }}
          </span>
        </template>
        <template #cell-defaultValue="{ record }">
          <span v-if="record.default" class="font-mono text-xs">{{ record.default }}</span>
          <span v-else class="text-content-tertiary">-</span>
        </template>
        <template #cell-comment="{ record }">
          <EllipsisCell :text="record.comment"/>
        </template>
        <template #cell-action="{ record }">
          <div class="flex items-center gap-2 text-xs">
            <button type="button" class="text-primary hover:underline" @click="openPanel('edit', record)">
              {{ $t('common.edit') }}
            </button>
            <button type="button" class="text-danger hover:underline" @click="confirmDrop([record.name])">
              {{ $t('common.delete') }}
            </button>
          </div>
        </template>
      </MfTable>

      <!-- 字段详情卡 -->
      <div v-if="detailColumn" class="mt-4 rounded-mf-lg border border-line bg-surface">
        <div class="flex items-center justify-between border-b border-line px-4 py-3">
          <div class="flex items-center gap-2">
            <span class="inline-flex size-7 items-center justify-center rounded-mf-md bg-primary/10 text-primary">
              <Columns3 :size="15"/>
            </span>
            <MfTag size="sm" :variant="typeTone(detailColumn.type)">{{ detailColumn.type }}</MfTag>
            <span class="font-mono text-sm text-content">{{ detailColumn.name }}</span>
          </div>
          <MfButton size="sm" @click="openPanel('edit', detailColumn)">
            <Pencil :size="13"/>
            {{ $t('common.edit') }}
          </MfButton>
        </div>
        <div class="px-4 py-3">
          <div class="mb-2 text-xs font-medium text-content-tertiary">{{ $t('source.common.info') }}</div>
          <div class="grid grid-cols-2 gap-x-8 gap-y-2 text-sm">
            <div class="flex justify-between gap-4 border-b border-line py-1">
              <span class="text-content-tertiary">{{ $t('source.structure.fieldName') }}</span>
              <span class="min-w-0 truncate font-mono text-content">{{ detailColumn.name }}</span>
            </div>
            <div class="flex justify-between gap-4 border-b border-line py-1">
              <span class="text-content-tertiary">{{ $t('source.structure.comment') }}</span>
              <span class="min-w-0 truncate text-content">{{ detailColumn.comment || '-' }}</span>
            </div>
            <div class="flex justify-between gap-4 border-b border-line py-1">
              <span class="text-content-tertiary">{{ $t('source.structure.type') }}</span>
              <span class="font-mono text-content">{{ detailColumn.type }}</span>
            </div>
            <div class="flex justify-between gap-4 border-b border-line py-1">
              <span class="text-content-tertiary">{{ $t('source.structure.nullable') }}</span>
              <span :class="detailColumn.nullable === 'NO' ? 'text-danger' : 'text-content'">
                {{ detailColumn.nullable === 'NO' ? $t('common.no') : $t('common.yes') }}
              </span>
            </div>
            <div class="flex justify-between gap-4 border-b border-line py-1">
              <span class="text-content-tertiary">{{ $t('source.structure.lengthPrecision') }}</span>
              <span class="text-content">{{ detailColumn.length && detailColumn.length > 0 ? detailColumn.length : '-' }}</span>
            </div>
            <div class="flex justify-between gap-4 border-b border-line py-1">
              <span class="text-content-tertiary">{{ $t('source.common.defaultValue') }}</span>
              <span class="min-w-0 truncate text-content">
                {{ detailColumn.autoincrement === 'YES' ? $t('source.common.autoIncrement') : (detailColumn.default || '-') }}
              </span>
            </div>
            <div class="flex justify-between gap-4 border-b border-line py-1">
              <span class="text-content-tertiary">{{ $t('common.position') }}</span>
              <span class="text-content">{{ detailColumn.position }}</span>
            </div>
            <div class="flex justify-between gap-4 border-b border-line py-1">
              <span class="text-content-tertiary">{{ $t('source.common.columnPrimaryKey') }}</span>
              <span class="text-content">{{ primaryNames.has(detailColumn.name) ? $t('common.yes') : $t('common.no') }}</span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 右侧添加/编辑字段面板：点击添加/编辑后才出现 -->
    <Transition enter-active-class="transition-transform duration-(--mf-duration-base)"
                enter-from-class="translate-x-full"
                leave-active-class="transition-transform duration-(--mf-duration-base)"
                leave-to-class="translate-x-full">
      <aside v-if="panelVisible"
             class="fixed bottom-0 right-0 top-16 z-30 flex w-[380px] flex-col border-l border-line bg-surface shadow-mf-lg">
        <div class="flex shrink-0 items-center justify-between border-b border-line px-4 py-3">
          <h3 class="m-0 text-base font-semibold text-content">
            {{ panelMode === 'edit' ? $t('source.structure.editField') : $t('source.structure.addField') }}
          </h3>
          <button type="button" class="text-content-tertiary transition-colors hover:text-content" @click="closePanel">
            <X :size="16"/>
          </button>
        </div>

        <div class="min-h-0 flex-1 overflow-y-auto px-4 py-4">
          <div v-if="panelMode === 'insert' && panelAnchor"
               class="mb-3 rounded-mf-md border border-primary/20 bg-primary/5 px-3 py-2 text-xs text-content-secondary">
            {{ $t('source.structure.insertAfter').replace('$VALUE', panelAnchor) }}
          </div>

          <div class="space-y-4">
            <div>
              <label class="mb-1 block text-sm font-medium text-content-secondary">
                {{ $t('source.structure.fieldName') }} <span class="text-danger">*</span>
              </label>
              <MfInput v-model="panelForm.name" :disabled="panelMode === 'edit'"/>
            </div>

            <div>
              <label class="mb-1 block text-sm font-medium text-content-secondary">
                {{ $t('source.structure.type') }} <span class="text-danger">*</span>
              </label>
              <MfSelect v-model="panelForm.type" :options="typeOptions"/>
            </div>

            <div>
              <label class="mb-1 block text-sm font-medium text-content-secondary">{{ $t('source.structure.lengthPrecision') }}</label>
              <MfInput v-model="panelForm.length" placeholder="0"/>
            </div>

            <div class="grid grid-cols-2 gap-3">
              <MfCheckbox v-model="panelForm.nullable">{{ $t('source.structure.nullable') }}</MfCheckbox>
              <MfCheckbox v-model="panelForm.autoIncrement">{{ $t('source.common.autoIncrement') }}</MfCheckbox>
            </div>

            <div>
              <label class="mb-1 block text-sm font-medium text-content-secondary">{{ $t('source.common.defaultValue') }}</label>
              <div class="flex gap-2">
                <MfSelect v-model="panelForm.defaultMode" class="w-32 shrink-0" :options="defaultModeOptions"/>
                <MfInput v-model="panelForm.defaultValue" class="min-w-0 flex-1" :disabled="panelForm.defaultMode !== 'constant'"/>
              </div>
            </div>

            <div>
              <label class="mb-1 block text-sm font-medium text-content-secondary">{{ $t('source.structure.comment') }}</label>
              <MfTextarea v-model="panelForm.comment" :rows="3" :maxlength="500"/>
              <div class="mt-1 text-right text-[11px] text-content-tertiary">
                {{ $t('source.structure.commentCounter', { count: (panelForm.comment || '').length }) }}
              </div>
            </div>

            <!-- 更多选项 -->
            <div class="rounded-mf-md border border-line">
              <button type="button" class="flex w-full items-center gap-1.5 px-3 py-2 text-sm font-medium text-content"
                      @click="moreOptionsOpen = !moreOptionsOpen">
                <ChevronDown :size="13" :class="['text-content-tertiary transition-transform duration-200', moreOptionsOpen && 'rotate-180']"/>
                {{ $t('source.structure.moreOptions') }}
              </button>
              <div v-if="moreOptionsOpen" class="space-y-3 border-t border-line px-3 py-3">
                <div>
                  <label class="mb-1 block text-xs text-content-secondary">{{ $t('source.structure.charset') }}</label>
                  <MfSelect v-model="panelForm.charset" :options="charsetOptions" clearable/>
                </div>
                <div>
                  <label class="mb-1 block text-xs text-content-secondary">{{ $t('source.common.collation') }}</label>
                  <MfSelect v-model="panelForm.collate" :options="collateOptions" clearable/>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="flex shrink-0 justify-end gap-2 border-t border-line px-4 py-3">
          <MfButton @click="closePanel">{{ $t('common.cancel') }}</MfButton>
          <MfButton variant="primary" :loading="saving" @click="onSubmitPanel">
            {{ $t('common.confirm') }}
          </MfButton>
        </div>
      </aside>
    </Transition>

    <!-- 删除字段确认 -->
    <MfModal v-model:open="dropVisible" :title="$t('source.structure.dropTitle')" width="440px">
      <MfSpace direction="vertical" fill :style="{ width: '100%' }">
        <MfAlert variant="danger" :title="$t('source.structure.dropAlert1')"/>
        <MfAlert :title="$t('source.common.dropColumnInfo').replace('$VALUE', [...selectedNames].join(', '))"/>
      </MfSpace>
      <div class="mt-4 flex justify-end">
        <MfSpace>
          <MfButton @click="dropVisible = false">{{ $t('common.cancel') }}</MfButton>
          <MfButton variant="danger" :loading="saving" @click="onDropFields">{{ $t('common.delete') }}</MfButton>
        </MfSpace>
      </div>
    </MfModal>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { MfMessage } from 'metaforge-ui'
import {
  ArrowDown, ArrowUp, ChevronDown, Columns3, Copy, KeyRound, ListPlus, Pencil,
  Plus, RefreshCw, Table2, Trash2, X
} from '@lucide/vue'
import MetadataService from '@/services/metadata'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import { ObjectUtils } from '@/utils/object'

defineOptions({ name: 'SourceTableStructure' })

const { t } = useI18n()
const route = useRoute()

const DATA_TYPES = ['TINYINT', 'SMALLINT', 'INT', 'BIGINT', 'FLOAT', 'DOUBLE', 'DECIMAL',
  'CHAR', 'VARCHAR', 'TEXT', 'LONGTEXT', 'DATE', 'TIME', 'DATETIME', 'TIMESTAMP', 'BLOB', 'LONGBLOB']
const CHARSETS = ['utf8mb4', 'utf8', 'latin1', 'gbk']
const COLLATES = ['utf8mb4_general_ci', 'utf8mb4_unicode_ci', 'utf8mb4_bin', 'utf8_general_ci', 'latin1_swedish_ci', 'gbk_chinese_ci']

const loading = ref(false)
const tableName = ref(String(route.params.table || ''))
const tableComment = ref('')
const tableEngine = ref('')
const tableFormat = ref('')
const columns = ref<any[]>([])
const primaryNames = ref<Set<string>>(new Set())
const selectedNames = ref<Set<string>>(new Set())
const detailName = ref<string | null>(null)

const panelVisible = ref(false)
const panelMode = ref<'add' | 'edit' | 'insert'>('add')
const panelAnchor = ref<string | null>(null)
const saving = ref(false)
const moreOptionsOpen = ref(false)
const dropVisible = ref(false)

const panelForm = ref({
  name: '',
  type: 'VARCHAR',
  length: '' as string | number,
  nullable: true,
  autoIncrement: false,
  defaultMode: 'none' as 'none' | 'constant' | 'timestamp',
  defaultValue: '',
  comment: '',
  charset: '',
  collate: ''
})

const typeOptions = computed(() => {
  const options = DATA_TYPES.map(value => ({ label: value, value }))
  // JDBC 类型可能超出枚举（JSON、POINT 等）：编辑时保留原值可选
  const current = panelForm.value.type
  if (current && !DATA_TYPES.includes(current)) {
    options.unshift({ label: current, value: current })
  }
  return options
})
const defaultModeOptions = computed(() => [
  { label: t('source.structure.defaultNone'), value: 'none' },
  { label: t('source.structure.defaultConstant'), value: 'constant' },
  { label: 'CURRENT_TIMESTAMP', value: 'timestamp' }
])
const charsetOptions = computed(() => CHARSETS.map(value => ({ label: value, value })))
const collateOptions = computed(() => COLLATES.map(value => ({ label: value, value })))

const allSelected = computed(() => columns.value.length > 0 && columns.value.every(item => selectedNames.value.has(item.name)))
const partialSelected = computed(() => selectedNames.value.size > 0 && !allSelected.value)
const detailColumn = computed(() => columns.value.find(item => item.name === detailName.value) || null)

const fieldHeaders = computed(() => [
  { title: '', key: '__check__', width: 40 },
  { title: '#', key: 'position', width: 50 },
  { title: t('source.structure.fieldName'), key: 'name' },
  { title: t('source.structure.type'), key: 'type', width: 120 },
  { title: t('source.structure.lengthPrecision'), key: 'length', width: 100 },
  { title: t('source.structure.nullable'), key: 'nullable', width: 80 },
  { title: t('source.common.defaultValue'), key: 'defaultValue', width: 160 },
  { title: t('source.structure.comment'), key: 'comment', width: 240, ellipsis: true },
  { title: t('common.action'), key: 'action', width: 110 }
])

const typeTone = (type: string): 'info' | 'success' | 'warning' | 'primary' | 'default' => {
  const value = (type || '').toUpperCase()
  if (/TINYINT|SMALLINT|INT|BIGINT|FLOAT|DOUBLE|DECIMAL|NUMERIC|DATE|TIME|DATETIME|TIMESTAMP|YEAR/.test(value)) return 'info'
  if (/CHAR|ENUM|SET/.test(value)) return 'success'
  if (/TEXT/.test(value)) return 'warning'
  if (/JSON/.test(value)) return 'primary'
  return 'default'
}

const handleInitialize = () => {
  const code = route.params.source
  const database = route.params.database
  const table = route.params.table
  tableName.value = String(table || '')
  if (!(code && database && table)) return

  loading.value = true
  Promise.all([
    MetadataService.getTable(code as string, database as string, table as string),
    MetadataService.getColumnsByTable(code as string, database as string, table as string)
  ])
      .then(([tableResponse, columnResponse]) => {
        if (tableResponse.status && tableResponse.data?.isSuccessful) {
          const info = tableResponse.data.columns?.[0] || null
          tableComment.value = info?.object_comment || ''
          tableEngine.value = info?.object_engine || ''
          tableFormat.value = info?.object_format || ''
        }
        if (columnResponse.status && columnResponse.data?.isSuccessful) {
          const rows = columnResponse.data.columns || []
          primaryNames.value = new Set(rows.filter((row: any) => row.type_name === 'primary').map((row: any) => row.object_name))
          columns.value = rows
              .filter((row: any) => row.type_name === 'column')
              .map((row: any) => ({
                name: row.object_name,
                type: row.object_data_type,
                nullable: row.object_nullable,
                default: row.object_default_value || '',
                comment: row.object_comment || '',
                position: Number(row.object_position) || 0,
                length: Number(row.object_length) || 0,
                autoincrement: row.object_autoincrement || 'NO'
              }))
          if (!columns.value.some(item => item.name === detailName.value)) {
            detailName.value = columns.value[0]?.name || null
          }
          selectedNames.value = new Set([...selectedNames.value].filter(name => columns.value.some(item => item.name === name)))
        }
        else {
          MfMessage.error(columnResponse.message)
        }
      })
      .finally(() => (loading.value = false))
}

const onCopyName = () => {
  ObjectUtils.copy(tableName.value)
  MfMessage.success(t('query.tip.copySuccess'))
}

const toggleAll = (checked: boolean) => {
  selectedNames.value = checked ? new Set(columns.value.map(item => item.name)) : new Set()
}

const toggleSelect = (name: string, checked: boolean) => {
  const next = new Set(selectedNames.value)
  if (checked) next.add(name)
  else next.delete(name)
  selectedNames.value = next
}

const openPanel = (mode: 'add' | 'edit' | 'insert', row: any) => {
  panelMode.value = mode
  panelAnchor.value = mode === 'insert' ? String(row?.name || detailColumn.value?.name || '') : null
  if (mode === 'edit' && row) {
    panelForm.value = {
      name: row.name,
      type: row.type,
      length: row.length > 0 ? row.length : '',
      nullable: row.nullable !== 'NO',
      autoIncrement: row.autoincrement === 'YES',
      defaultMode: row.default && row.default.toUpperCase() === 'CURRENT_TIMESTAMP' ? 'timestamp'
          : row.default ? 'constant' : 'none',
      defaultValue: row.default && row.default.toUpperCase() !== 'CURRENT_TIMESTAMP' ? row.default : '',
      comment: row.comment || '',
      charset: '',
      collate: ''
    }
  }
  else {
    panelForm.value = {
      name: '', type: 'VARCHAR', length: '', nullable: true, autoIncrement: false,
      defaultMode: 'none', defaultValue: '', comment: '', charset: '', collate: ''
    }
  }
  moreOptionsOpen.value = false
  panelVisible.value = true
}

const closePanel = () => {
  panelVisible.value = false
}

const onInsertField = () => {
  const anchor = detailColumn.value?.name || columns.value[columns.value.length - 1]?.name
  if (!anchor) {
    MfMessage.warning(t('source.structure.selectFirst'))
    return
  }
  openPanel('insert', { name: anchor })
}

const onSubmitPanel = () => {
  const form = panelForm.value
  if (!form.name.trim()) {
    MfMessage.error(t('source.common.columnName'))
    return
  }
  saving.value = true
  const code = route.params.source as string
  const database = route.params.database as string
  const table = route.params.table as string
  const column: Record<string, unknown> = {
    name: form.name.trim(),
    type: form.type,
    length: form.length !== '' && Number(form.length) > 0 ? Number(form.length) : undefined,
    nullable: form.nullable,
    autoIncrement: form.autoIncrement,
    defaultValue: form.defaultMode === 'constant' && form.defaultValue !== '' ? form.defaultValue
        : form.defaultMode === 'timestamp' ? 'CURRENT_TIMESTAMP' : undefined,
    comment: form.comment || undefined,
    charset: form.charset || undefined,
    collate: form.collate || undefined
  }
  if (panelMode.value === 'insert' && panelAnchor.value) {
    column.after = panelAnchor.value
  }
  const payload = { name: table, columns: [column] }
  const request = panelMode.value === 'edit'
      ? MetadataService.changeColumn(code, database, table, payload)
      : MetadataService.createColumn(code, database, table, payload)
  request
      .then(response => {
        if (response.status) {
          MfMessage.success(t('source.structure.saveSuccess'))
          closePanel()
          handleInitialize()
        }
        else {
          MfMessage.error(response.message)
        }
      })
      .finally(() => (saving.value = false))
}

const confirmDrop = (names: string[]) => {
  selectedNames.value = new Set(names)
  dropVisible.value = true
}

const onDropFields = () => {
  if (selectedNames.value.size === 0) return
  saving.value = true
  const code = route.params.source as string
  const database = route.params.database as string
  const table = route.params.table as string
  MetadataService.dropColumn(code, database, table, {
    name: table,
    columns: [...selectedNames.value].map(name => ({ name }))
  })
                 .then(response => {
                   if (response.status) {
                     MfMessage.success(t('source.structure.dropSuccess'))
                     dropVisible.value = false
                     selectedNames.value = new Set()
                     handleInitialize()
                   }
                   else {
                     MfMessage.error(response.message)
                   }
                 })
                 .finally(() => (saving.value = false))
}

const onMove = (direction: -1 | 1) => {
  const column = detailColumn.value
  if (!column) {
    MfMessage.warning(t('source.structure.selectFirst'))
    return
  }
  const index = columns.value.findIndex(item => item.name === column.name)
  const targetIndex = index + direction
  if (targetIndex < 0 || targetIndex >= columns.value.length) return

  const payload: Record<string, unknown> = {
    name: column.name,
    type: column.type,
    length: column.length > 0 ? column.length : undefined,
    nullable: column.nullable !== 'NO',
    autoIncrement: column.autoincrement === 'YES',
    defaultValue: column.default || undefined,
    comment: column.comment || undefined
  }
  if (direction === -1) {
    // 上移：到首位用 FIRST，否则排在原前前列之后
    if (targetIndex === 0) {
      payload.first = true
    }
    else {
      payload.after = columns.value[targetIndex - 1].name
    }
  }
  else {
    payload.after = columns.value[targetIndex].name
  }

  saving.value = true
  const code = route.params.source as string
  const database = route.params.database as string
  const table = route.params.table as string
  MetadataService.changeColumn(code, database, table, { name: table, columns: [payload] })
                 .then(response => {
                   if (response.status) {
                     handleInitialize()
                   }
                   else {
                     MfMessage.error(response.message)
                   }
                 })
                 .finally(() => (saving.value = false))
}

watch(() => route.params.table, () => handleInitialize())

onMounted(() => handleInitialize())
</script>
