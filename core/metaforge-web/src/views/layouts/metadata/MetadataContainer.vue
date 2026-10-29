<template>
  <BaseLayout>
    <!-- 工作台通栏布局：侧栏贴左、内容区自适应（严格按原型） -->
    <div class="flex min-h-[calc(100vh-4rem)] w-full flex-col">
      <div class="flex w-full flex-1">
        <aside class="w-[260px] shrink-0 border-r border-line bg-surface">
          <MetadataSidebar/>
        </aside>
        <div class="min-w-0 flex-1 px-6 py-4">
          <MetadataContent/>
        </div>
      </div>
    </div>
  </BaseLayout>
</template>

<script setup lang="ts">
import { provide, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import MetadataSidebar from '@/views/layouts/metadata/components/MetadataSidebar.vue'
import MetadataContent from '@/views/layouts/metadata/components/MetadataContent.vue'
import BaseLayout from '@/views/layouts/base/BaseLayout.vue'
import MetadataService from '@/services/metadata'
import { MfMessage } from 'metaforge-ui'

// Shared table context (name/comment/engine/format) for the sidebar and content header,
// plus index / foreign-key listings for the tab counts and their pages.
const route = useRoute()
const tableInfo = ref<any | null>(null)
const tableIndexes = ref<any[]>([])
const tableForeignKeys = ref<any[]>([])
const metadataRefreshKey = ref(0)
provide('metadataTableInfo', tableInfo)
provide('metadataTableStats', { indexes: tableIndexes, foreignKeys: tableForeignKeys })
provide('metadataRefreshKey', metadataRefreshKey)

const loadTableContext = () => {
  const source = route.params.source as string
  const database = route.params.database as string
  const table = route.params.table as string
  if (!(source && database && table)) {
    tableInfo.value = null
    tableIndexes.value = []
    tableForeignKeys.value = []
    return
  }
  MetadataService.getTable(source, database, table)
                 .then(response => {
                   if (response.status && response.data?.isSuccessful) {
                     tableInfo.value = response.data.columns?.[0] || null
                   }
                   else {
                     MfMessage.error(response.message)
                   }
                 })
  MetadataService.getIndexes(source, database, table)
                 .then(response => {
                   if (response.status && response.data?.isSuccessful) {
                     tableIndexes.value = response.data.columns || []
                   }
                 })
  MetadataService.getForeignKeys(source, database, table)
                 .then(response => {
                   if (response.status && response.data?.isSuccessful) {
                     tableForeignKeys.value = response.data.columns || []
                   }
                 })
}

watch(
    () => [route.params.source, route.params.database, route.params.table, metadataRefreshKey.value],
    () => loadTableContext(),
    { immediate: true }
)
</script>
