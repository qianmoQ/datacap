<template>
  <div class="relative h-full min-h-0 overflow-hidden">
    <div v-if="loading" class="flex h-full items-center justify-center"><MfSpinner/></div>

    <AceEditor v-else-if="statement" :value="statement" height="100%" :read-only="true"/>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useSourceManageRoute as useRoute } from '@/composables/useSourceManageRoute'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata'
import AceEditor from '@/views/components/editor/AceEditor.vue'

defineOptions({ name: 'SourceTableStatement' })

const route = useRoute()

const loading = ref(false)
const statement = ref<string | null>(null)

const handleInitialize = () => {
  const code = route?.params.source as string
  const database = route?.params.database as string
  const table = route?.params.table as string

  if (code && database && table) {
    loading.value = true
    statement.value = null
    MetadataService.getTableStatement(code, database, table)
                   .then((response) => {
                     if (response.status && response.data && response.data.isSuccessful) {
                       const content = response.data.columns[0]
                       statement.value = content.create_table_sql
                     }
                     else {
                       MfMessage.error(response.data?.message)
                     }
                   })
                   .finally(() => (loading.value = false))
  }
}

watch(() => route?.params.table, () => handleInitialize())

handleInitialize()
</script>
