<template>
  <MfTable :columns="headers" :data-source="rows" :loading="loading" row-key="object_name" :empty-text="$t('common.noData')">
    <template #cell-object_name="{ record }"><EllipsisCell :text="String(record.object_name)"/></template>
    <template #cell-object_definition="{ record }"><EllipsisCell :text="String(record.object_definition)"/></template>
  </MfTable>
</template>
<script setup lang="ts">
import { useSourceTableTriggers } from '@/composables/useSourceTableTriggers'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
const { loading, rows, headers } = useSourceTableTriggers()
</script>
