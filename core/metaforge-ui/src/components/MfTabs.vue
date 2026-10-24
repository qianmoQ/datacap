<script setup lang="ts">
import { computed, provide, ref, watch } from 'vue'
import clsx from 'clsx'

const props = withDefaults(defineProps<{
    activeKey?: string | number
    tabPosition?: 'top' | 'left' | 'right' | 'bottom'
}>(), {
    activeKey: '',
    tabPosition: 'top',
})

const emit = defineEmits<{
    'update:activeKey': [key: string | number]
    change: [key: string | number]
}>()

interface PaneEntry {
    key: string | number
    label: string | number
}

const innerKey = ref<string | number>(props.activeKey)
watch(() => props.activeKey, value => { innerKey.value = value })

const panes = ref<PaneEntry[]>([])

provide('mfTabs', {
    register(key: string | number, label: string | number): void {
        if (!panes.value.some(p => p.key === key)) {
            panes.value = [...panes.value, { key, label }]
            if (innerKey.value === '' && panes.value.length === 1) {
                select(key)
            }
        }
    },
    unregister(key: string | number): void {
        panes.value = panes.value.filter(p => p.key !== key)
    },
    isActive(key: string | number): boolean {
        return innerKey.value === key
    },
})

function select(key: string | number): void {
    if (innerKey.value !== key) {
        innerKey.value = key
        emit('update:activeKey', key)
        emit('change', key)
    }
}

const vertical = computed(() => props.tabPosition === 'left' || props.tabPosition === 'right')

const navClass = computed(() => clsx(
    'flex gap-1',
    vertical
        ? clsx('flex-col py-1', props.tabPosition === 'right' ? 'border-l border-line order-2' : 'border-r border-line order-1')
        : clsx('flex-row items-center border-b border-line px-1', props.tabPosition === 'bottom' ? 'order-2' : 'order-1'),
))

function tabClass(key: string | number): string {
    const active = innerKey.value === key
    return clsx(
        'cursor-pointer whitespace-nowrap px-3.5 py-2 text-sm font-medium select-none',
        'transition-[color,border-color,background-color] duration-(--mf-duration-base) ease-(--mf-easing)',
        active
            ? vertical ? 'text-primary bg-surface-active rounded-mf-md' : 'border-b-2 border-b-primary text-primary -mb-px'
            : clsx('text-content-secondary hover:text-content', vertical ? 'rounded-mf-md hover:bg-surface-hover' : 'border-b-2 border-b-transparent'),
    )
}
</script>

<template>
    <div class="flex w-full" :class="vertical ? 'flex-row' : 'flex-col'">
        <div role="tablist" :class="navClass">
            <div
                v-for="pane in panes"
                :key="pane.key"
                role="tab"
                :aria-selected="innerKey === pane.key"
                :class="tabClass(pane.key)"
                @click="select(pane.key)"
            >
                {{ pane.label }}
            </div>
        </div>
        <div class="min-w-0 flex-1" :class="vertical ? 'px-3' : (tabPosition === 'bottom' ? '' : 'pt-3')">
            <slot />
        </div>
    </div>
</template>
