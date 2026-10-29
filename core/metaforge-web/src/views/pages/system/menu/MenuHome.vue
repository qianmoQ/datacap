<template>

    <DataTable filterable :filter-fields="tableFilters" :title="$t('menu.common.list')"
               :columns="headers"
               :data-source="data"
               :loading="loading"
               :page-index="pageIndex"
               :page-size="pageSize"
               :total="dataCount"
               @refresh="handlerInitialize"
               @search="onSearch"
               @reset="onSearch"
               @page-change="onPageChange"
               @size-change="onSizeChange">
      <template #actions>
        <MfTooltip :content="$t('menu.common.create')">
          <MfButton variant="primary" @click="handlerChangeInfo(true, null)">
            <Plus :size="16" />
            {{ $t('menu.common.create') }}
          </MfButton>
        </MfTooltip>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <EllipsisCell :text="record.name"/>
        </template>
        <template v-else-if="column.key === 'description'">
          <EllipsisCell :text="record.description"/>
        </template>
        <template v-else-if="column.key === 'active'">
          <MfSwitch :model-value="record.active"
                    @update:model-value="(value: boolean) => onChangeActive(record, value)"/>
        </template>
        <template v-else-if="column.key === 'action'">
          <MfSpace :wrap="false" :size="2">
            <MfTooltip :content="$t('common.editData')">
              <MfButton variant="ghost" size="sm" shape="circle" @click="handlerChangeInfo(true, record)">
                <Pencil :size="15" />
              </MfButton>
            </MfTooltip>

            <MfTooltip :content="$t('menu.common.delete')">
              <MfButton variant="ghost" size="sm" shape="circle" @click="handlerChangeDelete(true, record)">
                <Trash2 :size="15" />
              </MfButton>
            </MfTooltip>
          </MfSpace>
        </template>
      </template>
    </DataTable>

  <MenuDelete v-if="dataDeleteVisible"
              :is-visible="dataDeleteVisible"
              :info="dataInfo"
              @close="handlerChangeDelete(false, null)"/>

  <MenuInfo v-if="dataInfoVisible"
            :is-visible="dataInfoVisible"
            :info="dataInfo"
            @close="handlerChangeInfo(false, null)"/>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { FilterModel, type TableFilters } from '@/model/filter'
import { useHeaders } from '@/views/pages/system/menu/MenuUtils'
import { MenuModel } from '@/model/menu'
import DataTable from '@/views/components/table/DataTable.vue'
import { useTableFilters } from '@/views/components/table/tableFilters'
import MenuInfo from '@/views/pages/system/menu/MenuInfo.vue'
import MenuDelete from '@/views/pages/system/menu/MenuDelete.vue'
import MenuService from '@/services/menu'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import { MfMessage } from 'metaforge-ui'
import { Pencil, Plus, Trash2 } from '@lucide/vue'

defineOptions({ name: 'MenuHome' })

const tableFilters = useTableFilters('menu')
const filter: FilterModel = new FilterModel()
const { headers } = useHeaders()

const loading = ref(false)
const dataInfoVisible = ref(false)
const dataDeleteVisible = ref(false)
const data = ref<any[]>([])
const pageIndex = ref(1)
const pageSize = ref(10)
const dataCount = ref(0)
const dataInfo = ref<MenuModel | null>(null)

const onSearch = (values: TableFilters) => {
  Object.assign(filter, values, { page: 1 })
  pageIndex.value = 1
  handlerInitialize()
}

let latestRequest = 0
const handlerInitialize = () => {
  const request = ++latestRequest
  loading.value = true
  MenuService.getAll({ ...filter })
             .then((response) => {
                 if (request !== latestRequest) return
               if (response.status) {
                 data.value = response.data.content
                 dataCount.value = response.data.total
                 pageSize.value = response.data.size
                 pageIndex.value = response.data.page
               }
             })
             .finally(() => { if (request === latestRequest) loading.value = false })
}

const fetchData = (value: number) => {
  filter.page = value
  filter.size = pageSize.value
  handlerInitialize()
}

const onPageChange = (value: number) => fetchData(value)

const onSizeChange = (_current: number, size: number) => {
  pageSize.value = size
  fetchData(pageIndex.value)
}

const onChangeActive = (record: any, value: boolean) => {
  MenuService.changeActive(record.code, value)
             .then((response) => {
               if (response.status) {
                 record.active = value
               }
               else {
                 MfMessage.error(response.message)
               }
             })
}

const handlerChangeDelete = (isOpen: boolean, info: MenuModel | null) => {
  dataDeleteVisible.value = isOpen
  dataInfo.value = info
  if (!isOpen) {
    handlerInitialize()
  }
}

const handlerChangeInfo = (isOpen: boolean, info: MenuModel | null) => {
  dataInfoVisible.value = isOpen
  dataInfo.value = info
  if (!isOpen) {
    handlerInitialize()
  }
}

onMounted(() => handlerInitialize())
</script>
