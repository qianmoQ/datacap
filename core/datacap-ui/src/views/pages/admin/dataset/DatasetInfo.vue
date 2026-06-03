<template>
  <ShadcnCard :title="$t('common.dataset')" :border="false">
    <template #extra>
      <ShadcnButton :disabled="!data?.data.columns" @click="configureVisible = true">
        {{ $t('common.configure') }}
      </ShadcnButton>
    </template>

    <div class="relative h-full space-y-4" style="min-height: 200px;">
      <ShadcnSpin v-model="loading" fixed/>

      <ShadcnCard v-if="sourceInfo" class="w-full">
        <template #title>
          <ShadcnButton :loading="running" :disabled="running" @click="onRun()">
            {{ $t('query.common.execute') }}
          </ShadcnButton>
        </template>

        <div class="relative">
          <ShadcnSpin v-model="running" fixed/>

          <AceEditor :value="value" @update:value="value = $event"/>
        </div>
      </ShadcnCard>

      <div v-if="data || code" class="relative">
        <ShadcnSpin v-model="running" fixed/>

        <AgGridVue v-if="data?.data.columns"
                   :style="{height: '300px'}"
                   class="ag-theme-datacap"
                   :pagination="true"
                   :columnDefs="columnDefs"
                   :rowData="data.data.columns"
                   :gridOptions="gridOptions as any"/>
      </div>

      <div v-if="!sourceInfo" class="mt-3 justify-center items-center">
        <div class="flex flex-col items-center space-y-4">
          <ShadcnAlert :title="i18n.t('dataset.common.onlyPreviewCreate')"/>

          <ShadcnButton>
            <RouterLink to="/admin/query">
              {{ i18n.t('dataset.common.returnQuery') }}
            </RouterLink>
          </ShadcnButton>
        </div>
      </div>
    </div>
  </ShadcnCard>

  <ShadcnDrawer v-model="configureVisible"
                height="70%"
                :closable="false"
                position="bottom">
    <template #header>
      <div class="w-full flex items-center">
        <div class="flex-none">
          {{ $t('common.configure') }}
        </div>

        <div class="flex-1 flex items-center justify-end space-x-2">
          <ShadcnButton @click="onSubmit">
            {{ code ? $t('dataset.common.modify') : $t('dataset.common.create') }}
          </ShadcnButton>

          <ShadcnButton type="default" @click="configureVisible = false">
            {{ $t('common.cancel') }}
          </ShadcnButton>
        </div>
      </div>
    </template>

    <ShadcnAlert v-if="validator" type="error" class="mt-2">{{ validatorMessage }}</ShadcnAlert>

    <ShadcnTab v-model="activeTab" class="mt-1">
      <ShadcnTabItem :label="$t('dataset.common.dataColumn')" class="space-y-2" value="columns">
        <ShadcnSpin v-model="loading" fixed/>

        <ShadcnRow gutter="8">
          <ShadcnCol span="1" class="text-center">{{ $t('dataset.common.columnName') }}</ShadcnCol>
          <ShadcnCol span="1" class="text-center">{{ $t('dataset.common.columnAlias') }}</ShadcnCol>
          <ShadcnCol span="1" class="text-center">{{ $t('dataset.common.columnType') }}</ShadcnCol>
          <ShadcnCol span="1" class="text-center">{{ $t('dataset.common.columnMode') }}</ShadcnCol>
          <ShadcnCol span="1" class="text-center">{{ $t('dataset.common.columnDefaultValue') }}</ShadcnCol>
          <ShadcnCol span="1" class="text-center">{{ $t('dataset.common.columnIsNullable') }}</ShadcnCol>
          <ShadcnCol span="1" class="text-center">{{ $t('dataset.common.columnIsOrderByKey') }}</ShadcnCol>
          <ShadcnCol span="1" class="text-center">{{ $t('dataset.common.columnIsPartitionKey') }}</ShadcnCol>
          <ShadcnCol span="1" class="text-center">{{ $t('dataset.common.columnIsPrimaryKey') }}</ShadcnCol>
          <ShadcnCol span="1" class="text-center">{{ $t('dataset.common.columnIsSampling') }}</ShadcnCol>
          <ShadcnCol span="1" class="text-center">{{ $t('dataset.common.columnLength') }}</ShadcnCol>
          <ShadcnCol span="1" class="text-center">{{ $t('common.action') }}</ShadcnCol>
        </ShadcnRow>

        <ShadcnRow gutter="8">
          <template v-for="(item, index) in formState.columns" :key="index">
            <ShadcnCol span="1" class="flex justify-center">
              <ShadcnInput v-model="item.name"/>
            </ShadcnCol>

            <ShadcnCol span="1" class="flex justify-center">
              <ShadcnInput v-model="item.aliasName"/>
            </ShadcnCol>

            <ShadcnCol span="1" class="flex justify-center">
              <ShadcnSelect v-model="item.type" placeholder="Options" class="w-full">
                <template #options>
                  <ShadcnSelectOption value="STRING" :label="$t('dataset.common.columnTypeString')"/>
                  <ShadcnSelectOption value="NUMBER" :label="$t('dataset.common.columnTypeNumber')"/>
                  <ShadcnSelectOption value="NUMBER_SIGNED" :label="$t('dataset.common.columnTypeNumberSigned')"/>
                  <ShadcnSelectOption value="BOOLEAN" :label="$t('dataset.common.columnTypeBoolean')"/>
                  <ShadcnSelectOption value="DATETIME" :label="$t('dataset.common.columnTypeDateTime')"/>
                </template>
              </ShadcnSelect>
            </ShadcnCol>

            <ShadcnCol span="1" class="flex justify-center items-center">
              <ShadcnSwitch v-model="item.mode" true-value="METRIC" false-value="DIMENSION">
                <template #open>{{ $t('dataset.common.columnModeDimension') }}</template>
                <template #close>{{ $t('dataset.common.columnModeMetric') }}</template>
              </ShadcnSwitch>
            </ShadcnCol>

            <ShadcnCol span="1" class="flex justify-center">
              <ShadcnInput v-model="item.defaultValue" :disabled="item.virtualColumn"/>
            </ShadcnCol>

            <ShadcnCol span="1" class="flex justify-center items-center">
              <ShadcnSwitch v-model="item.nullable" :disabled="item.virtualColumn" @on-change="setNullable(item, $event)"/>
            </ShadcnCol>

            <ShadcnCol span="1" class="flex justify-center items-center">
              <ShadcnSwitch v-model="item.orderByKey" :disabled="item.virtualColumn" @on-change="setOrderByKey(item, $event)"/>
            </ShadcnCol>

            <ShadcnCol span="1" class="flex justify-center items-center">
              <ShadcnSwitch v-model="item.partitionKey" :disabled="item.virtualColumn" @on-change="setPartitionKey(item, $event)"/>
            </ShadcnCol>

            <ShadcnCol span="1" class="flex justify-center items-center">
              <ShadcnSwitch v-model="item.primaryKey" :disabled="item.virtualColumn" @on-change="setPrimaryKey(item, $event)"/>
            </ShadcnCol>

            <ShadcnCol span="1" class="flex justify-center items-center">
              <ShadcnSwitch v-model="item.samplingKey" :disabled="item.virtualColumn" @on-change="setSamplingKey(item, $event)"/>
            </ShadcnCol>

            <ShadcnCol span="1" class="flex justify-center">
              <ShadcnNumber v-model="item.length" type="number" :disabled="item.type === 'BOOLEAN' || item.type === 'DATETIME' || item.virtualColumn"/>
            </ShadcnCol>

            <ShadcnCol span="1" class="flex justify-center items-center space-x-1">
              <ShadcnHoverCard position="bottom">
                <ShadcnButton circle size="small">
                  <ShadcnIcon icon="Pencil" size="10"/>
                </ShadcnButton>

                <template #content>
                  <div class="p-2 space-y-2">
                    <ShadcnText type="h6">{{ $t('dataset.common.columnComment') }}</ShadcnText>
                    <ShadcnInput v-model="item.comment" type="textarea"/>
                  </div>
                </template>
              </ShadcnHoverCard>

              <ShadcnButton circle
                            type="error"
                            size="small"
                            :disabled="!item.customColumn"
                            @click="onRemoveColumn(index)">
                <ShadcnIcon icon="Trash" size="10"/>
              </ShadcnButton>

              <ShadcnButton circle size="small" @click="onAddColumn(index)">
                <ShadcnIcon icon="Plus" size="10"/>
              </ShadcnButton>
            </ShadcnCol>
          </template>
        </ShadcnRow>
      </ShadcnTabItem>

      <ShadcnTabItem :label="$t('dataset.common.dataConfigure')" value="configure">
        <ShadcnForm class="w-[40%] mx-auto items-center" v-model="formState">
          <ShadcnRow gutter="8">
            <ShadcnCol span="6">
              <ShadcnFormItem name="name" :label="$t('common.name')" :rules="[{ required: true, message: $t('common.name') }]">
                <ShadcnInput v-model="formState.name" name="name"/>
              </ShadcnFormItem>
            </ShadcnCol>

            <ShadcnCol span="6">
              <ShadcnFormItem name="executor" :label="$t('common.executor')">
                <ShadcnSelect v-model="formState.executor" name="executor">
                  <template #options>
                    <ShadcnSelectOption v-for="item in executors" :label="item.name" :value="item.name"/>
                  </template>
                </ShadcnSelect>
              </ShadcnFormItem>
            </ShadcnCol>

            <ShadcnCol span="6">
              <ShadcnFormItem name="syncMode" :label="$t('dataset.common.syncMode')">
                <ShadcnSelect v-model="formState.syncMode" name="syncMode">
                  <template #options>
                    <ShadcnSelectOption value="MANUAL" :label="$t('dataset.common.syncModeManual')"/>
                    <ShadcnSelectOption value="TIMING" :label="$t('dataset.common.syncModeTiming')"/>
                    <ShadcnSelectOption value="OUT_SYNC" :label="$t('dataset.common.syncModeOutSync')"/>
                  </template>
                </ShadcnSelect>
              </ShadcnFormItem>
            </ShadcnCol>

            <ShadcnCol span="6">
              <ShadcnFormItem name="expression" :label="$t('dataset.common.columnExpression')">
                <ShadcnInput v-model="formState.expression" name="expression" :disabled="formState.syncMode !== 'TIMING'"/>
              </ShadcnFormItem>
            </ShadcnCol>

            <ShadcnCol span="6">
              <ShadcnFormItem name="scheduler" :label="$t('common.scheduler')">
                <ShadcnSelect v-model="formState.scheduler" name="scheduler" :disabled="formState.syncMode !== 'TIMING'">
                  <template #options>
                    <ShadcnSelectOption v-for="item in schedulers" :label="item.name" :value="item.name"/>
                  </template>
                </ShadcnSelect>
              </ShadcnFormItem>
            </ShadcnCol>

            <ShadcnCol span="12">
              <ShadcnDivider/>
            </ShadcnCol>

            <ShadcnCol span="12">
              <ShadcnAlert v-if="formState.columns.filter(item => item.type === 'DATETIME').length === 0" type="error">
                {{ $t('dataset.tip.lifeCycleMustDateColumn') }}
              </ShadcnAlert>
            </ShadcnCol>

            <ShadcnCol span="12">
              <ShadcnFormItem name="lifeCycleColumn" :label="$t('dataset.common.lifeCycleColumn')" :description="$t('dataset.tip.lifeCycle')">
                <ShadcnSelect v-model="formState.lifeCycleColumn" name="lifeCycleColumn"
                              :disabled="formState.columns.filter(item => item.type === 'DATETIME').length === 0">
                  <template #options>
                    <ShadcnSelectOption v-for="item in formState.columns.filter(v => v.type === 'DATETIME')" :label="item.name" :value="item.name"/>
                  </template>
                </ShadcnSelect>
              </ShadcnFormItem>
            </ShadcnCol>

            <ShadcnCol span="6">
              <ShadcnFormItem name="lifeCycleType" :label="$t('dataset.common.lifeCycleType')">
                <ShadcnSelect v-model="formState.lifeCycleType" name="lifeCycleType" :disabled="!formState.lifeCycleColumn">
                  <template #options>
                    <ShadcnSelectOption value="MONTH" :label="$t('dataset.common.lifeCycleMonth')"/>
                    <ShadcnSelectOption value="WEEK" :label="$t('dataset.common.lifeCycleWeek')"/>
                    <ShadcnSelectOption value="DAY" :label="$t('dataset.common.lifeCycleDay')"/>
                    <ShadcnSelectOption value="HOUR" :label="$t('dataset.common.lifeCycleHour')"/>
                  </template>
                </ShadcnSelect>
              </ShadcnFormItem>
            </ShadcnCol>

            <ShadcnCol span="6">
              <ShadcnFormItem name="lifeCycle" :label="$t('dataset.common.lifeCycleNumber')">
                <ShadcnNumber v-model="formState.lifeCycle" name="lifeCycle" min="1" :disabled="!formState.lifeCycleColumn"/>
              </ShadcnFormItem>
            </ShadcnCol>

            <ShadcnCol span="12">
              <ShadcnDivider/>
            </ShadcnCol>

            <ShadcnCol span="12">
              <ShadcnFormItem name="description" :label="$t('common.description')">
                <ShadcnInput v-model="formState.description" type="textarea" name="description"/>
              </ShadcnFormItem>
            </ShadcnCol>
          </ShadcnRow>
        </ShadcnForm>
      </ShadcnTabItem>
    </ShadcnTab>
  </ShadcnDrawer>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import { useI18n } from 'vue-i18n'
import GridOptions from '@/views/components/grid/GridOptions'
import DatasetService from '@/services/dataset'
import { HttpUtils } from '@/utils/http'
import { GridColumn } from '@/views/components/grid/GridColumn'
import PluginService from '@/services/plugin'
import { AgGridVue } from 'ag-grid-vue3'
import 'ag-grid-community/styles/ag-grid.css'
import '@/views/components/grid/ag-theme-datacap.css'
import { DatasetModel } from '@/model/dataset'
import { ResponseModel } from '@/model/response.ts'
import AceEditor from '@/views/components/editor/AceEditor.vue'
import { SourceModel } from '@/model/source.ts'
import SourceService from '@/services/source'
import ExecuteService from '@/services/execute'
import { ExecuteModel } from '@/model/execute.ts'
import { ArrayUtils } from '@/utils/array.ts'
import { join } from 'lodash'

export default defineComponent({
  name: 'DatasetInfo',
  components: { AceEditor, AgGridVue },
  setup()
  {
    const i18n = useI18n()
    const gridOptions = GridOptions.createDefaultOptions(i18n)

    return {
      i18n,
      gridOptions
    }
  },
  data()
  {
    return {
      code: null as string | null,
      loading: false,
      saving: false,
      validator: false,
      validatorMessage: null as string | null,
      columnDefs: [] as GridColumn[],
      schedulers: [],
      executors: [],
      configureVisible: false,
      formState: {
        id: null,
        name: null as string | null | undefined,
        description: null as string | null | undefined,
        query: null as string | null,
        syncMode: 'MANUAL',
        columns: [] as any[],
        source: { code: null },
        expression: null as string | null,
        scheduler: 'Default',
        executor: 'Local',
        lifeCycle: null as number | null,
        lifeCycleColumn: null as string | null,
        lifeCycleType: null as string | null
      },
      data: null as ResponseModel | null,
      sourceInfo: null as SourceModel | null,
      value: '',
      running: false,
      activeTab: 'columns'
    }
  },
  created()
  {
    this.handleInitialize()
  },
  methods: {
    handleInitialize()
    {
      setTimeout(() => {
        PluginService.getPlugins()
                     .then(response => {
                       if (response.status) {
                         this.schedulers = response.data.filter((v: { type: string }) => v.type === 'SCHEDULER')
                         this.executors = response.data.filter((v: { type: string }) => v.type === 'EXECUTOR')
                       }
                     })
        const code = this.$route.params.code
        const sourceCode = this.$route.params.sourceCode

        if (code) {
          this.loading = true
          this.code = code as string
          const axios = new HttpUtils().getAxios()
          axios.all([DatasetService.getByCode(this.code), DatasetService.getColumnsByCode(this.code)])
               .then(axios.spread((info, column) => {
                 if (info.status) {
                   this.formState = info.data
                   this.formState.source.code = info.data.source.code
                   this.sourceInfo = info.data.source
                   this.value = info.data.query
                   this.onRun()
                 }
                 if (column.status) {
                   this.formState.columns = column.data
                 }
               }))
               .finally(() => this.loading = false)
        }
        else if (sourceCode) {
          const tempId = this.$route.query.tempId
          if (tempId) {
            this.value = localStorage.getItem(`QueryContent_${ tempId }`) || ''
            localStorage.removeItem(`QueryContent_${ tempId }`)
          }
          this.loading = true
          SourceService.getByCode(sourceCode as string)
                       .then(response => {
                         if (response.status) {
                           this.sourceInfo = response.data
                           this.formState.source.code = response.data.code
                         }
                       })
                       .finally(() => this.loading = false)
        }
      })
    },
    onSubmit()
    {
      if (!this.beforeCheck()) {
        this.saving = true
        this.formState.query = this.value
        DatasetService.saveOrUpdate(this.formState as unknown as DatasetModel)
                      .then(response => {
                        if (response.status) {
                          this.$Message.success({
                            content: this.$t('dataset.tip.publishSuccess').replace('$VALUE', this.formState.name as string),
                            showIcon: true
                          })

                          this.$router.push('/admin/dataset')
                        }
                      })
                      .finally(() => this.saving = false)
      }
    },
    onAddColumn(index: number)
    {
      this.formState.columns.splice(index + 1, 0, {
        id: null,
        name: null,
        aliasName: null,
        type: 'STRING',
        comment: null,
        defaultValue: null,
        position: index + 1,
        nullable: false,
        length: 0,
        original: null,
        orderByKey: false,
        partitionKey: false,
        primaryKey: false,
        samplingKey: false,
        mode: 'DIMENSION',
        virtualColumn: true,
        customColumn: true
      })
    },
    onRemoveColumn(index: number)
    {
      this.formState.columns.splice(index, 1)
    },
    onRun()
    {
      const configure: ExecuteModel = {
        content: this.value,
        name: this.sourceInfo?.code as unknown as string,
        mode: 'DATASET'
      }
      this.running = true
      ExecuteService.execute(configure, null)
                    .then((response) => {
                      if (response.status) {
                        this.data = response
                        this.columnDefs = []
                        response.data?.headers.forEach((header: any) => {
                          const columnDef: GridColumn = { headerName: header, field: header }
                          this.columnDefs.push(columnDef)
                        })
                        if (this.formState.columns.length === 0) {
                          response.data?.headers.map((header: any, index: number) => {
                            const column = {
                              id: null,
                              name: `column_${ index + 1 }`,
                              aliasName: header.replace('(', '_').replace(')', ''),
                              type: 'STRING',
                              comment: header,
                              defaultValue: null,
                              position: index,
                              nullable: false,
                              length: 0,
                              original: header,
                              orderByKey: false,
                              partitionKey: false,
                              primaryKey: false,
                              samplingKey: false,
                              mode: 'DIMENSION',
                              virtualColumn: false,
                              customColumn: false
                            }
                            this.formState.columns.push(column)
                          })
                        }
                      }
                      else {
                        this.$Message.error({
                          content: response.message,
                          showIcon: true
                        })
                      }
                    })
                    .finally(() => this.running = false)
    },
    beforeCheck(): boolean
    {
      const duplicateColumns = ArrayUtils.findDuplicates(this.formState.columns)
      if (duplicateColumns.length > 0) {
        this.validator = true
        this.validatorMessage = this.$t('dataset.validator.duplicateColumn').replace('$VALUE', join(duplicateColumns, ','))
        return true
      }

      const orderByColumns = this.formState.columns.filter(item => item.orderByKey)
      const primaryKeyColumns = this.formState.columns.filter(item => item.primaryKey)
      if (orderByColumns.length === 0 && primaryKeyColumns.length === 0) {
        this.validator = true
        this.validatorMessage = this.$t('dataset.validator.specifiedColumn')
        return true
      }

      if (!this.formState.name) {
        this.validator = true
        this.validatorMessage = this.$t('dataset.validator.specifiedName')
        return true
      }
      return false
    },
    validatorSampling()
    {
      const samplingColumns = this.formState.columns
                                  .filter((item: { samplingKey: boolean; }) => item.samplingKey)
      if (samplingColumns.length === 0) {
        this.validator = false
        this.validatorMessage = null
        return
      }

      const orderByColumns = this.formState.columns
                                 .filter((item: { orderByKey: boolean; }) => item.orderByKey)
      const isNameInOrderByColumns = samplingColumns.every((samplingItem: { name: string; }) => {
        return orderByColumns.some((orderByItem: { name: string; }) => orderByItem.name === samplingItem.name)
      })
      if (!isNameInOrderByColumns) {
        this.validator = true
        this.validatorMessage = this.$t('dataset.tip.validatorSampling') as string
      }
      else {
        this.validator = false
        this.validatorMessage = null
      }
    },
    setNullable(item: any, checked: boolean)
    {
      item.nullable = checked
    },
    setOrderByKey(item: any, checked: boolean)
    {
      item.orderByKey = checked
      this.validatorSampling()
    },
    setPartitionKey(item: any, checked: boolean)
    {
      item.partitionKey = checked
      this.validatorSampling()
    },
    setPrimaryKey(item: any, checked: boolean)
    {
      item.primaryKey = checked
      this.validatorSampling()
    },
    setSamplingKey(item: any, checked: boolean)
    {
      item.samplingKey = checked
      this.validatorSampling()
    }
  }
})
</script>
