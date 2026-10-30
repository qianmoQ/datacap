<script setup lang="ts" generic="T extends Record<string, unknown>">
import { computed } from 'vue'
import clsx from 'clsx'
import MfSpinner from './MfSpinner.vue'
import MfEmpty from './MfEmpty.vue'

export interface MfTableColumn {
    title: string
    dataIndex: string
    key?: string
    width?: string | number
    align?: 'left' | 'center' | 'right'
    /** Pin the column while the table scrolls horizontally. */
    fixed?: 'left' | 'right'
    /**
     * Clamp the column to its width and truncate overflowing text;
     * pair it with a tooltip cell so the full content stays reachable.
     */
    ellipsis?: boolean
    /** Rendered as `--` when falsy. */
    placeholder?: string
}

const props = withDefaults(defineProps<{
    columns?: MfTableColumn[]
    dataSource?: T[]
    rowKey?: string
    loading?: boolean
    bordered?: boolean
    striped?: boolean
    hoverable?: boolean
    density?: 'default' | 'compact'
    rowClass?: (record: T, index: number) => string
    /** Scroll rows inside a bounded workspace while keeping the header visible. */
    fill?: boolean
  /** Empty-state text. */
    emptyText?: string
    emptyDescription?: string
    emptyVariant?: 'data' | 'search' | 'initial'
}>(), {
    columns: () => [],
    dataSource: () => [],
    rowKey: 'id',
    loading: false,
    bordered: true,
    striped: false,
    hoverable: true,
    density: 'default',
    fill: false,
    emptyText: 'No data',
    emptyDescription: '',
    emptyVariant: 'data',
})

defineEmits<{
    rowClick: [record: T, index: number]
}>()

const alignClass = (align?: string) =>
    align === 'center' ? 'text-center' : align === 'right' ? 'text-right' : 'text-left'

const colKey = (column: MfTableColumn) => column.key ?? column.dataIndex

const widthStyle = (column: MfTableColumn) =>
    column.width === undefined ? {} : { width: typeof column.width === 'number' ? `${column.width}px` : column.width }

// Sticky cells need an opaque background; striped rows blend surface-hover over
// surface, so the striped sticky cell uses the pre-mixed solid color.
const STRIPED_SOLID = 'bg-[color-mix(in_srgb,var(--mf-color-surface-hover)_40%,var(--mf-color-surface))]'

const rows = computed(() => props.dataSource)

function display(record: T, column: MfTableColumn): unknown {
    return record[column.dataIndex]
}
</script>

<template>
    <div class="w-full" :class="fill ? 'min-h-0 flex-1 overflow-auto' : 'overflow-x-auto'">
        <table class="w-full border-collapse text-sm" :class="bordered && 'border border-line rounded-mf-lg'">
            <thead>
                <tr class="bg-surface-hover">
                    <th
                        v-for="column in columns"
                        :key="colKey(column)"
                        class="whitespace-nowrap border-b border-line px-3 text-xs font-semibold text-content-secondary"
                        :class="[
                            alignClass(column.align),
                            density === 'compact' ? 'py-2' : 'py-2.5',
                            fill && 'sticky top-0 z-20 bg-surface-hover',
                            bordered && 'border-r border-line last:border-r-0',
                            column.fixed === 'right' && 'sticky right-0 z-10 bg-surface-hover border-l border-line',
                            column.fixed === 'left' && 'sticky left-0 z-10 bg-surface-hover border-r border-line',
                        ]"
                        :style="widthStyle(column)"
                    >
                        <slot :name="`header-${colKey(column)}`" :column="column">
                            {{ column.title }}
                        </slot>
                    </th>
                </tr>
            </thead>
            <tbody v-if="!loading">
                <tr
                    v-for="(record, index) in rows"
                    :key="String(record[rowKey] ?? index)"
                    :data-row-key="String(record[rowKey] ?? index)"
                    class="group transition-[background-color] duration-(--mf-duration-fast)"
                    :class="clsx(
                        striped && index % 2 === 1 && 'bg-surface-hover/40',
                        hoverable && 'hover:bg-surface-hover',
                        'border-b border-line last:border-b-0',
                        rowClass?.(record, index),
                    )"
                    @click="$emit('rowClick', record, index)"
                >
                    <td
                        v-for="column in columns"
                        :key="colKey(column)"
                        class="whitespace-nowrap px-3 text-content"
                        :class="[
                            alignClass(column.align),
                            density === 'compact' ? 'py-1.5' : 'py-2.5',
                            bordered && 'border-r border-line last:border-r-0',
                            column.ellipsis && 'max-w-0 overflow-hidden',
                            column.fixed === 'right' && clsx('sticky right-0 z-10 border-l border-line', striped && index % 2 === 1 ? STRIPED_SOLID : 'bg-surface', hoverable && 'group-hover:bg-surface-hover'),
                            column.fixed === 'left' && clsx('sticky left-0 z-10 border-r border-line', striped && index % 2 === 1 ? STRIPED_SOLID : 'bg-surface', hoverable && 'group-hover:bg-surface-hover'),
                        ]"
                    >
                        <slot
                            :name="`cell-${colKey(column)}`"
                            :record="record"
                            :column="column"
                            :index="index"
                            :text="display(record, column)"
                        >
                            {{ display(record, column) ?? column.placeholder ?? '--' }}
                        </slot>
                    </td>
                </tr>
                <tr v-if="rows.length === 0">
                    <td :colspan="Math.max(columns.length, 1)" class="px-3 py-3 text-center text-sm text-content-tertiary">
                        <slot name="emptyText">
                            <MfEmpty :title="emptyText" :description="emptyDescription" :variant="emptyVariant" size="sm"/>
                        </slot>
                    </td>
                </tr>
            </tbody>
        </table>
        <div v-if="loading" class="flex items-center justify-center py-10">
            <MfSpinner label="Loading..." />
        </div>
    </div>
</template>
