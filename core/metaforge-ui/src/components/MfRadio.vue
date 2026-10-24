<script setup lang="ts">
import { computed, inject, ref, type Ref } from 'vue'
import clsx from 'clsx'
import { TRANSITION } from './common'

interface RadioGroupContext {
    modelValue: Ref<string | number>
    disabled: Ref<boolean>
    name: Ref<string | undefined>
    select: (value: string | number) => void
}

const props = withDefaults(defineProps<{
    value: string | number
    label?: string
    disabled?: boolean
}>(), {
    disabled: false,
})

const group = inject<RadioGroupContext | null>('mfRadioGroup', null)
const inputRef = ref<HTMLInputElement>()

const checked = computed(() => group ? group.modelValue.value === props.value : false)
const isDisabled = computed(() => props.disabled || (group?.disabled.value ?? false))
</script>

<template>
    <label
        class="inline-flex cursor-pointer items-center gap-2 text-sm text-content"
        :class="isDisabled && 'cursor-not-allowed opacity-60'"
        @click.prevent="group?.select(value)"
    >
        <span
            class="inline-flex size-4.5 items-center justify-center rounded-mf-full border shadow-mf-xs"
            :class="clsx(
                TRANSITION,
                checked ? 'border-primary' : 'border-line-strong bg-surface',
            )"
        >
            <span
                class="size-2 rounded-mf-full bg-primary transition-[transform,opacity] duration-(--mf-duration-fast) ease-(--mf-easing)"
                :class="checked ? 'scale-100 opacity-100' : 'scale-0 opacity-0'"
            />
        </span>
        <input
            ref="inputRef"
            type="radio"
            class="sr-only"
            :checked="checked"
            :disabled="isDisabled"
            :name="group?.name.value"
        >
        <span v-if="label">{{ label }}</span>
        <slot v-else />
    </label>
</template>
