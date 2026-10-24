<script setup lang="ts">
import { computed } from 'vue'
import clsx from 'clsx'
import { CONTROL_SIZES, FOCUS_RING, TRANSITION, type MfControlSize } from './common'

export interface MfSelectOption {
    label: string
    value: string | number
    disabled?: boolean
}

const props = withDefaults(defineProps<{
    modelValue?: string | number
    options?: MfSelectOption[]
    size?: MfControlSize
    placeholder?: string
    disabled?: boolean
    invalid?: boolean
}>(), {
    modelValue: '',
    options: () => [],
    size: 'md',
    disabled: false,
    invalid: false,
})

const emit = defineEmits<{
    'update:modelValue': [value: string]
    change: [value: string]
}>()

const classes = computed(() => clsx(
    'w-full appearance-none rounded-mf-md border bg-surface pl-3 pr-8 text-sm text-content cursor-pointer',
    CONTROL_SIZES[props.size],
    'px-3!',
    FOCUS_RING,
    TRANSITION,
    props.invalid ? 'border-danger' : 'border-line',
    props.disabled && 'opacity-60 cursor-not-allowed',
    !props.modelValue && 'text-content-tertiary',
))

function onChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value
    emit('update:modelValue', value)
    emit('change', value)
}
</script>

<template>
    <div class="relative inline-flex w-full">
        <select :class="classes" :disabled="disabled" :value="modelValue" @change="onChange">
            <option v-if="placeholder" value="" disabled hidden>{{ placeholder }}</option>
            <option v-for="opt in options" :key="opt.value" :value="opt.value" :disabled="opt.disabled">
                {{ opt.label }}
            </option>
        </select>
        <svg
            class="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-content-tertiary"
            viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"
        >
            <path d="M6 9l6 6 6-6" />
        </svg>
    </div>
</template>
