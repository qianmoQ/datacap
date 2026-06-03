<template>
  <ShadcnModal v-model="visible"
               width="60%"
               height="60%"
               :title="$t('dataset.history.logger')"
               @on-close="onCancel">
    <ShadcnSpin v-model="loading" fixed/>

    <div v-if="!loading" class="flex items-center gap-3 pb-2">
      <ShadcnSwitch v-model="autoRefresh"
                    :disabled="!isRunning"
                    @on-change="onAutoRefreshChange"/>
      <span class="text-xs">{{ $t('dataset.history.autoRefresh') }}</span>
      <span class="text-xs text-gray-500" v-if="isRunning && autoRefresh">
        ({{ $t('dataset.history.refreshInterval', { seconds: refreshSeconds }) }})
      </span>
    </div>

    <ShadcnLogger v-if="!loading"
                  height="380"
                  toolbar
                  :items="logs"
                  :custom-patterns="customPatterns"/>

    <template #footer>
      <ShadcnButton type="default" @click="onCancel">
        {{ $t('common.cancel') }}
      </ShadcnButton>
    </template>
  </ShadcnModal>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import DatasetService from '@/services/dataset'

export default defineComponent({
  name: 'DatasetHistoryLogger',
  computed: {
    visible: {
      get(): boolean
      {
        return this.isVisible
      },
      set(value: boolean)
      {
        this.$emit('close', value)
      }
    },
    isRunning(): boolean
    {
      const state = this.info?.state
      return state === 'RUNNING' || state === 'CREATED' || state === 'QUEUE'
    }
  },
  props: {
    isVisible: {
      type: Boolean
    },
    info: {
      type: Object as () => any | null
    }
  },
  data()
  {
    return {
      loading: false,
      logs: Array<string>(),
      autoRefresh: false,
      refreshSeconds: 3,
      timer: null as any,
      customPatterns: {
        timestamp: [/^(\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2},\d{3})/],
        level: [/\b(INFO|ERROR|WARN|DEBUG)\b/],
        thread: [/\[(pool-\d+-thread-\d+)\]/],
        file: [/\[([^[\]]+\.kt:\d+)\]/, /\[([^[\]]+\.java:\d+)\]/]
      }
    }
  },
  created()
  {
    // RUNNING 任务默认打开自动刷新
    this.autoRefresh = this.isRunning
    this.handleInitialize(true)
  },
  beforeUnmount()
  {
    this.stopTimer()
  },
  watch: {
    isVisible(value: boolean)
    {
      if (!value) {
        this.stopTimer()
      }
    }
  },
  methods: {
    handleInitialize(showSpin: boolean)
    {
      if (!this.info?.id) {
        return
      }
      if (showSpin) {
        this.loading = true
      }
      DatasetService.getHistoryLog(this.info.id)
                    .then(response => {
                      if (response.status) {
                        this.logs = response.data || []
                      }
                      else if (showSpin) {
                        this.$Message.error({
                          content: response.message,
                          showIcon: true
                        })
                      }
                    })
                    .finally(() => {
                      this.loading = false
                      if (this.autoRefresh && this.isRunning) {
                        this.scheduleNext()
                      }
                    })
    },
    scheduleNext()
    {
      this.stopTimer()
      this.timer = setTimeout(() => this.handleInitialize(false), this.refreshSeconds * 1000)
    },
    stopTimer()
    {
      if (this.timer) {
        clearTimeout(this.timer)
        this.timer = null
      }
    },
    onAutoRefreshChange(value: boolean)
    {
      this.autoRefresh = value
      if (value && this.isRunning) {
        this.scheduleNext()
      }
      else {
        this.stopTimer()
      }
    },
    onCancel()
    {
      this.stopTimer()
      this.visible = false
    }
  }
})
</script>
