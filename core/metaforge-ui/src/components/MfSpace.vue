<script setup lang="ts">
import { computed } from 'vue'
import clsx from 'clsx'

const props = withDefaults(defineProps<{
    vertical?: boolean
    size?: 'xs' | 'sm' | 'md' | 'lg'
    wrap?: boolean
}>(), {
    vertical: false,
    size: 'md',
    wrap: false,
})

const GAP = {
    xs: 'gap-1',
    sm: 'gap-2',
    md: 'gap-3',
    lg: 'gap-6',
} as const

const classes = computed(() => clsx(
    'flex',
    props.vertical ? 'flex-col' : clsx('items-center', props.wrap && 'flex-wrap'),
    GAP[props.size],
))
</script>

<template>
    <div :class="classes">
        <slot />
    </div>
</template>
