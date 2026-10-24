<script setup lang="ts">
import { computed } from 'vue'
import clsx from 'clsx'
import { CONTROL_SIZES, FOCUS_RING, TRANSITION, type MfControlSize } from './common'

const props = withDefaults(defineProps<{
    modelValue?: string | number
    type?: string
    size?: MfControlSize
    placeholder?: string
    clearable?: boolean
    disabled?: boolean
    readonly?: boolean
    invalid?: boolean
    maxlength?: number
}>(), {
    modelValue: '',
    type: 'text',
    size: 'md',
    clearable: false,
    disabled: false,
    readonly: false,
    invalid: false,
})

const emit = defineEmits<{
    'update:modelValue': [value: string]
    blur: [event: FocusEvent]
    enter: [event: KeyboardEvent]
}>()

const wrapperClass = computed(() => clsx(
    'inline-flex items-center w-full rounded-mf-md border bg-surface',
    CONTROL_SIZES[props.size],
    'px-0!',
    TRANSITION,
    props.invalid ? 'border-danger' : 'border-line',
    props.disabled && 'opacity-60 cursor-not-allowed',
))

function onInput(event: Event): void {
    emit('update:modelValue', (event.target as HTMLInputElement).value)
}

function clear(): void {
    emit('update:modelValue', '')
}
</script>

<template>
    <div :class="wrapperClass">
        <span v-if="$slots.prefix" class="pl-3 inline-flex items-center text-content-tertiary shrink-0">
            <slot name="prefix" />
        </span>
        <input
            :value="modelValue"
            :type="type"
            :placeholder="placeholder"
            :disabled="disabled"
            :readonly="readonly"
            :maxlength="maxlength"
            class="min-w-0 flex-1 bg-transparent px-3 text-sm text-content placeholder:text-content-tertiary outline-none"
            :class="[FOCUS_RING, readonly ? 'cursor-default' : '']"
            @input="onInput"
            @blur="emit('blur', $event)"
            @keyup.enter="emit('enter', $event)"
        >
        <button
            v-if="clearable && modelValue && !disabled && !readonly"
            type="button"
            class="mr-2 inline-flex size-5 items-center justify-center rounded-mf-full text-content-tertiary hover:bg-surface-hover hover:text-content"
            aria-label="clear"
            @click="clear"
        >
            <svg class="size-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path d="M6 6l12 12M18 6L6 18" />
            </svg>
        </button>
        <span v-if="$slots.suffix" class="pr-3 inline-flex items-center text-content-tertiary shrink-0">
            <slot name="suffix" />
        </span>
    </div>
</template>
