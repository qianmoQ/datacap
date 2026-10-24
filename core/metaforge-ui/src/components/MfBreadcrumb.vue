<script setup lang="ts">
import { provide, ref } from 'vue'

provide('mfBreadcrumb', { separator: ref('/') })
</script>

<template>
    <nav class="flex flex-wrap items-center gap-1.5 text-sm text-content-tertiary" aria-label="breadcrumb">
        <slot />
    </nav>
</template>
