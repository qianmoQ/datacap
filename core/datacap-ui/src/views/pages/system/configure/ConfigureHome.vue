<template>
  <ShadcnCard>
    <template #title>
      <div class="ml-2 font-normal text-sm">{{ $t('configure.runtime.title') }}</div>
    </template>

    <div class="grid grid-cols-12 gap-3">
      <!-- left: category + rows -->
      <div class="col-span-3 border-r pr-2 space-y-2">
        <div v-for="cat in categories" :key="cat.value">
          <div class="text-xs font-medium text-gray-500 mb-1">{{ cat.label }}</div>
          <div v-for="row in (groupedRows[cat.value] || [])" :key="row.id"
               class="px-2 py-1 rounded text-xs cursor-pointer"
               :class="selected && selected.name === row.name && selected.category === cat.value
                       ? 'bg-blue-100 dark:bg-blue-900'
                       : 'hover:bg-gray-100 dark:hover:bg-gray-800'"
               @click="onSelect(cat.value, row.name)">
            {{ row.name }}
          </div>
          <div v-if="!(groupedRows[cat.value] || []).length" class="text-xs text-gray-400 italic ml-1">
            {{ $t('configure.runtime.empty') }}
          </div>
        </div>
      </div>

      <!-- right: detail form -->
      <div class="col-span-9 pl-2">
        <ShadcnSpin v-if="loading" fixed/>

        <div v-if="!selected" class="text-xs text-gray-500 italic">
          {{ $t('configure.runtime.selectHint') }}
        </div>

        <div v-else class="space-y-3">
          <div class="text-sm font-medium">{{ selected.category }} / {{ selected.name }}</div>

          <div v-for="field in schema" :key="field.name" class="space-y-1">
            <label class="text-xs">
              <span>{{ field.name }}</span>
              <span v-if="!field.tunable" class="ml-1 text-xs text-orange-500">[{{ $t('configure.runtime.adminOnly') }}]</span>
            </label>
            <div v-if="field.description" class="text-xs text-gray-500">{{ field.description }}</div>

            <ShadcnInput v-if="field.type === 'STRING'" v-model="form[field.name]"/>
            <ShadcnInputNumber v-else-if="field.type === 'NUMBER'" v-model="form[field.name]"/>
            <ShadcnSwitch v-else-if="field.type === 'BOOLEAN'"
                          v-model="booleanProxies[field.name]"
                          @on-change="(v: boolean) => onBoolChange(field.name, v)"/>
            <ShadcnInput v-else-if="field.type === 'PASSWORD'" type="password" v-model="form[field.name]"/>
          </div>

          <div class="pt-2">
            <ShadcnButton type="primary" :loading="saving" @click="onSave">
              {{ $t('common.save') }}
            </ShadcnButton>
          </div>
        </div>
      </div>
    </div>
  </ShadcnCard>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import RuntimeConfigureService from '@/services/runtimeConfigure'

interface PluginConfigureField {
  name: string
  type: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'PASSWORD'
  defaultValue: string
  description: string
  tunable: boolean
}

interface ConfigureRow {
  id: number
  name: string
  category: string
}

const CATEGORY_KEYS = ['EXECUTOR', 'DATASET']

export default defineComponent({
  name: 'ConfigureHome',
  computed: {
    categories(): Array<{ value: string; label: string }>
    {
      return [
        { value: 'EXECUTOR', label: this.$t('configure.runtime.categoryExecutor') as string },
        { value: 'DATASET', label: this.$t('configure.runtime.categoryDataset') as string }
      ]
    }
  },
  data()
  {
    return {
      loading: false,
      saving: false,
      groupedRows: {} as Record<string, ConfigureRow[]>,
      selected: null as { category: string; name: string } | null,
      schema: [] as PluginConfigureField[],
      form: {} as Record<string, string>,
      booleanProxies: {} as Record<string, boolean>
    }
  },
  created()
  {
    this.loadAll()
  },
  methods: {
    async loadAll()
    {
      this.loading = true
      try {
        for (const c of CATEGORY_KEYS) {
          const response = await RuntimeConfigureService.list(c)
          this.groupedRows[c] = response.status ? (response.data || []) : []
        }
      }
      finally {
        this.loading = false
      }
    },
    onSelect(category: string, name: string)
    {
      this.selected = { category, name }
      this.loading = true
      RuntimeConfigureService.detail(category, name)
                             .then(response => {
                               if (response.status && response.data) {
                                 this.schema = response.data.schema || []
                                 this.form = {}
                                 this.booleanProxies = {}
                                 const values = response.data.values || {}
                                 for (const f of this.schema) {
                                   this.form[f.name] = values[f.name] ?? f.defaultValue ?? ''
                                   if (f.type === 'BOOLEAN') {
                                     this.booleanProxies[f.name] = (this.form[f.name] + '').toLowerCase() === 'true'
                                   }
                                 }
                               }
                               else {
                                 this.$Message.error({ content: response.message, showIcon: true })
                               }
                             })
                             .finally(() => this.loading = false)
    },
    onBoolChange(name: string, value: boolean)
    {
      this.form[name] = value ? 'true' : 'false'
    },
    onSave()
    {
      if (!this.selected) return
      // sync booleans
      for (const f of this.schema) {
        if (f.type === 'BOOLEAN') {
          this.form[f.name] = this.booleanProxies[f.name] ? 'true' : 'false'
        }
      }
      this.saving = true
      RuntimeConfigureService.save(this.selected.category, this.selected.name, this.form)
                             .then(response => {
                               if (response.status) {
                                 this.$Message.success({ content: this.$t('common.successfully'), showIcon: true })
                                 this.loadAll()
                               }
                               else {
                                 this.$Message.error({ content: response.message, showIcon: true })
                               }
                             })
                             .finally(() => this.saving = false)
    }
  }
})
</script>
