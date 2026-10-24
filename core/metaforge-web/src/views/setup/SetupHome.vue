<template>
  <BaseLayout>
    <div class="h-screen flex items-center justify-center">
      <div class="w-full max-w-xl px-4 mx-auto">
        <MfCard class="w-full">
          <template #header>
            <div class="flex items-center justify-center gap-3">
              <MfAvatar src="/static/images/logo.png" alt="MetaForge"/>
              <div>
                <div class="text-base font-semibold">{{ $t('setup.title') }}</div>
                <div class="text-xs text-content-tertiary">{{ $t('setup.subtitle') }}</div>
              </div>
            </div>
          </template>

          <!-- 自绘步骤条 -->
          <div class="flex items-center justify-center gap-0 px-6 pt-2 pb-6">
            <template v-for="(label, index) in stepLabels" :key="label">
              <div class="flex items-center">
                <div class="flex flex-col items-center gap-1">
                  <span
                    class="flex size-7 items-center justify-center rounded-mf-full text-xs font-semibold transition-colors duration-(--mf-duration-base)"
                    :class="step > index ? 'bg-primary text-on-primary' : step === index ? 'border-2 border-primary text-primary' : 'border border-line text-content-tertiary'">
                    <svg v-if="step > index" class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" aria-hidden="true">
                      <path d="M5 13l4 4L19 7" />
                    </svg>
                    <template v-else>{{ index + 1 }}</template>
                  </span>
                  <span class="text-xs" :class="step >= index ? 'text-content' : 'text-content-tertiary'">{{ label }}</span>
                </div>
                <div v-if="index < stepLabels.length - 1" class="mx-3 mb-5 h-px w-16 sm:w-24" :class="step > index ? 'bg-primary' : 'bg-line'"/>
              </div>
            </template>
          </div>

          <div class="px-6 pb-6">
            <!-- Step 1: environment -->
            <div v-if="step === 0">
              <div v-if="envLoading" class="flex items-center justify-center py-8">
                <MfSpinner size="lg" label="Loading..."/>
              </div>
              <template v-else>
                <div class="grid gap-3">
                  <div class="flex items-center justify-between rounded-mf-md border border-line px-4 py-3 text-sm">
                    <span class="text-content-tertiary">{{ $t('setup.env.database') }}</span>
                    <span class="font-medium text-content">{{ environment.database || '--' }}</span>
                  </div>
                  <div class="flex items-center justify-between rounded-mf-md border border-line px-4 py-3 text-sm">
                    <span class="text-content-tertiary">{{ $t('setup.env.product') }}</span>
                    <span class="font-medium text-content">{{ environment.product || '--' }}</span>
                  </div>
                  <div class="flex items-center justify-between rounded-mf-md border border-line px-4 py-3 text-sm">
                    <span class="text-content-tertiary">{{ $t('setup.env.version') }}</span>
                    <span class="max-w-[60%] truncate font-medium text-content">{{ environment.version || '--' }}</span>
                  </div>
                </div>

                <div class="mt-4 flex items-center gap-3">
                  <MfButton variant="outline" :loading="testing" @click="onTestConnection">
                    {{ testing ? $t('setup.env.testing') : $t('setup.env.test') }}
                  </MfButton>
                  <MfTag v-if="testResult === 'ok'" variant="success">{{ $t('setup.env.connected') }} · {{ testElapsed }}ms</MfTag>
                  <MfTag v-else-if="testResult === 'failed'" variant="danger">{{ $t('setup.env.testFailed') }}</MfTag>
                  <MfTag v-else-if="environment.connected === false" variant="danger">{{ $t('setup.env.disconnected') }}</MfTag>
                  <MfTag v-else variant="info">{{ $t('setup.env.connected') }}</MfTag>
                </div>

                <div class="mt-6 flex justify-end">
                  <MfButton variant="primary" :disabled="environment.connected === false" @click="step = 1">
                    {{ $t('setup.next') }}
                  </MfButton>
                </div>
              </template>
            </div>

            <!-- Step 2: administrator account -->
            <div v-else-if="step === 1">
              <MfForm ref="formRef" :model="formState" layout="vertical" @finish="step = 2; onInstall()" @finish-failed="onInvalid">
                <MfFormItem name="username" :label="$t('setup.account.username')"
                            :rules="[
                              { required: true, message: $t('setup.account.usernameTip') },
                              { min: 3, message: $t('setup.account.usernameSizeTip') },
                              { max: 20, message: $t('setup.account.usernameSizeTip') }
                            ]">
                  <MfInput v-model="formState.username" :placeholder="$t('setup.account.usernameTip')"/>
                </MfFormItem>

                <MfFormItem name="password" :label="$t('setup.account.password')"
                            :rules="[
                              { required: true, message: $t('setup.account.passwordTip') },
                              { min: 6, message: $t('setup.account.passwordSizeTip') },
                              { max: 20, message: $t('setup.account.passwordSizeTip') }
                            ]">
                  <MfInput v-model="formState.password" type="password" show-password :placeholder="$t('setup.account.passwordTip')"/>
                </MfFormItem>

                <MfFormItem name="confirmPassword" :label="$t('setup.account.confirmPassword')"
                            :rules="[
                              { required: true, message: $t('setup.account.passwordTip') },
                              { validator: validatePassword }
                            ]">
                  <MfInput v-model="formState.confirmPassword" type="password" show-password :placeholder="$t('setup.account.passwordTip')"/>
                </MfFormItem>

                <MfFormItem name="name" :label="$t('setup.account.name')">
                  <MfInput v-model="formState.name" :placeholder="$t('setup.account.name')"/>
                </MfFormItem>

                <div class="mt-2 flex justify-between">
                  <MfButton @click="step = 0">{{ $t('setup.back') }}</MfButton>
                  <MfButton variant="primary" type="submit">{{ $t('setup.next') }}</MfButton>
                </div>
              </MfForm>
            </div>

            <!-- Step 3: install result -->
            <div v-else class="flex flex-col items-center gap-4 py-6 text-center">
              <template v-if="installState === 'running'">
                <MfSpinner size="lg"/>
                <p class="text-sm text-content-secondary">{{ $t('setup.install.running') }}</p>
              </template>
              <template v-else-if="installState === 'success'">
                <MfAlert variant="success" :title="$t('setup.install.success')" class="text-left">
                  {{ $t('setup.install.successTip') }}
                </MfAlert>
                <MfButton variant="primary" @click="$router.push('/auth/signin')">
                  {{ $t('setup.install.goToSignin') }}
                </MfButton>
              </template>
              <template v-else>
                <MfAlert variant="danger" :title="$t('setup.install.failed')" class="text-left">
                  {{ installMessage }}
                </MfAlert>
                <MfButton variant="outline" @click="step = 1">{{ $t('setup.install.retry') }}</MfButton>
              </template>
            </div>
          </div>
        </MfCard>
      </div>
    </div>
  </BaseLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import SetupService from '@/services/setup'
import BaseLayout from '@/views/layouts/base/BaseLayout.vue'
import { invalidateSetupCheck } from '@/router'

defineOptions({ name: 'SetupHome' })

const { t } = useI18n()

const step = ref(0)
const stepLabels = computed(() => [
  t('setup.steps.environment'),
  t('setup.steps.account'),
  t('setup.steps.finish'),
])

const envLoading = ref(true)
const environment = ref<Record<string, any>>({})
const testing = ref(false)
const testResult = ref<'idle' | 'ok' | 'failed'>('idle')
const testElapsed = ref(0)

const formRef = ref()
const formState = ref({
  username: '',
  password: '',
  confirmPassword: '',
  name: '',
})

const installState = ref<'idle' | 'running' | 'success' | 'failed'>('idle')
const installMessage = ref('')

const validatePassword = (value: unknown) => {
  if (value !== formState.value.password) {
    return t('setup.account.passwordNotMatchTip')
  }
  return true
}

const onInvalid = () => MfMessage.error(t('setup.account.passwordNotMatchTip'))

const loadEnvironment = () => {
  envLoading.value = true
  SetupService.getEnvironment()
              .then(response => {
                if (response.status) {
                  environment.value = response.data || {}
                }
              })
              .finally(() => (envLoading.value = false))
}

const onTestConnection = () => {
  testing.value = true
  testResult.value = 'idle'
  SetupService.testConnection()
              .then(response => {
                if (response.status && response.data?.connected) {
                  testResult.value = 'ok'
                  testElapsed.value = response.data.elapsed ?? 0
                }
                else {
                  testResult.value = 'failed'
                }
              })
              .catch(() => (testResult.value = 'failed'))
              .finally(() => (testing.value = false))
}

const onInstall = () => {
  installState.value = 'running'
  SetupService.install({
    username: formState.value.username,
    password: formState.value.password,
    name: formState.value.name,
  })
              .then(response => {
                if (response.status) {
                  installState.value = 'success'
                  invalidateSetupCheck()
                }
                else {
                  installState.value = 'failed'
                  installMessage.value = response.message || ''
                }
              })
              .catch((error) => {
                installState.value = 'failed'
                installMessage.value = String(error)
              })
}

onMounted(loadEnvironment)
</script>
