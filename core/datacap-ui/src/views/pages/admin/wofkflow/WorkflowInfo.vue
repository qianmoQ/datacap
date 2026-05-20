<template>
  <div class="w-full h-screen min-h-screen" style="height: 100%;">
    <ShadcnCard>
      <template #title>
        <div class="ml-2">{{ $t('workflow.text.create') }}</div>
      </template>

      <template #extra>
        <div class="flex items-center space-x-4">
          <div class="flex items-center space-x-2">
            <ShadcnText type="small">{{ $t('common.executor') }}</ShadcnText>

            <ShadcnSelect v-model="formState.executor">
              <template #options>
                <ShadcnSelectOption v-for="executor in installedExecutors"
                                    :key="executor.name"
                                    :label="executor.name"
                                    :value="executor.name">
                </ShadcnSelectOption>
              </template>
            </ShadcnSelect>
          </div>

          <ShadcnButton :disabled="!(workflowState?.validation?.length === 0)" @click="visible = true">
            {{ $t('common.publish') }}
          </ShadcnButton>
        </div>
      </template>

      <div class="relative h-screen">
        <ShadcnSpin v-model="loading" fixed/>

        <ShadcnWorkflowEditor v-if="configuration && !loading"
                              v-model="workflowState"
                              :categories="configuration.categories"
                              :nodes="configuration.nodes"
                              :connections="[]"
                              :configureWidth="380">
        </ShadcnWorkflowEditor>
      </div>
    </ShadcnCard>
  </div>

  <ShadcnModal v-model="visible">
    <template #title>{{ $t('workflow.text.configure') }}</template>

    <ShadcnForm v-model="formState" @on-submit="onSubmit">
      <ShadcnFormItem name="name"
                      :label="$t('workflow.text.name')"
                      :rules="[
                          { required: true, message: $t('workflow.validator.name.required') },
                      ]">
        <ShadcnInput v-model="formState.name" name="name" :placeholder="$t('workflow.placeholder.name')"/>
      </ShadcnFormItem>

      <div class="justify-end">
        <ShadcnButton submit>{{ $t('common.submit') }}</ShadcnButton>
      </div>
    </ShadcnForm>
  </ShadcnModal>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import ConfigurationService from '@/services/configure'
import WorkflowService from '@/services/workflow'
import PluginService from '@/services/plugin'
import HttpUtils from '@/utils/http.ts'
import { RouterUtils } from '@/utils/route.ts'

export interface Configuration
{
  categories: any[]
  nodes: any[]
}

export default defineComponent({
  name: 'WorkflowInfo',
  data()
  {
    return {
      loading: false,
      visible: false,
      configuration: null as Configuration | null,
      workflowState: null as any | null,
      installedExecutors: [] as any[],
      formState: {
        code: null as string | null,
        executor: 'Local',
        name: '',
        configure: null
      }
    }
  },
  created()
  {
    this.handleInitialize()
  },
  methods: {
    handleInitialize()
    {
      const code = RouterUtils.getParam(this.$route, 'code')
      this.loading = true

      // Prepare the base API calls that are always needed
      const baseApiCalls = [
        ConfigurationService.getExecutor(),
        PluginService.getPlugins()
      ]

      // If code exists, add the workflow API call
      const apiCalls = code
          ? [...baseApiCalls, WorkflowService.getByCode(code)]
          : baseApiCalls

      HttpUtils.all(apiCalls)
               .then(HttpUtils.spread((...responses) => {
                 // Handle executor configuration
                 const [executor, plugin, ...rest] = responses

                 if (executor.status && executor.data) {
                   this.configuration = executor.data
                 }

                 // Handle plugins
                 if (plugin.status && plugin.data) {
                   this.installedExecutors = plugin.data.filter(
                       (v: { type: string }) => v.type === 'EXECUTOR'
                   )
                 }

                 // Handle workflow data if code exists
                 if (code && rest.length > 0) {
                   const workflow = rest[0]
                   if (workflow.status && workflow.data) {
                     this.workflowState = workflow.data.configure
                     // Update form state with existing workflow data
                     this.formState.executor = workflow.data.executor || 'Local'
                     this.formState.name = workflow.data.name || ''
                     this.formState.configure = workflow.data.configure || null
                     this.formState.code = workflow.data.code || null
                   }
                   else {
                     this.$Message.error({ content: workflow.message, showIcon: true })
                   }
                 }
               }))
               .catch(error => {
                 this.$Message.error({
                   content: error.message || 'Failed to initialize workflow',
                   showIcon: true
                 })
               })
               .finally(() => {
                 this.loading = false
               })
    },
    onSubmit()
    {
      this.formState.configure = this.workflowState.data
      WorkflowService.saveOrUpdate(this.formState)
                     .then((response) => {
                       if (response.status) {
                         this.$Message.success({
                           content: `${ this.$t('workflow.tip.publishSuccess').replace('$VALUE', response.data.name) }`,
                           showIcon: true
                         })

                         this.$router.push('/admin/workflow')
                         this.visible = false
                       }
                       else {
                         this.$Message.error({
                           content: response.message,
                           showIcon: true
                         })
                       }
                     })
    }
  }
})
</script>
