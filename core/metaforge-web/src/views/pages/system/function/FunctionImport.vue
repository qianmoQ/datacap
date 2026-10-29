<template>
  <MfDrawer v-model:open="visible" :title="$t('function.common.import')" width="56%">
    <template #default>
      <p class="mb-3 text-xs text-content-tertiary">{{ $t('function.import.subtitle') }}</p>

      <!-- 步骤条 -->
      <div class="mb-3 flex items-center gap-2">
        <template v-for="(item, index) in steps" :key="item.key">
          <div class="flex items-center gap-2">
            <span class="inline-flex size-6 shrink-0 items-center justify-center rounded-mf-full text-xs font-medium"
                  :class="step > index + 1 ? 'bg-primary text-on-primary' : step === index + 1 ? 'bg-primary text-on-primary' : 'border border-line bg-surface text-content-tertiary'">
              <Check v-if="step > index + 1" :size="13"/>
              <template v-else>{{ index + 1 }}</template>
            </span>
            <div class="leading-tight">
              <div class="text-xs font-medium" :class="step >= index + 1 ? 'text-content' : 'text-content-tertiary'">{{ $t(item.title) }}</div>
              <div class="text-[11px] text-content-tertiary">{{ $t(item.desc) }}</div>
            </div>
          </div>
          <span v-if="index < steps.length - 1" class="h-px w-6 bg-line"/>
        </template>
      </div>

      <!-- 第一步：选择数据源 -->
      <div v-if="step === 1" class="space-y-3">
        <MfTabs v-model:active-key="source">
          <MfTabPane :item-key="SOURCES.paste" :tab="$t('function.import.sourcePaste')">
            <MfFormItem :label="$t('common.content')">
              <AceEditor :value="pasteText" @update:value="pasteText = $event"/>
            </MfFormItem>
            <p class="text-xs text-content-tertiary">{{ $t('function.import.pasteTip') }}</p>
          </MfTabPane>

          <MfTabPane :item-key="SOURCES.url" :tab="$t('function.import.sourceUrl')">
            <div class="space-y-2">
              <div class="flex gap-2">
                <MfInput v-model="url" :placeholder="'https://example.com/functions.json'" class="flex-1"/>
                <MfButton :loading="fetching" @click="onFetchUrl">
                  <CloudDownload :size="14"/>
                  {{ $t('function.import.urlFetch') }}
                </MfButton>
              </div>
              <p class="text-xs text-content-tertiary">{{ $t('function.import.urlTip') }}</p>
              <p v-if="urlFetchedTip" class="text-xs text-primary">{{ urlFetchedTip }}</p>
            </div>
          </MfTabPane>

          <MfTabPane :item-key="SOURCES.file" :tab="$t('function.import.sourceFile')">
            <div class="space-y-2">
              <button type="button"
                      class="w-full rounded-mf-md border border-dashed border-line px-4 py-5 text-center transition-colors hover:border-primary hover:bg-primary/5"
                      @click="fileInput?.click()">
                <Upload :size="18" class="mx-auto mb-1 text-content-tertiary"/>
                <span class="block text-sm text-content">{{ $t('function.import.fileTip') }}</span>
              </button>
              <input ref="fileInput" type="file" accept=".json,.csv,.tsv,.txt,.md,.markdown" class="hidden" @change="onPickFile"/>
              <p v-if="fileName" class="text-xs text-content-secondary">{{ fileLoadedTip }}</p>
            </div>
          </MfTabPane>
        </MfTabs>
      </div>

      <!-- 第二步：配置导入 -->
      <div v-else-if="step === 2" class="space-y-3">
        <div class="grid grid-cols-2 gap-3">
          <MfFormItem :label="$t('function.import.targetPlugin')" required-mark>
            <MfSelect v-model="plugin" multiple :options="plugins" :placeholder="$t('function.tip.selectPluginHolder')"/>
            <p class="mt-1 text-[11px] text-content-tertiary">{{ $t('function.import.targetPluginDesc') }}</p>
          </MfFormItem>
          <MfFormItem :label="$t('function.import.dataType')" required-mark>
            <MfSelect v-model="type" :options="typeOptions"/>
          </MfFormItem>
        </div>

        <div>
          <div class="mb-1 flex items-baseline justify-between">
            <span class="text-sm font-medium text-content">{{ $t('function.import.mapping') }}</span>
            <span class="text-[11px] text-content-tertiary">{{ $t('function.import.mappingDesc') }}</span>
          </div>
          <div class="rounded-mf-md border border-line">
            <div v-for="field in mappingFields" :key="field.key"
                 class="grid grid-cols-[160px_1fr] items-center gap-3 border-b border-line px-3 py-1.5 last:border-b-0">
              <span class="text-xs text-content-secondary">
                {{ $t(`function.field.${ field.key }`) }}<span v-if="field.required" class="ml-0.5 text-danger">*</span>
              </span>
              <MfSelect v-model="mapping[field.key]" size="sm" :options="columnOptions" clearable/>
            </div>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-3">
          <MfFormItem :label="$t('function.import.duplicate')">
            <MfSelect v-model="duplicate" :options="duplicateOptions"/>
          </MfFormItem>
          <div class="flex items-end pb-1">
            <MfCheckbox v-model="keepRest">{{ $t('function.import.keepRest') }}</MfCheckbox>
          </div>
        </div>
      </div>

      <!-- 第三步：确认导入 -->
      <div v-else class="space-y-3">
        <div class="flex items-baseline gap-2">
          <span class="text-sm font-medium text-content">{{ $t('function.import.preview') }}</span>
          <span class="text-[11px] text-content-tertiary">{{ $t('function.import.previewDesc') }}</span>
        </div>

        <div class="flex gap-2">
          <div v-for="stat in stats" :key="stat.label" class="min-w-20 flex-1 rounded-mf-md border border-line px-3 py-1.5">
            <div class="text-[11px] text-content-tertiary">{{ stat.label }}</div>
            <div class="text-base font-semibold" :class="stat.tone">{{ stat.value }}</div>
          </div>
        </div>

        <div class="flex items-center justify-end gap-2">
          <MfButton size="sm" @click="toggleAll(expanded.size === validEntries.length)">
            <ChevronsUpDown :size="13"/>
            {{ expanded.size === validEntries.length ? $t('function.import.collapseAll') : $t('function.import.expandAll') }}
          </MfButton>
        </div>

        <div class="divide-y divide-line rounded-mf-md border border-line">
          <div v-for="(entry, index) in previewEntries" :key="index">
            <button type="button"
                    class="flex w-full items-center gap-2 px-3 py-1.5 text-left transition-colors hover:bg-surface-hover"
                    @click="toggleExpand(index)">
              <ChevronRight :size="13" :class="['shrink-0 text-content-tertiary transition-transform duration-200', expanded.has(index) && 'rotate-90']"/>
              <span class="min-w-0 flex-1 truncate font-mono text-xs text-content">{{ entry.name || $t('function.import.invalidReason') }}</span>
              <MfTag v-if="entry.type" size="sm">{{ $t('function.common.' + String(entry.type).toLowerCase()) }}</MfTag>
              <span v-if="!entry.name" class="shrink-0 text-xs text-danger">{{ $t('function.import.invalidReason') }}</span>
            </button>
            <div v-if="expanded.has(index)" class="space-y-1.5 border-t border-line bg-surface-hover/40 px-3 py-2 pl-8">
              <template v-for="(value, key) in entry" :key="key">
                <div v-if="value" class="grid grid-cols-[110px_1fr] items-start gap-2">
                  <span class="text-[11px] leading-5 text-content-tertiary">{{ attributeLabel(String(key)) }}</span>
                  <pre class="m-0 overflow-x-auto whitespace-pre-wrap font-mono text-xs leading-5 text-content">{{ value }}</pre>
                </div>
              </template>
            </div>
          </div>
        </div>
        <p v-if="previewEntries.length > 10" class="text-[11px] text-content-tertiary">
          {{ $t('dataTable.total', { count: previewEntries.length }) }}
        </p>
      </div>
    </template>

    <template #footer>
      <MfSpace>
        <MfButton v-if="step > 1" @click="step--">{{ $t('function.import.prev') }}</MfButton>
        <MfButton @click="visible = false">{{ $t('common.cancel') }}</MfButton>
        <MfButton v-if="step < 3" variant="primary" @click="onNext">{{ $t('common.next') }}</MfButton>
        <MfButton v-else variant="primary" :loading="importing" @click="onImport">{{ $t('function.import.beginImport') }}</MfButton>
      </MfSpace>
    </template>
  </MfDrawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { Check, ChevronRight, ChevronsUpDown, CloudDownload, Upload } from '@lucide/vue'
import {
  FunctionMappingField,
  FunctionSourceDocument,
  FunctionType
} from '@/model/function'
import {
  guessColumn,
  mapDocumentEntries,
  parseFunctionDocument
} from './importParse'
import FunctionService from '@/services/function'
import PluginService from '@/services/plugin'
import AceEditor from '@/views/components/editor/AceEditor.vue'

defineOptions({ name: 'FunctionImport' })

const props = withDefaults(defineProps<{ isVisible?: boolean }>(), {
  isVisible: false
})
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const { t } = useI18n()

const SOURCES = { paste: 'paste', url: 'url', file: 'file' } as const

const steps = [
  { key: 'source', title: 'function.import.step1', desc: 'function.import.step1Desc' },
  { key: 'configure', title: 'function.import.step2', desc: 'function.import.step2Desc' },
  { key: 'confirm', title: 'function.import.step3', desc: 'function.import.step3Desc' }
]

// Dynamic system fields per data type; plugins may contribute extra attributes through the keep-rest switch.
const MAPPING_FIELDS: Record<string, FunctionMappingField[]> = {
  [FunctionType.KEYWORD]: [
    { key: 'name', required: true },
    { key: 'description' },
    { key: 'syntax' },
    { key: 'examples' },
    { key: 'notes' },
    { key: 'version' }
  ],
  [FunctionType.OPERATOR]: [
    { key: 'name', required: true },
    { key: 'description' },
    { key: 'syntax' },
    { key: 'examples' },
    { key: 'parameters' },
    { key: 'return_type' },
    { key: 'notes' },
    { key: 'version' }
  ],
  [FunctionType.FUNCTION]: [
    { key: 'name', required: true },
    { key: 'category' },
    { key: 'description' },
    { key: 'syntax' },
    { key: 'examples' },
    { key: 'parameters' },
    { key: 'return_type' },
    { key: 'notes' },
    { key: 'version' }
  ]
}

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const step = ref(1)
const source = ref<string>(SOURCES.paste)
const pasteText = ref('')
const url = ref('')
const urlText = ref('')
const fetching = ref(false)
const urlFetchedTip = ref('')
const fileName = ref('')
const fileSize = ref(0)
const fileText = ref('')
const fileInput = ref<HTMLInputElement>()

const plugin = ref<string[]>([])
const type = ref<string>(FunctionType.FUNCTION)
const duplicate = ref<'skip' | 'overwrite'>('skip')
const keepRest = ref(true)
const mapping = ref<Record<string, string | undefined>>({})
const document = ref<FunctionSourceDocument>({ columns: [], rows: [] })

const plugins = ref<{ label: string; value: string }[]>([])
const importing = ref(false)
const expanded = ref<Set<number>>(new Set())

const typeOptions = computed(() => [
  { label: t('function.common.function'), value: FunctionType.FUNCTION },
  { label: t('function.common.keyword'), value: FunctionType.KEYWORD },
  { label: t('function.common.operator'), value: FunctionType.OPERATOR }
])
const duplicateOptions = computed(() => [
  { label: t('function.import.duplicateSkip'), value: 'skip' },
  { label: t('function.import.duplicateOverwrite'), value: 'overwrite' }
])
const mappingFields = computed(() => MAPPING_FIELDS[type.value] || MAPPING_FIELDS[FunctionType.FUNCTION])
const columnOptions = computed(() => document.value.columns.map(column => ({ label: column, value: column })))

const fileLoadedTip = computed(() => t('function.import.fileLoaded')
  .replace('{name}', fileName.value)
  .replace('{size}', formatSize(fileSize.value)))

const activeText = computed(() => {
  if (source.value === SOURCES.paste) return pasteText.value
  if (source.value === SOURCES.url) return urlText.value
  return fileText.value
})

const entries = computed<Record<string, string>[]>(() => {
  if (!document.value.rows.length) return []
  const mapped = mapDocumentEntries(document.value, mapping.value)
  if (!keepRest.value) return mapped
  const mappedColumns = new Set(Object.values(mapping.value).filter(Boolean) as string[])
  const rest = document.value.columns.filter(column => !mappedColumns.has(column))
  if (!rest.length) return mapped
  return document.value.rows.map((row, index) => {
    const entry = { ...mapped[index] }
    rest.forEach(column => {
      const value = row[document.value.columns.indexOf(column)]
      if (value && String(value).trim() !== '') entry[column] = String(value).trim()
    })
    return entry
  })
})

const validEntries = computed(() => entries.value.filter(entry => !!entry.name))
const previewEntries = computed(() => entries.value.slice(0, 10))
const stats = computed(() => [
  { label: t('function.import.total'), value: String(entries.value.length), tone: 'text-content' },
  { label: t('function.import.valid'), value: String(validEntries.value.length), tone: 'text-success' },
  { label: t('function.import.invalid'), value: String(entries.value.length - validEntries.value.length), tone: 'text-danger' },
  { label: t('function.import.targetPlugin'), value: String(plugin.value.length), tone: 'text-content' }
])

const attributeLabel = (key: string) => {
  const known = t(`function.field.${ key }`)
  return known === `function.field.${ key }` ? key : known
}

const toggleExpand = (index: number) => {
  const next = new Set(expanded.value)
  if (next.has(index)) next.delete(index)
  else next.add(index)
  expanded.value = next
}

const toggleAll = (collapse: boolean) => {
  expanded.value = collapse ? new Set() : new Set(previewEntries.value.map((_, index) => index))
}

const applyDocument = (text: string) => {
  try {
    const parsed = parseFunctionDocument(text)
    if (!parsed.rows.length) {
      MfMessage.warning(t('function.import.emptyDocument'))
      return false
    }
    document.value = parsed
    // auto-guess the mapping for the new document
    const guessed: Record<string, string | undefined> = {}
    mappingFields.value.forEach(field => {
      guessed[field.key] = guessColumn(field.key, parsed.columns)
    })
    mapping.value = guessed
    MfMessage.success(t('function.import.documentReady')
      .replace('{rows}', String(parsed.rows.length))
      .replace('{columns}', String(parsed.columns.length)))
    return true
  }
  catch {
    MfMessage.error(t('function.import.parseFailed'))
    return false
  }
}

const onFetchUrl = () => {
  if (!url.value.trim()) {
    MfMessage.warning(t('function.import.noDocument'))
    return
  }
  fetching.value = true
  urlFetchedTip.value = ''
  FunctionService.previewUrl(url.value.trim())
               .then(response => {
                 if (response.status) {
                   urlText.value = String(response.data ?? '')
                   urlFetchedTip.value = t('function.import.urlFetched').replace('{count}', String(urlText.value.length))
                 }
                 else {
                   MfMessage.error(response.message)
                 }
               })
               .finally(() => (fetching.value = false))
}

const onPickFile = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  fileName.value = file.name
  fileSize.value = file.size
  file.text()
      .then(text => {
        fileText.value = text
      })
      .catch(() => MfMessage.error(t('function.import.parseFailed')))
  input.value = ''
}

const loadPlugins = () => {
  PluginService.getPlugins()
               .then(response => {
                 if (response.status) {
                   plugins.value = response.data
                       .filter((value: { type: string }) => value.type === 'CONNECTOR')
                       .map((value: { name: string }) => ({ label: value.name, value: value.name }))
                 }
               })
}

const onNext = () => {
  if (step.value === 1) {
    if (!activeText.value.trim()) {
      MfMessage.warning(t('function.import.noDocument'))
      return
    }
    if (!applyDocument(activeText.value)) return
    step.value = 2
    return
  }
  if (step.value === 2) {
    if (!plugin.value.length) {
      MfMessage.warning(t('function.import.noPlugin'))
      return
    }
    const missing = mappingFields.value.some(field => field.required && !mapping.value[field.key])
    if (missing) {
      MfMessage.warning(t('function.import.noMapping'))
      return
    }
    expanded.value = new Set()
    step.value = 3
  }
}

const onImport = () => {
  importing.value = true
  FunctionService.import({
    plugin: plugin.value,
    type: type.value,
    entries: entries.value,
    duplicate: duplicate.value
  })
               .then(response => {
                 if (response.status) {
                   const result = response.data || {}
                   MfMessage.success(t('function.import.success')
                     .replace('{imported}', String(result.imported ?? 0))
                     .replace('{duplicated}', String(result.duplicated ?? 0))
                     .replace('{invalid}', String(result.invalid ?? 0)))
                   visible.value = false
                 }
                 else {
                   MfMessage.error(response.message)
                 }
               })
               .finally(() => (importing.value = false))
}

const formatSize = (size: number) => (size > 1024 * 1024 ? `${ (size / 1024 / 1024).toFixed(1) } MB` : `${ Math.max(1, Math.round(size / 1024)) } KB`)

loadPlugins()
</script>
