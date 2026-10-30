<template>
  <div class="flex h-full min-h-0 flex-col gap-2 overflow-hidden">
    <section class="flex min-h-0 flex-[0_0_42%] flex-col overflow-hidden rounded-mf-md border border-line">
      <div class="flex shrink-0 items-center justify-between border-b border-line px-2 py-1.5">
        <span class="text-sm font-medium text-content-secondary">{{ $t('source.tab.sqlQuery') }}</span>
        <MfButton size="sm" variant="primary" :loading="running" :disabled="!content.trim()" @click="run"><Play :size="14"/>{{ $t('query.common.execute') }}</MfButton>
      </div>
      <div ref="editorRoot" class="min-h-0 flex-1"><AceEditor v-model:value="content" height="100%" @created="onEditorCreated"/></div>
    </section>
    <section class="flex min-h-0 flex-1 flex-col overflow-hidden rounded-mf-md border border-line">
      <QueryResultPanel :response="response" :running="running" :query="executedQuery" :source-code="sourceCode" :dataset-available="false" @refresh="run"/>
    </section>
  </div>
</template>
<script setup lang="ts">
import { Play } from '@lucide/vue'
import AceEditor from '@/views/components/editor/AceEditor.vue'
import QueryResultPanel from '@/views/pages/admin/query/QueryResultPanel.vue'
import { useSourceSqlPane } from '@/composables/useSourceSqlPane'
const { content, response, running, executedQuery, editorRoot, onEditorCreated, run, sourceCode } = useSourceSqlPane()
</script>
