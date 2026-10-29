<template>
  <BaseLayout>
    <div class="relative min-h-screen overflow-hidden bg-surface">
      <!-- 品牌波浪背景 -->
      <svg aria-hidden="true" class="pointer-events-none absolute inset-x-0 top-0 h-[480px] w-full" viewBox="0 0 1440 480" preserveAspectRatio="none">
        <path d="M0 0h1440v72c-200 120-420 20-660 96C560 240 300 130 0 244Z" fill="var(--mf-color-primary)" fill-opacity="0.07"/>
        <path d="M0 148c220 96 470-28 720 66 250 94 520-8 720-96V0H0Z" fill="var(--mf-color-primary)" fill-opacity="0.05"/>
        <path d="M0 472c260-96 540 28 820-40 240-58 440 10 620-16V360c-200 84-420-20-660 44-240 64-520-40-780 40Z" fill="var(--mf-color-primary)" fill-opacity="0.04"/>
      </svg>

      <div class="relative flex min-h-screen flex-col items-center justify-center px-6 text-center">
        <span class="text-warning">
          <CircleAlert :size="96" :stroke-width="1.4"/>
        </span>
        <h1 class="mt-7 text-3xl font-semibold text-content">{{ $t('common.pageNotNetwork') }}</h1>
        <p class="mt-3 text-base text-content-secondary">{{ $t('common.tip.pageNotNetwork') }}</p>

        <div class="mt-9 flex items-center justify-center gap-3">
          <MfButton size="lg" @click="goBack">{{ $t('common.backTo') }}</MfButton>
          <MfButton size="lg" variant="primary" @click="$router.push('/')">{{ $t('common.backToHome') }}</MfButton>
        </div>
      </div>
    </div>
  </BaseLayout>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { CircleAlert } from '@lucide/vue'
import BaseLayout from '@/views/layouts/base/BaseLayout.vue'

defineOptions({ name: 'NotNetwork' })

const route = useRoute()
const router = useRouter()

// 返回：优先回到被中断的原始页面；没有历史时浏览器后退。
const goBack = () => {
  const redirect = route.query.redirect
  if (redirect) {
    router.push(String(redirect))
  }
  else {
    router.back()
  }
}
</script>
