<script setup lang="ts">
import { computed, inject, onBeforeUnmount, onMounted } from 'vue'

interface TabsContext {
    register: (key: string | number, label: string | number) => void
    unregister: (key: string | number) => void
    isActive: (key: string | number) => boolean
}

const props = withDefaults(defineProps<{
    /** Stable identity; also usable as `:key` on the pane. */
    itemKey?: string | number
    /** Nav label. */
    tab: string | number
    disabled?: boolean
}>(), {
    itemKey: '',
    disabled: false,
})

const key = computed(() => props.itemKey !== '' ? props.itemKey : props.tab)
const tabs = inject<TabsContext | null>('mfTabs', null)
const active = computed(() => tabs?.isActive(key.value) ?? false)

onMounted(() => tabs?.register(key.value, props.tab))
onBeforeUnmount(() => tabs?.unregister(key.value))
</script>

<template>
    <div v-show="active" role="tabpanel" class="outline-none">
        <slot />
    </div>
</template>
