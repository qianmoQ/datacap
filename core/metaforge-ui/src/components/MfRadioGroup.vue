<script setup lang="ts">
import { computed, provide, toRef } from 'vue'

const props = withDefaults(defineProps<{
    modelValue?: string | number
    disabled?: boolean
    name?: string
}>(), {
    modelValue: '',
    disabled: false,
})

const emit = defineEmits<{
    'update:modelValue': [value: string | number]
    change: [value: string | number]
}>()

provide('mfRadioGroup', {
    modelValue: computed(() => props.modelValue),
    disabled: toRef(props, 'disabled'),
    name: toRef(props, 'name'),
    select(value: string | number): void {
        emit('update:modelValue', value)
        emit('change', value)
    },
})
</script>

<template>
    <div class="inline-flex flex-wrap items-center gap-4" role="radiogroup">
        <slot />
    </div>
</template>
