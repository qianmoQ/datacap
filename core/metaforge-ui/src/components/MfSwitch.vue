<script setup lang="ts">
import { computed } from 'vue'
import clsx from 'clsx'
import { TRANSITION } from './common'

const props = withDefaults(defineProps<{
    modelValue?: boolean
    disabled?: boolean
    size?: 'sm' | 'md'
}>(), {
    modelValue: false,
    disabled: false,
    size: 'md',
})

const emit = defineEmits<{
    'update:modelValue': [value: boolean]
}>()

const track = computed(() => clsx(
    'relative inline-flex shrink-0 cursor-pointer items-center rounded-mf-full border-2 border-transparent',
    TRANSITION,
    props.size === 'sm' ? 'h-5 w-9' : 'h-6 w-11',
    props.modelValue ? 'bg-primary' : 'bg-surface-active',
    props.disabled && 'cursor-not-allowed opacity-60',
))

const thumb = computed(() => clsx(
    'inline-block translate-x-0 rounded-mf-full bg-white shadow-mf-sm transition-transform duration-(--mf-duration-base) ease-(--mf-easing)',
    props.size === 'sm' ? 'size-3.5' : 'size-4.5',
    props.modelValue && (props.size === 'sm' ? 'translate-x-4' : 'translate-x-5'),
))

function toggle(): void {
    if (!props.disabled) {
        emit('update:modelValue', !props.modelValue)
    }
}
</script>

<template>
    <button type="button" role="switch" :aria-checked="modelValue" :class="track" :disabled="disabled" @click="toggle">
        <span :class="thumb" />
    </button>
</template>
