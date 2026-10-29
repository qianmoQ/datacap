<template>
  <BaseLayout>
    <div class="flex min-h-screen items-center justify-center px-6 py-10">
      <div class="w-full max-w-lg rounded-mf-lg border border-line bg-surface px-8 py-10 text-center shadow-mf-sm">
        <div class="mx-auto flex size-16 items-center justify-center rounded-mf-full bg-warning/10 text-warning">
          <WifiOff :size="28"/>
        </div>

        <h1 class="mt-5 text-xl font-semibold text-content">{{ $t('common.pageNotNetwork') }}</h1>
        <p class="mt-2 text-sm leading-6 text-content-secondary">{{ $t('common.tip.pageNotNetwork') }}</p>

        <div class="mt-6 space-y-1.5 rounded-mf-md border border-line bg-surface-hover/50 px-4 py-3 text-xs">
          <div class="flex items-center justify-between gap-3">
            <span class="shrink-0 text-content-tertiary">{{ $t('common.network.service') }}</span>
            <span class="min-w-0 truncate font-mono text-content" :title="serviceUrl">{{ serviceUrl }}</span>
          </div>
          <div class="flex items-center justify-between gap-3">
            <span class="shrink-0 text-content-tertiary">{{ $t('common.network.targetPage') }}</span>
            <span class="min-w-0 truncate font-mono text-content" :title="targetPath">{{ targetPath }}</span>
          </div>
        </div>

        <p v-if="restored" class="mt-4 text-xs text-success">{{ $t('common.network.restored') }}</p>
        <p v-else-if="lastChecked" class="mt-4 text-xs text-content-tertiary">
          {{ $t('common.network.lastChecked', { time: lastChecked }) }}
        </p>

        <div class="mt-6 flex items-center justify-center gap-2">
          <MfButton variant="primary" :loading="checking" @click="onRetry">
            <RefreshCw :size="14"/>
            {{ $t('common.network.retry') }}
          </MfButton>
          <MfButton @click="$router.push('/')">
            {{ $t('common.backToHome') }}
          </MfButton>
        </div>

        <p class="mt-4 text-xs text-content-tertiary">{{ $t('common.network.hint') }}</p>
      </div>
    </div>
  </BaseLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { RefreshCw, WifiOff } from '@lucide/vue'
import BaseLayout from '@/views/layouts/base/BaseLayout.vue'
import SetupService from '@/services/setup'

defineOptions({ name: 'NotNetwork' })

const route = useRoute()
const router = useRouter()

const targetPath = String(route.query.redirect || '/')
const serviceUrl = axios.defaults.baseURL || window.location.origin

const checking = ref(false)
const restored = ref(false)
const lastChecked = ref('')

// HttpUtils resolves transport failures with code 0; anything else means the server answered.
const onRetry = () => {
  checking.value = true
  restored.value = false
  SetupService.getStatus()
               .then(response => {
                 if (response && response.code !== 0) {
                   restored.value = true
                   lastChecked.value = new Date().toLocaleTimeString()
                   window.setTimeout(() => router.push(targetPath), 800)
                 }
                 else {
                   lastChecked.value = new Date().toLocaleTimeString()
                 }
               })
               .finally(() => (checking.value = false))
}
</script>
