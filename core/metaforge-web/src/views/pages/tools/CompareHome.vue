<template>
  <div class="mx-auto max-w-4xl">
    <div class="mb-5">
      <h1 class="m-0 text-xl font-semibold text-content">{{ $t('tools.compare.title') }}</h1>
      <p class="mb-0 mt-1 text-sm text-content-secondary">{{ $t('tools.compare.desc') }}</p>
    </div>

    <div class="rounded-mf-lg border border-line bg-surface p-5">
      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="mb-1 block text-sm font-medium text-content-secondary">{{ $t('source.common.name') }} <span class="text-danger">*</span></label>
          <MfSelect v-model="sourceCode" :options="sourceOptions" @update:model-value="onSourceChange"/>
        </div>
        <div>
          <label class="mb-1 block text-sm font-medium text-content-secondary">{{ $t('source.common.database') }} <span class="text-danger">*</span></label>
          <MfSelect v-model="databaseName" :options="databaseOptions"/>
        </div>
      </div>

      <div class="mt-4 grid grid-cols-3 items-end gap-3">
        <div>
          <label class="mb-1 block text-sm font-medium text-content-secondary">{{ $t('tools.compare.tableA') }} <span class="text-danger">*</span></label>
          <MfSelect v-model="tableA" :options="tableOptions"/>
        </div>
        <div>
          <label class="mb-1 block text-sm font-medium text-content-secondary">{{ $t('tools.compare.tableB') }} <span class="text-danger">*</span></label>
          <MfSelect v-model="tableB" :options="tableOptions"/>
        </div>
        <MfButton variant="primary" :loading="comparing" :disabled="!tableA || !tableB || tableA === tableB" @click="onCompare">
          <GitCompare :size="14"/>
          {{ $t('tools.compare.execute') }}
        </MfButton>
      </div>
    </div>

    <template v-if="compared">
      <div v-if="onlyInA.length === 0 && onlyInB.length === 0 && mismatches.length === 0"
           class="mt-5 rounded-mf-lg border border-line bg-surface p-6 text-center text-sm text-content-tertiary">
        {{ $t('tools.compare.identical') }}
      </div>

      <div v-for="section in sections" v-else :key="section.key" class="mt-4 rounded-mf-lg border border-line bg-surface">
        <div class="border-b border-line px-4 py-2.5 text-sm font-medium text-content">{{ section.title }}</div>
        <MfTable :columns="section.headers" :data-source="section.rows" row-key="__key"/>
      </div>
    </template>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { GitCompare } from '@lucide/vue'
import { FilterModel } from '@/model/filter'
import SourceService from '@/services/source'
import MetadataService from '@/services/metadata'

defineOptions({ name: 'CompareHome' })

const { t } = useI18n()

const sourceCode = ref('')
const databaseName = ref('')
const tableA = ref('')
const tableB = ref('')
const sourceOptions = ref<{ label: string; value: string }[]>([])
const databaseOptions = ref<{ label: string; value: string }[]>([])
const tableOptions = ref<{ label: string; value: string }[]>([])
const comparing = ref(false)
const compared = ref(false)
const onlyInA = ref<any[]>([])
const onlyInB = ref<any[]>([])
const mismatches = ref<any[]>([])

const loadDatabases = () => {
  databaseName.value = ''
  tableOptions.value = []
  databaseOptions.value = []
  if (!sourceCode.value) return
  MetadataService.getDatabaseBySource(sourceCode.value)
                 .then(response => {
                   if (response.status) {
                     databaseOptions.value = (response.data?.columns || [])
                         .map((item: any) => ({ label: item.object_name || item.schema_name, value: item.object_name || item.schema_name }))
                         .filter((item: any) => item.value)
                   }
                 })
}

const onSourceChange = () => loadDatabases()

const loadColumns = (table: string) => {
  return MetadataService.getColumnsByTable(sourceCode.value, databaseName.value, table)
      .then(response => {
        if (response.status && response.data?.isSuccessful) {
          return (response.data.columns || [])
              .filter((row: any) => row.type_name === 'column')
              .map((row: any) => ({ name: row.object_name, type: row.object_data_type }))
        }
        return []
      })
}

const onCompare = () => {
  comparing.value = true
  Promise.all([loadColumns(tableA.value), loadColumns(tableB.value)])
         .then(([columnsA, columnsB]) => {
           const mapA = new Map(columnsA.map(item => [item.name, item.type]))
           const mapB = new Map(columnsB.map(item => [item.name, item.type]))
           onlyInA.value = columnsA.filter(item => !mapB.has(item.name))
               .map((item, index) => ({ __key: `a_${ index }`, name: item.name, type: item.type }))
           onlyInB.value = columnsB.filter(item => !mapA.has(item.name))
               .map((item, index) => ({ __key: `b_${ index }`, name: item.name, type: item.type }))
           mismatches.value = columnsA.filter(item => mapB.has(item.name) && mapB.get(item.name) !== item.type)
               .map((item, index) => ({
                 __key: `m_${ index }`,
                 name: item.name,
                 typeA: item.type,
                 typeB: mapB.get(item.name)
               }))
           compared.value = true
         })
         .finally(() => (comparing.value = false))
}

const diffHeaders = computed(() => [
  { title: t('source.structure.fieldName'), key: 'name' },
  { title: t('source.structure.type'), key: 'type' }
])
const mismatchHeaders = computed(() => [
  { title: t('source.structure.fieldName'), key: 'name' },
  { title: t('tools.compare.tableA'), key: 'typeA' },
  { title: t('tools.compare.tableB'), key: 'typeB' }
])

const sections = computed(() => [
  { key: 'a', title: t('tools.compare.onlyA'), headers: diffHeaders.value, rows: onlyInA.value },
  { key: 'b', title: t('tools.compare.onlyB'), headers: diffHeaders.value, rows: onlyInB.value },
  { key: 'm', title: t('tools.compare.mismatch'), headers: mismatchHeaders.value, rows: mismatches.value }
])

SourceService.getAll(new FilterModel())
             .then(response => {
               if (response.status) {
                 sourceOptions.value = (response.data.content || [])
                     .map((item: any) => ({ label: item.name, value: item.code }))
               }
             })

watch(databaseName, () => {
  tableA.value = ''
  tableB.value = ''
  compared.value = false
})
</script>
