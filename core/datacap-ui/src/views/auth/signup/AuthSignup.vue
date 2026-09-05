<template>
  <BaseLayout>
    <div class="h-screen flex items-center justify-center">
      <div class="w-full max-w-md px-4 mx-auto">
        <a-card class="w-full">
          <template #title>
            <div class='flex items-center justify-center'>
              <a-avatar src="/static/images/logo.png" alt="DataCap"/>
            </div>
          </template>

          <div class="text-center text-gray-600 mb-2">
            {{ $t('user.auth.signupTip') }}
          </div>

          <div class="px-6 py-8 relative">
            <div v-if="loading" class="flex items-center justify-center py-8">
              <a-spin size="large"/>
            </div>
            <a-form v-else
                    ref="formRef"
                    :model="formState"
                    layout="vertical"
                    @finish="onSubmit"
                    @finishFailed="onError">
              <a-form-item name="username"
                           :label="$t('user.common.username')"
                           :rules="[
                             { required: true, message: $t('user.auth.usernameTip') },
                             { min: 3, message: $t('user.auth.usernameSizeTip') },
                             { max: 20, message: $t('user.auth.usernameSizeTip') }
                         ]">
                <a-input v-model:value="formState.username"
                         :placeholder="$t('user.auth.usernameTip')"/>
              </a-form-item>

              <a-form-item name="password"
                           :label="$t('user.common.password')"
                           :rules="[
                             { required: true, message: $t('user.auth.passwordTip') },
                             { min: 6, message: $t('user.auth.passwordSizeTip') },
                             { max: 20, message: $t('user.auth.passwordSizeTip') }
                         ]">
                <a-input-password v-model:value="formState.password"
                                  :placeholder="$t('user.auth.passwordTip')"/>
              </a-form-item>

              <a-form-item name="confirmPassword"
                           :label="$t('user.common.confirmPassword')"
                           :rules="[
                             { required: true, message: $t('user.auth.passwordTip') },
                             { validator: validatePassword }
                         ]">
                <a-input-password v-model:value="formState.confirmPassword"
                                  :placeholder="$t('user.auth.confirmPasswordTip')"/>
              </a-form-item>

              <a-form-item v-if="showCaptcha"
                           name="captcha"
                           :label="$t('user.common.captcha')"
                           :rules="[
                             { required: true, message: $t('user.auth.captchaTip') },
                             { min: 1, message: $t('user.auth.captchaSizeTip') },
                             { max: 6, message: $t('user.auth.captchaSizeTip') }
                         ]">
                <div class="flex items-center gap-2">
                  <a-input v-model:value="formState.captcha"
                           :placeholder="$t('user.auth.captchaTip')"/>
                  <a-button style="padding: 0"
                            type="text"
                            :loading="captchaLoading"
                            :disabled="captchaLoading"
                            @click="initCaptcha">
                    <img v-if="!captchaLoading" style="min-width: 120px; height: 100%;" :src="'data:image/png;base64,' + captchaImage"/>
                  </a-button>
                </div>
              </a-form-item>

              <a-space direction="vertical" :style="{ width: '100%' }">
                <a-button type="primary"
                          html-type="submit"
                          block
                          :disabled="submitting"
                          :loading="submitting">
                  {{ $t('user.common.signup') }}
                </a-button>

                <a-divider class="text-sm text-gray-400 py-2" orientation="center">
                  {{ $t('user.auth.hasUserTip') }}
                </a-divider>

                <a-button block class="text-center" @click="$router.push('/auth/signin')">
                  {{ $t('user.common.signin') }}
                </a-button>
              </a-space>
            </a-form>
          </div>
        </a-card>
      </div>
    </div>
  </BaseLayout>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import { message } from 'ant-design-vue'
import CaptchaService from '@/services/captcha'
import UserService from '@/services/user'
import router from '@/router'
import BaseLayout from '@/views/layouts/base/BaseLayout.vue'

interface Props
{
  username: string
  password: string
  confirmPassword: string
  timestamp: number
  captcha: number
}

export default defineComponent({
  name: 'AuthSignup',
  components: { BaseLayout },
  data()
  {
    return {
      formState: {} as Props,
      submitting: false,
      loading: false,
      showCaptcha: false,
      captchaImage: null,
      captchaLoading: false
    }
  },
  created()
  {
    this.loading = true
    this.initCaptcha()
  },
  methods: {
    initCaptcha()
    {
      this.captchaLoading = true
      this.formState.timestamp = Date.parse(new Date().toString())
      CaptchaService.getCaptcha(this.formState.timestamp)
                    .then(response => {
                      if (response.data !== false) {
                        this.showCaptcha = true
                        this.captchaImage = response.data.image
                      }
                    })
                    .finally(() => {
                      this.captchaLoading = false
                      this.loading = false
                    })
    },
    validatePassword(_rule: any, value: string)
    {
      if (value !== this.formState.password) {
        return Promise.reject(new Error(this.$t('user.auth.passwordNotMatchTip')))
      }
      return Promise.resolve(true)
    },
    onError(error: any)
    {
      const names = (error?.errorFields || []).map((field: any) => (Array.isArray(field.name) ? field.name.join('.') : field.name))
      message.error(`Validation error field: [ ${ names.join(', ') } ]`)
    },
    async onSubmit()
    {
      this.submitting = true
      UserService.signup(this.formState as any)
                 .then(response => {
                   if (response.status) {
                     router.push('/auth/signin')
                   }
                   else {
                     message.error(response.message)
                     this.initCaptcha()
                   }
                 })
                 .finally(() => this.submitting = false)
    }
  }
})
</script>
