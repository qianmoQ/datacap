<template>
  <BaseLayout>
    <div class="h-screen">
      <a-result status="500"
                :title="$t('common.pageNotNetwork')"
                :sub-title="$t('common.tip.pageNotNetwork')">
        <template #icon>
          <warning-outlined :style="{ fontSize: '80px', color: '#9ca3af' }"/>
        </template>
        <template #extra>
          <a-button @click="$router.push(String($route.query.redirect || '/'))">
            {{ $t('common.backTo') }}
          </a-button>
          <a-button type="primary" @click="$router.push(String($route.query.redirect || '/'))">
            {{ $t('common.backToHome') }}
          </a-button>
        </template>
      </a-result>
    </div>
  </BaseLayout>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import { WarningOutlined } from '@ant-design/icons-vue'
import BaseLayout from '@/views/layouts/base/BaseLayout.vue'

export default defineComponent({
  name: 'NotNetwork',
  components: { BaseLayout, WarningOutlined }
})
</script>
