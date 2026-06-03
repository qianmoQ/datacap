<template>
  <ShadcnModal v-model="visible" :title="`[ ${ info?.name } ] ${ $t('dataset.common.syncData') }`" @on-close="onCancel">
    <ShadcnAlert type="error" :title="$t('dataset.tip.syncData')"/>

    <div v-if="loadingFields" class="py-2 text-xs text-gray-500">{{ $t('common.loading') }}</div>

    <div v-else-if="fields.length > 0" class="mt-3 space-y-3">
      <div class="text-xs font-medium">{{ $t('dataset.sync.overrideTitle') }}</div>
      <div v-for="field in fields" :key="field.name" class="space-y-1">
        <label class="text-xs">{{ field.name }}</label>
        <div v-if="field.description" class="text-xs text-gray-500">{{ field.description }}</div>

        <ShadcnInput v-if="field.type === 'STRING'" v-model="overrides[field.name]"/>
        <ShadcnInputNumber v-else-if="field.type === 'NUMBER'" v-model="overrides[field.name]"/>
        <ShadcnSwitch v-else-if="field.type === 'BOOLEAN'" v-model="booleanProxies[field.name]"
                      @on-change="(v: boolean) => onBoolChange(field.name, v)"/>
        <ShadcnInput v-else-if="field.type === 'PASSWORD'" type="password" v-model="overrides[field.name]"/>
      </div>
    </div>

    <template #footer>
      <ShadcnSpace>
        <ShadcnButton type="default" @click="onCancel">{{ $t('common.cancel') }}</ShadcnButton>

        <ShadcnButton :disabled="loading" :loading="loading" @click="onSubmit">
          {{ $t('dataset.common.syncData') }}
        </ShadcnButton>
      </ShadcnSpace>
    </template>
  </ShadcnModal>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import DatasetService from '@/services/dataset'
import { DatasetModel } from '@/model/dataset'

interface PluginConfigureField {
  name: string
  type: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'PASSWORD'
  defaultValue: string
  description: string
  tunable: boolean
}

export default defineComponent({
  name: 'DatasetSync',
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
  data()
  {
    return {
      loading: false,
      loadingFields: false,
      fields: [] as PluginConfigureField[],
      overrides: {} as Record<string, string>,
      booleanProxies: {} as Record<string, boolean>
    }
  },
  created()
  {
    this.handleInitialize()
  },
  methods: {
    handleInitialize()
    {
      if (!this.info?.code) {
        return
      }
      this.loadingFields = true
      DatasetService.getSyncFields(this.info.code)
                    .then(response => {
                      if (response.status && Array.isArray(response.data)) {
                        this.fields = response.data
                        // 用 effective 值（已通过 defaultValue 字段下发）预填表单
                        for (const f of this.fields) {
                          this.overrides[f.name] = f.defaultValue ?? ''
                          if (f.type === 'BOOLEAN') {
                            this.booleanProxies[f.name] = (f.defaultValue + '').toLowerCase() === 'true'
                          }
                        }
                      }
                    })
                    .finally(() => this.loadingFields = false)
    },
    onBoolChange(name: string, value: boolean)
    {
      this.overrides[name] = value ? 'true' : 'false'
    },
    onSubmit()
    {
      if (this.info) {
        this.loading = true
        // 把所有 boolean 类型同步从 booleanProxies 落到 overrides
        for (const f of this.fields) {
          if (f.type === 'BOOLEAN') {
            this.overrides[f.name] = this.booleanProxies[f.name] ? 'true' : 'false'
          }
        }
        DatasetService.syncData(this.info.code, this.overrides)
                      .then(response => {
                        if (response.status) {
                          this.$Message.success({
                            content: `${ this.$t('dataset.common.syncData') } [ ${ this.info?.name } ] ${ this.$t('common.successfully') }`,
                            showIcon: true
                          })

                          this.onCancel()
                        }
                        else {
                          this.$Message.error({
                            content: `${ this.$t('dataset.common.syncData') } [ ${ this.info?.name } ] ${ this.$t('common.fail') }`,
                            showIcon: true
                          })
                        }
                      })
                      .finally(() => this.loading = false)
      }
    },
    onCancel()
    {
      this.visible = false
    }
  }
})
</script>
