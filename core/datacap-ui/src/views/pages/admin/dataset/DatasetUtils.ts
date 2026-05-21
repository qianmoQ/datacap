import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

/**
 * Hook for dataset table headers
 */
export function useDatasetHeaders()
{
    const { t } = useI18n()

    const headers = computed(() => [
        { key: 'id', label: t('common.id') },
        { key: 'name', label: t('common.name') },
        { key: 'description', label: t('common.description') },
        { key: 'source', label: t('common.source'), slot: 'source' },
        { key: 'syncMode', label: t('dataset.common.syncMode'), slot: 'syncMode' },
        { key: 'scheduler', label: t('common.scheduler') },
        { key: 'executor', label: t('common.executor') },
        { key: 'state', label: t('common.state'), slot: 'state' },
        { key: 'totalRows', label: t('dataset.common.totalRows') },
        { key: 'totalSize', label: t('dataset.common.totalSize') },
        { key: 'createTime', label: t('common.createTime') },
        { key: 'updateTime', label: t('common.updateTime') },
        { key: 'action', label: t('common.action'), slot: 'action' }
    ])

    const historyHeaders = computed(() => [
        { key: 'id', label: t('common.id') },
        { key: 'elapsed', label: t('common.elapsed') },
        { key: 'totalCount', label: t('dataset.history.totalCount') },
        { key: 'processedCount', label: t('dataset.history.processedCount') },
        { key: 'progress', label: t('dataset.history.progress'), slot: 'progress' },
        { key: 'count', label: t('common.count') },
        { key: 'createTime', label: t('common.createTime') },
        { key: 'updateTime', label: t('common.updateTime') },
        { key: 'state', label: t('common.state'), slot: 'state' }
    ])

    return {
        headers,
        historyHeaders
    }
}
