<template>
  <MfDrawer v-model:open="visible" :title="$t('function.common.import')" width="40%">
    <MfForm v-if="formState" :model="formState" layout="vertical">
      <MfTabs v-model:active-key="formState.mode">
        <MfTabPane :item-key="FunctionMode.txt" :tab="$t('common.content')">
          <MfFormItem name="content" :label="$t('common.content')">
            <AceEditor :value="formState.content" @update:value="formState.content = $event"/>
          </MfFormItem>
        </MfTabPane>

        <MfTabPane :item-key="FunctionMode.url" :tab="$t('function.common.importFromUrl')">
          <div class="rounded-mf-md border border-line bg-surface px-3 py-2.5">
            <p class="mb-2 text-xs text-content-tertiary">{{ $t('function.tip.urlExample') }}</p>
            <div class="flex flex-col gap-1.5">
              <div v-for="item in urlExamples" :key="item.url" class="flex items-center gap-2">
                <MfTag>{{ item.label }}</MfTag>
                <code class="min-w-0 flex-1 truncate font-mono text-xs text-content-secondary">{{ item.url }}</code>
              </div>
            </div>
          </div>

          <MfFormItem name="url" :label="$t('common.url')" class="mt-3">
            <MfInput v-model="formState.content" :placeholder="$t('function.tip.urlExample')"/>
          </MfFormItem>
        </MfTabPane>
      </MfTabs>

      <MfFormItem name="plugin"
                  :label="$t('common.plugin')"
                  :rules="[{ required: true, message: $t('common.plugin') }]">
        <MfSelect v-model="formState.plugin"
                  multiple
                  :placeholder="$t('function.tip.selectPluginHolder')"
                  :options="plugins"/>
      </MfFormItem>

      <MfFormItem name="type"
                  :label="$t('common.type')"
                  :rules="[{ required: true, message: $t('common.type') }]">
        <MfSelect v-model="formState.type"
                  :placeholder="$t('function.tip.selectTypeHolder')"
                  :options="types"/>
      </MfFormItem>
    </MfForm>

    <template #footer>
      <MfSpace>
        <MfButton variant="danger" :loading="loading" :disabled="loading" @click="onCancel()">
          {{ $t('common.cancel') }}
        </MfButton>
        <MfButton variant="primary" :loading="loading" :disabled="loading" @click="onSubmit()">
          {{ $t('function.common.import') }}
        </MfButton>
      </MfSpace>
    </template>
  </MfDrawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { useHeaders } from '@/views/pages/system/function/FunctionUtils'
import { FunctionImportModel, FunctionMode } from '@/model/function'
import FunctionService from '@/services/function'
import SourceService from '@/services/source'
import AceEditor from '@/views/components/editor/AceEditor.vue'

defineOptions({ name: 'FunctionImport' })

const props = withDefaults(defineProps<{ isVisible?: boolean }>(), {
  isVisible: false
})
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const { t } = useI18n()

const { typeHeaders: types } = useHeaders()

const urlExamples = [
  { label: t('function.common.keyword'), url: 'https://metaforge.devlive.org/resources/functions/plugin/keywords.txt' },
  { label: t('function.common.operator'), url: 'https://metaforge.devlive.org/resources/functions/plugin/operators.txt' },
  { label: t('function.common.function'), url: 'https://metaforge.devlive.org/resources/functions/plugin/functions.txt' }
]

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const formState = ref<FunctionImportModel>(null as unknown as FunctionImportModel)
const loading = ref(false)
const plugins = ref<any[]>([])

const handlerInitialize = () => {
  formState.value = {
    mode: FunctionMode.txt,
    plugin: undefined,
    content: undefined,
    type: undefined
  } as unknown as FunctionImportModel

  SourceService.getPlugins()
               .then(response => {
                 if (response.status) {
                   plugins.value = Array.from(
                       new Set(
                           (Object.values(response.data)
                                  .reduce((acc: any, curr) => acc.concat(curr), []) as any[])
                               .map((value: { name: string }) => ({ label: value.name, value: value.name }))
                       )
                   )
                 }
               })
}

const onSubmit = () => {
  loading.value = true
  FunctionService.import(formState.value)
                 .then((response) => {
                   if (response.status) {
                     MfMessage.success('Create successfully')
                     onCancel()
                   }
                   else {
                     MfMessage.error(response.message)
                   }
                 })
                 .finally(() => (loading.value = false))
}

const onCancel = () => {
  emit('close', false)
}

handlerInitialize()
</script>
