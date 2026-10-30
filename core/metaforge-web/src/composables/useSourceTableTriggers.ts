import { computed, inject, ref, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'

export function useSourceTableTriggers() {
  const { t } = useI18n()
  const columnRows = inject<Ref<Record<string, unknown>[]>>('metadataColumnRows', ref([]))
  const loading = inject<Ref<boolean>>('metadataLoading', ref(false))
  const rows = computed(() => columnRows.value.filter(row => row.type_name === 'trigger'))
  const headers = computed(() => [
    { title: t('common.name'), dataIndex: 'object_name', width: 220, ellipsis: true },
    { title: t('source.prototype.triggerDefinition'), dataIndex: 'object_definition', width: 500, ellipsis: true },
  ])
  return { loading, rows, headers }
}
