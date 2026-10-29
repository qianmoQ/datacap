<template>
  <div>
    <MfSkeleton v-if="loading"/>

    <MfTable v-else
             :columns="headers"
             :data-source="rows"
             row-key="key"
             :empty-text="$t('common.noData')">
      <template #cell-object_unique="{ record }">
        <MfTag size="sm" :variant="record.unique ? 'warning' : 'default'">
          {{ record.unique ? $t('source.index.uniqueLabel') : $t('source.index.normal') }}
        </MfTag>
      </template>
    </MfTable>
  </div>
</template>

<script lang="ts" setup>
import { computed, inject, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata'

defineOptions({ name: 'SourceTableIndexes' })

const { t } = useI18n()
const route = useRoute()
const stats = inject<{ indexes: import('vue').Ref<any[]> } | null>('metadataTableStats', null)

const loading = ref(false)
const rows = ref<any[]>([])

const headers = computed(() => [
  { title: t('source.index.indexName'), key: 'name' },
  { title: t('source.index.columns'), key: 'column' },
  { title: t('source.index.indexType'), key: 'object_unique', width: 120 }
])

const handleInitialize = () => {
  const code = route.params.source as string
  const database = route.params.database as string
  const table = route.params.table as string
  if (!(code && database && table)) return

  loading.value = true
  MetadataService.getIndexes(code, database, table)
                 .then(response => {
                   if (response.status && response.data?.isSuccessful) {
                     const raw = response.data.columns || []
                     // 同名索引的多个列合并为一行
                     const grouped = new Map<string, any>()
                     raw.forEach((row: any) => {
                       const existing = grouped.get(row.object_name)
                       if (existing) {
                         existing.columns.push(row.object_column)
                       }
                       else {
                         grouped.set(row.object_name, {
                           key: `${ row.object_name }_${ grouped.size }`,
                           name: row.object_name,
                           columns: [row.object_column],
                           unique: row.object_unique === true || row.object_unique === 'true'
                         })
                       }
                     })
                     rows.value = [...grouped.values()]
                     if (stats) {
                       stats.indexes.value = raw
                     }
                   }
                   else {
                     MfMessage.error(response.message)
                   }
                 })
                 .finally(() => (loading.value = false))
}

watch(() => route.params.table, () => handleInitialize())

onMounted(() => handleInitialize())
</script>
