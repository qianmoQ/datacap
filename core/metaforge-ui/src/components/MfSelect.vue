<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import clsx from 'clsx'
import { CONTROL_SIZES, TRANSITION, type MfControlSize, type MfSelectOption } from './common'

const props = withDefaults(defineProps<{
    modelValue?: string | number | (string | number)[]
    options?: MfSelectOption[]
    size?: MfControlSize
    placeholder?: string
    multiple?: boolean
    clearable?: boolean
    disabled?: boolean
    invalid?: boolean
}>(), {
    modelValue: '',
    options: () => [],
    size: 'md',
    multiple: false,
    clearable: false,
    disabled: false,
    invalid: false,
})

const emit = defineEmits<{
    'update:modelValue': [value: string | number | (string | number)[]]
    change: [value: string | number | (string | number)[]]
}>()

const open = ref(false)
const root = ref<HTMLElement>()

function toggle(): void {
    if (!props.disabled) {
        open.value = !open.value
    }
}

function close(): void {
    open.value = false
}

function onDocClick(event: MouseEvent): void {
    if (root.value && !root.value.contains(event.target as Node)) {
        close()
    }
}

onMounted(() => document.addEventListener('mousedown', onDocClick))
onBeforeUnmount(() => document.removeEventListener('mousedown', onDocClick))

const selectedValues = computed<(string | number)[]>(() =>
    Array.isArray(props.modelValue)
        ? props.modelValue
        : props.modelValue === '' || props.modelValue === undefined || props.modelValue === null
            ? []
            : [props.modelValue])

function isSelected(option: MfSelectOption): boolean {
    return selectedValues.value.includes(option.value)
}

function select(option: MfSelectOption): void {
    if (option.disabled || props.disabled) return
    if (props.multiple) {
        const next = isSelected(option)
            ? selectedValues.value.filter(v => v !== option.value)
            : [...selectedValues.value, option.value]
        emit('update:modelValue', next)
        emit('change', next)
    }
    else {
        emit('update:modelValue', option.value)
        emit('change', option.value)
        close()
    }
}

function clear(): void {
    const next = props.multiple ? [] : ''
    emit('update:modelValue', next)
    emit('change', next)
}

const selectedOptions = computed(() =>
    props.options.filter(option => isSelected(option)))

const label = computed(() => selectedOptions.value.map(option => option.label).join(', '))

const grouped = computed(() => {
    const groups = new Map<string, MfSelectOption[]>()
    for (const option of props.options) {
        const key = option.group ?? ''
        if (!groups.has(key)) groups.set(key, [])
        groups.get(key)!.push(option)
    }
    return Array.from(groups.entries())
})

const heightClass = computed(() => CONTROL_SIZES[props.size].split(' ').find(c => c.startsWith('h-')) ?? 'h-9')

const triggerClass = computed(() => clsx(
    'mf-invalidable inline-flex w-full cursor-pointer items-center gap-2 rounded-mf-md border bg-surface pl-3 pr-2.5 text-sm',
    props.multiple ? 'min-h-9 py-1' : heightClass.value,
    TRANSITION,
    props.invalid ? 'border-danger' : open.value ? 'border-primary' : 'border-line',
    props.disabled && 'cursor-not-allowed opacity-60',
))
</script>

<template>
    <div ref="root" class="relative inline-flex w-full">
        <button type="button" :class="triggerClass" :disabled="disabled" @click="toggle">
            <span
                class="flex min-w-0 flex-1 flex-wrap items-center gap-1 text-left"
                :class="selectedOptions.length === 0 && 'text-content-placeholder'"
            >
                <template v-if="selectedOptions.length === 0">{{ placeholder }}</template>
                <template v-else-if="multiple">
                    <span
                        v-for="option in selectedOptions"
                        :key="String(option.value)"
                        class="inline-flex items-center gap-1 rounded-mf-sm bg-surface-active px-1.5 py-0.5 text-xs text-content"
                    >
                        {{ option.label }}
                    </span>
                </template>
                <template v-else>
                    <span class="truncate text-content">{{ label }}</span>
                </template>
            </span>
            <span
                v-if="clearable && selectedOptions.length > 0 && !disabled"
                class="inline-flex size-4 shrink-0 items-center justify-center rounded-mf-full text-content-tertiary hover:bg-surface-hover hover:text-content"
                @click.stop="clear"
            >
                <svg class="size-2.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18" /></svg>
            </span>
            <svg
                class="size-3.5 shrink-0 text-content-tertiary transition-transform duration-(--mf-duration-base) ease-(--mf-easing)"
                :class="open && 'rotate-180'"
                viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"
            >
                <path d="M6 9l6 6 6-6" />
            </svg>
        </button>

        <Transition
            enter-active-class="transition duration-(--mf-duration-fast) ease-(--mf-easing)"
            enter-from-class="opacity-0 scale-95"
            leave-active-class="transition duration-(--mf-duration-fast) ease-(--mf-easing)"
            leave-to-class="opacity-0 scale-95"
        >
            <ul
                v-if="open"
                role="listbox"
                class="absolute z-40 mt-1.5 max-h-64 w-full min-w-32 overflow-y-auto rounded-mf-lg border border-line bg-surface p-1 shadow-mf-lg"
            >
                <template v-for="[group, groupOptions] in grouped" :key="group || 'default'">
                    <li v-if="group" class="px-2.5 pt-1.5 pb-1 text-xs font-semibold text-content-tertiary">
                        {{ group }}
                    </li>
                    <li
                        v-for="option in groupOptions"
                        :key="String(option.value)"
                        role="option"
                        :aria-selected="isSelected(option)"
                        class="flex cursor-pointer items-center justify-between gap-2 rounded-mf-md px-2.5 py-1.5 text-sm"
                        :class="clsx(
                            option.disabled
                                ? 'cursor-not-allowed opacity-40'
                                : isSelected(option)
                                    ? 'bg-surface-active font-medium text-primary'
                                    : 'text-content-secondary hover:bg-surface-hover hover:text-content',
                        )"
                        @click="select(option)"
                    >
                        <span class="truncate">{{ option.label }}</span>
                        <svg v-if="isSelected(option)" class="size-3.5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
                            <path d="M5 13l4 4L19 7" />
                        </svg>
                    </li>
                </template>
                <li v-if="options.length === 0" class="px-2.5 py-2 text-sm text-content-tertiary">No data</li>
            </ul>
        </Transition>
    </div>
</template>
