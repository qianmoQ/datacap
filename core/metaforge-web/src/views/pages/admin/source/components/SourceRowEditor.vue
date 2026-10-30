<template>
  <MfDrawer :open="open" inline :container="container || undefined" :width="width + 'px'"
            :title="$t(mode === 'edit' ? 'source.data.editRow' : 'source.data.addRow')"
            :closable="!saving" @update:open="value => !value && emit('close')">
    <form :id="formId" class="flex flex-col gap-4" @submit.prevent="emit('save')">
      <div v-for="name in columns" :key="name" class="flex flex-col gap-1.5">
        <label :for="`${formId}-${name}`" class="flex min-w-0 items-center gap-2 text-sm font-medium text-content">
          <span class="truncate" :title="name">{{ name }}</span>
          <MfTag v-if="typeOf(name)" size="sm">{{ typeOf(name) }}</MfTag>
        </label>
        <div class="flex items-center gap-2">
          <MfInput :id="`${formId}-${name}`" :model-value="values[name] ?? ''" size="sm" class="min-w-0 flex-1" :disabled="saving"
                   :placeholder="values[name] === null ? 'NULL' : ''" @update:model-value="value => values[name] = String(value)"/>
          <MfButton size="sm" :disabled="saving || values[name] === null" :aria-label="$t('source.data.setNull', { name })" @click="values[name] = null">NULL</MfButton>
        </div>
      </div>
    </form>
    <template #footer>
      <MfButton size="sm" :disabled="saving" @click="emit('close')">{{ $t('common.cancel') }}</MfButton>
      <MfButton size="sm" variant="primary" type="submit" :form="formId" :loading="saving">{{ $t('source.prototype.confirm') }}</MfButton>
    </template>
  </MfDrawer>
</template>

<script setup lang="ts">
import { getCurrentInstance } from 'vue'
import type { RowEditorMode } from '@/composables/useSourceTableData'
const values = defineModel<Record<string, string | null>>({ required: true })
defineProps<{
  open: boolean; mode: RowEditorMode; columns: string[]; typeOf: (name: string) => string
  saving: boolean; width: number; container: HTMLElement | null
}>()
const emit = defineEmits<{ close: []; save: [] }>()
const formId = 'source-row-form-' + getCurrentInstance()!.uid
</script>
