<template>
  <div class="rounded-mf-lg border border-line bg-surface p-5">
    <MfEmpty :title="$t('source.tab.triggers')" :description="$t('source.tab.triggersUnsupported')"/>
  </div>
</template>

<script lang="ts" setup>
defineOptions({ name: 'SourceTableTriggers' })
</script>
