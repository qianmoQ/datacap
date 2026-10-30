<script setup lang="ts" generic="T extends Record<string, unknown>">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
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
    rowClick: [record: T, index: number, event: MouseEvent]
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

const rowClasses = (record: T, index: number) => clsx(
    props.striped && index % 2 === 1 && 'bg-surface-hover/40',
    props.hoverable && 'hover:bg-surface-hover',
    'border-b border-line last:border-b-0',
    props.rowClass?.(record, index),
)

const tdClass = (column: MfTableColumn, index: number) => [
    'whitespace-nowrap px-3 text-content',
    alignClass(column.align),
    props.density === 'compact' ? 'py-1.5' : 'py-2.5',
    props.bordered && 'border-r border-line last:border-r-0',
    column.ellipsis && 'max-w-0 overflow-hidden',
    column.fixed === 'right' && clsx('sticky right-0 z-10 border-l border-line', props.striped && index % 2 === 1 ? STRIPED_SOLID : 'bg-surface', props.hoverable && 'group-hover:bg-surface-hover'),
    column.fixed === 'left' && clsx('sticky left-0 z-10 border-r border-line', props.striped && index % 2 === 1 ? STRIPED_SOLID : 'bg-surface', props.hoverable && 'group-hover:bg-surface-hover'),
]

const thClass = (column: MfTableColumn) => [
    'whitespace-nowrap px-3 text-xs font-semibold text-content-secondary',
    alignClass(column.align),
    props.density === 'compact' ? 'py-2' : 'py-2.5',
    !props.fill && 'border-b border-line',
    props.bordered && 'border-r border-line last:border-r-0',
    column.fixed === 'right' && 'sticky right-0 z-10 bg-surface-hover border-l border-line',
    column.fixed === 'left' && 'sticky left-0 z-10 bg-surface-hover border-r border-line',
]

/*
 * Fill mode splits the header from the scrolling body so the scrollbar only runs beside the rows.
 * The body keeps an invisible copy of the header row: it sizes the columns like a single table
 * would, and its measured cell widths are applied to the visible header.
 */
const headScroller = ref<HTMLElement>()
const bodyScroller = ref<HTMLElement>()
const bodyTable = ref<HTMLTableElement>()
const headWidths = ref<number[]>([])
const headHeight = ref(0)
const bodyWidth = ref(0)
const gutter = ref(0)

function measure(): void {
    const row = bodyTable.value?.tHead?.rows[0]
    const scroller = bodyScroller.value
    if (!row || !scroller) return
    headWidths.value = Array.from(row.cells).map(cell => cell.getBoundingClientRect().width)
    headHeight.value = row.getBoundingClientRect().height
    bodyWidth.value = bodyTable.value!.getBoundingClientRect().width
    gutter.value = scroller.offsetWidth - scroller.clientWidth
    syncScroll()
}

function syncScroll(): void {
    if (headScroller.value && bodyScroller.value) headScroller.value.scrollLeft = bodyScroller.value.scrollLeft
}

let observer: ResizeObserver | undefined
onMounted(() => {
    if (!props.fill || typeof ResizeObserver === 'undefined') return
    observer = new ResizeObserver(measure)
    if (bodyTable.value) observer.observe(bodyTable.value)
    if (bodyScroller.value) observer.observe(bodyScroller.value)
    measure()
})
watch(() => [props.columns, props.dataSource, props.loading], () => { if (props.fill) nextTick(measure) }, { deep: false })
onBeforeUnmount(() => observer?.disconnect())
</script>

<template>
    <div v-if="fill" class="flex min-h-0 w-full flex-1 flex-col overflow-hidden" :class="bordered && 'rounded-mf-lg border border-line'">
        <div class="flex shrink-0 border-b border-line bg-surface-hover">
            <div ref="headScroller" class="min-w-0 flex-1 overflow-hidden">
                <table class="table-fixed border-collapse text-sm" :style="{ width: bodyWidth ? `${bodyWidth}px` : '100%' }">
                    <colgroup v-if="headWidths.length === columns.length">
                        <col v-for="(width, index) in headWidths" :key="index" :style="{ width: `${width}px` }">
                    </colgroup>
                    <thead>
                        <tr>
                            <th v-for="column in columns" :key="colKey(column)" :class="thClass(column)" :style="widthStyle(column)">
                                <slot :name="`header-${colKey(column)}`" :column="column">{{ column.title }}</slot>
                            </th>
                        </tr>
                    </thead>
                </table>
            </div>
            <div class="shrink-0" :style="{ width: `${gutter}px` }" />
        </div>
        <div ref="bodyScroller" class="flex min-h-0 flex-1 flex-col overflow-auto" @scroll="syncScroll">
            <table ref="bodyTable" class="w-full shrink-0 border-collapse text-sm" :style="{ marginTop: `-${headHeight}px` }">
                <thead class="invisible" aria-hidden="true">
                    <tr>
                        <th v-for="column in columns" :key="colKey(column)" :class="thClass(column)" :style="widthStyle(column)">
                            <slot :name="`header-${colKey(column)}`" :column="column">{{ column.title }}</slot>
                        </th>
                    </tr>
                </thead>
                <tbody v-if="!loading">
                    <tr
                        v-for="(record, index) in rows"
                        :key="String(record[rowKey] ?? index)"
                        :data-row-key="String(record[rowKey] ?? index)"
                        class="group transition-[background-color] duration-(--mf-duration-fast)"
                        :class="rowClasses(record, index)"
                        @click="$emit('rowClick', record, index, $event)"
                    >
                        <td v-for="column in columns" :key="colKey(column)" :class="tdClass(column, index)">
                            <slot :name="`cell-${colKey(column)}`" :record="record" :column="column" :index="index" :text="display(record, column)">
                                {{ display(record, column) ?? column.placeholder ?? '--' }}
                            </slot>
                        </td>
                    </tr>
                </tbody>
            </table>
            <!-- The empty and loading states fill the space under the header, centered and pinned to the
                 visible width while the table scrolls sideways. -->
            <div v-if="loading || rows.length === 0" class="sticky left-0 flex w-full min-h-40 flex-1 items-center justify-center px-3 py-6">
                <MfSpinner v-if="loading" label="Loading..." />
                <slot v-else name="emptyText">
                    <MfEmpty :title="emptyText" :description="emptyDescription" :variant="emptyVariant" size="sm"/>
                </slot>
            </div>
        </div>
    </div>
    <div v-else class="w-full overflow-x-auto">
        <table class="w-full border-collapse text-sm" :class="bordered && 'border border-line rounded-mf-lg'">
            <thead>
                <tr class="bg-surface-hover">
                    <th v-for="column in columns" :key="colKey(column)" :class="thClass(column)" :style="widthStyle(column)">
                        <slot :name="`header-${colKey(column)}`" :column="column">{{ column.title }}</slot>
                    </th>
                </tr>
            </thead>
            <tbody v-if="!loading">
                <tr
                    v-for="(record, index) in rows"
                    :key="String(record[rowKey] ?? index)"
                    :data-row-key="String(record[rowKey] ?? index)"
                    class="group transition-[background-color] duration-(--mf-duration-fast)"
                    :class="rowClasses(record, index)"
                    @click="$emit('rowClick', record, index, $event)"
                >
                    <td v-for="column in columns" :key="colKey(column)" :class="tdClass(column, index)">
                        <slot :name="`cell-${colKey(column)}`" :record="record" :column="column" :index="index" :text="display(record, column)">
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
