<template>
  <ShadcnModal v-model="visible"
               width="60%"
               :title="`[ ${info?.name} ] ${$t('dataset.common.history')}`"
               @on-close="onCancel">
    <div class="relative">
      <ShadcnSpin v-if="loading" fixed/>

      <ShadcnTable size="small" :columns="historyHeaders" :data="data">
        <template #state="{ row }">
          <ShadcnHoverCard v-if="hasMessage(row)">
            <ShadcnTag :color="Common.getColor(row?.state)">
              {{ getStateText(row?.state) }}
            </ShadcnTag>

            <template #content>
              <div class="p-2 w-full overflow-x-auto">
                {{ row?.message }}
              </div>
            </template>
          </ShadcnHoverCard>

          <ShadcnTag v-else :color="Common.getColor(row?.state)">
            <span>{{ getStateText(row?.state) }}</span>
          </ShadcnTag>
        </template>

        <template #progress="{ row }">
          <div class="flex items-center gap-2 min-w-[120px]">
            <div class="flex-1 h-2 rounded bg-gray-200 dark:bg-gray-700 overflow-hidden">
              <div class="h-full bg-blue-500 transition-all"
                   :style="{ width: formatProgressWidth(row?.progress) }"/>
            </div>
            <span class="text-xs whitespace-nowrap">{{ formatProgressText(row?.progress) }}</span>
          </div>
        </template>

        <template #action="{ row }">
          <div class="flex gap-1">
            <ShadcnButton size="small" type="primary" @click="onViewLog(row)">
              {{ $t('dataset.history.viewLog') }}
            </ShadcnButton>
            <ShadcnButton v-if="isStoppable(row)"
                          size="small"
                          type="error"
                          :loading="stoppingId === row.id"
                          @click="onStop(row)">
              {{ $t('dataset.history.stop') }}
            </ShadcnButton>
          </div>
        </template>
      </ShadcnTable>

      <DatasetHistoryLogger v-if="loggerVisible"
                            :is-visible="loggerVisible"
                            :info="loggerInfo"
                            @close="onLoggerClose"/>

      <ShadcnPagination v-if="data?.length > 0"
                        v-model="pageIndex"
                        class="py-2"
                        show-total
                        show-sizer
                        :page-size="pageSize"
                        :total="dataCount"
                        :sizerOptions="[10, 20, 50]"
                        :prevText="$t('source.common.previousPage')"
                        :nextText="$t('source.common.nextPage')"
                        @on-change="onPageChange"
                        @on-prev="onPrevChange"
                        @on-next="onNextChange"
                        @on-change-size="onSizeChange"/>
    </div>
  </ShadcnModal>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import { FilterModel } from '@/model/filter'
import { useDatasetHeaders } from './DatasetUtils'
import DatasetService from '@/services/dataset'
import { DatasetModel } from '@/model/dataset'
import Common, { useUtil } from '@/utils/common'
import DatasetHistoryLogger from './DatasetHistoryLogger.vue'

export default defineComponent({
  name: 'DatasetHistory',
  components: { DatasetHistoryLogger },
  props: {
    isVisible: {
      type: Boolean,
      default: () => false
    },
    info: {
      type: Object as () => DatasetModel | null
    }
  },
  computed: {
    Common()
    {
      return Common
    },
    visible: {
      get(): boolean
      {
        return this.isVisible
      },
      set(value: boolean)
      {
        this.$emit('close', value)
      }
    }
  },
  setup()
  {
    const filter: FilterModel = new FilterModel()
    const { historyHeaders } = useDatasetHeaders()
    const { getText } = useUtil()

    return {
      filter,
      historyHeaders,
      getText
    }
  },
  data()
  {
    return {
      loading: false,
      data: [],
      pageIndex: 1,
      pageSize: 10,
      dataCount: 0,
      loggerVisible: false,
      loggerInfo: null as any,
      stoppingId: null as number | null
    }
  },
  created()
  {
    this.handleInitialize()
  },
  methods: {
    handleInitialize()
    {
      this.loading = true
      DatasetService.getHistory(this.info?.code as string, this.filter)
                    .then((response) => {
                      if (response.status) {
                        this.data = response.data.content
                        this.dataCount = response.data.total
                        this.pageSize = response.data.size
                        this.pageIndex = response.data.page
                      }
                    })
                    .finally(() => this.loading = false)
    },
    fetchData(value: number)
    {
      this.filter.page = value
      this.filter.size = this.pageSize
      this.handleInitialize()
    },
    onPageChange(value: number)
    {
      this.fetchData(value)
    },
    onPrevChange(value: number)
    {
      this.fetchData(value)
    },
    onNextChange(value: number)
    {
      this.fetchData(value)
    },
    onSizeChange(value: number)
    {
      this.pageSize = value
      this.fetchData(this.pageIndex)
    },
    onCancel()
    {
      this.visible = false
    },
    getStateText(origin: string): string
    {
      return this.getText(origin)
    },
    formatProgressWidth(value: number | string | null | undefined): string
    {
      if (value === null || value === undefined || value === '') return '0%'
      const v = typeof value === 'string' ? parseFloat(value) : value
      if (isNaN(v) || v < 0) return '0%'
      return Math.min(v, 100) + '%'
    },
    formatProgressText(value: number | string | null | undefined): string
    {
      if (value === null || value === undefined || value === '') return '-'
      const v = typeof value === 'string' ? parseFloat(value) : value
      if (isNaN(v)) return '-'
      return v.toFixed(2) + '%'
    },
    onViewLog(row: any)
    {
      this.loggerInfo = row
      this.loggerVisible = true
    },
    onLoggerClose()
    {
      this.loggerVisible = false
      this.loggerInfo = null
    },
    isStoppable(row: any): boolean
    {
      // STOPPING 状态已经在停了，不再展示按钮，防止重复点击
      return row?.state === 'RUNNING' || row?.state === 'CREATED'
    },
    hasMessage(row: any): boolean
    {
      // FAILURE / INTERRUPTED 都带 message，hover 显示原因
      return !!row?.message && (row?.state === 'FAILURE' || row?.state === 'INTERRUPTED')
    },
    onStop(row: any)
    {
      if (!row?.id) {
        return
      }
      this.stoppingId = row.id
      DatasetService.stopHistory(row.id)
                    .then(response => {
                      if (response.status) {
                        this.$Message.success({
                          content: this.$t('dataset.history.stopRequested'),
                          showIcon: true
                        })
                        this.handleInitialize()
                      }
                      else {
                        this.$Message.error({
                          content: response.message,
                          showIcon: true
                        })
                      }
                    })
                    .finally(() => this.stoppingId = null)
    }
  }
})
</script>
