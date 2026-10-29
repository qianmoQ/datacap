<template>
  <div>
    <MfSkeleton v-if="loading"/>

    <MfTable v-else
             :columns="headers"
             :data-source="rows"
             row-key="key"
             :empty-text="$t('common.noData')"/>
  </div>
</template>

<script lang="ts" setup>
import { computed, inject, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata'

defineOptions({ name: 'SourceTableForeignKeys' })

const { t } = useI18n()
const route = useRoute()
const stats = inject<{ foreignKeys: import('vue').Ref<any[]> } | null>('metadataTableStats', null)

const loading = ref(false)
const rows = ref<any[]>([])

const headers = computed(() => [
  { title: t('common.name'), key: 'name' },
  { title: t('source.fk.column'), key: 'column' },
  { title: t('source.fk.refTable'), key: 'refTable' },
  { title: t('source.fk.refColumn'), key: 'refColumn' }
])

const handleInitialize = () => {
  const code = route.params.source as string
  const database = route.params.database as string
  const table = route.params.table as string
  if (!(code && database && table)) return

  loading.value = true
  MetadataService.getForeignKeys(code, database, table)
                 .then(response => {
                   if (response.status && response.data?.isSuccessful) {
                     rows.value = (response.data.columns || []).map((row: any, index: number) => ({
                       key: `${ row.object_name }_${ index }`,
                       name: row.object_name,
                       column: row.object_column,
                       refTable: row.object_reference_table,
                       refColumn: row.object_reference_column
                     }))
                     if (stats) {
                       stats.foreignKeys.value = response.data.columns || []
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
