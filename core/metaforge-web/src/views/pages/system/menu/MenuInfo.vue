<template>
  <MfDrawer v-model:open="visible" :title="title" width="40%">
    <div v-if="loading" class="flex items-center justify-center py-8">
      <MfSpinner size="lg"/>
    </div>
    <MfForm v-else-if="formState" :model="formState" layout="vertical" @finish="onSubmit">
      <div class="grid grid-cols-2 gap-4">
        <div>
          <MfFormItem name="name"
                      :label="$t('common.name')"
                      :rules="[{ required: true, message: $t('common.name') }]">
            <MfInput v-model="formState.name"/>
          </MfFormItem>
        </div>

        <div>
          <MfFormItem name="icon" :label="$t('menu.common.icon')">
            <div ref="iconPickerRoot" class="relative inline-flex w-full">
              <div class="mf-invalidable inline-flex h-9 w-full cursor-pointer items-center gap-2 rounded-mf-md border bg-surface pl-3 pr-2.5 text-sm transition"
                   :class="iconPickerVisible ? 'border-primary' : 'border-line'"
                   @click="iconPickerVisible = !iconPickerVisible">
                <span class="flex min-w-0 flex-1 items-center gap-2 text-left">
                  <component v-if="formState.icon && MENU_ICONS[formState.icon]"
                             :is="MENU_ICONS[formState.icon]" class="shrink-0 text-primary" :style="{ fontSize: '15px' }"/>
                  <span class="truncate" :class="formState.icon ? 'text-content' : 'text-content-placeholder'">
                    {{ formState.icon || $t('menu.tip.selectIcon') }}
                  </span>
                </span>
                <span v-if="formState.icon"
                      class="inline-flex size-4 shrink-0 items-center justify-center rounded-mf-full text-content-tertiary hover:bg-surface-hover hover:text-content"
                      @click.stop="clearIcon">
                  <svg class="size-2.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18"/></svg>
                </span>
                <svg class="size-3.5 shrink-0 text-content-tertiary transition-transform duration-(--mf-duration-base) ease-(--mf-easing)"
                     :class="iconPickerVisible && 'rotate-180'"
                     viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                  <path d="M6 9l6 6 6-6"/>
                </svg>
              </div>

              <div v-if="iconPickerVisible"
                   class="absolute left-0 top-full z-40 mt-1.5 w-full rounded-mf-lg border border-line bg-surface p-1 shadow-mf-lg">
                <div class="p-1 pb-2">
                  <MfInput v-model="iconSearch" :placeholder="$t('menu.tip.searchIcon')">
                    <template #prefix>
                      <SearchOutlined :style="{ fontSize: '13px', color: 'var(--dc-text-secondary)' }"/>
                    </template>
                  </MfInput>
                </div>
                <div class="max-h-64 overflow-y-auto">
                  <div class="grid grid-cols-6 gap-1">
                    <button v-for="(component, name) in filteredIcons"
                            :key="name"
                            type="button"
                            :title="name"
                            class="flex size-10 cursor-pointer items-center justify-center rounded-mf-md transition"
                            :class="formState.icon === name
                              ? 'bg-surface-active font-medium text-primary'
                              : 'text-content-secondary hover:bg-surface-hover hover:text-content'"
                            @click="selectIcon(name)">
                      <component :is="component" :style="{ fontSize: '16px' }"/>
                    </button>
                  </div>
                  <div v-if="Object.keys(filteredIcons).length === 0" class="py-6 text-center text-sm text-content-tertiary">
                    {{ $t('common.noData') }}
                  </div>
                </div>
              </div>
            </div>
          </MfFormItem>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-4">
        <div>
          <MfFormItem name="url"
                      :label="$t('common.url')"
                      :rules="[{ required: true, message: $t('common.url') }]">
            <MfInput v-model="formState.url"/>
          </MfFormItem>
        </div>

        <div>
          <MfFormItem name="group" :label="$t('common.group')">
            <MfInput v-model="formState.group"/>
          </MfFormItem>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-4">
        <div>
          <MfFormItem name="sorted" :label="$t('common.sorted')" :rules="[{ required: true, message: $t('common.sorted') }]">
            <MfInputNumber v-model="formState.sorted" :style="{ width: '100%' }"/>
          </MfFormItem>
        </div>

        <div>
          <MfFormItem name="type"
                      :label="$t('common.type')"
                      :rules="[{ required: true, message: $t('common.type') }]">
            <MfSelect v-model="formState.type" :placeholder="$t('menu.tip.selectType')" :options="[{ label: 'VIEW', value: 'VIEW' }]"/>
          </MfFormItem>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-4">
        <div>
          <MfFormItem :label="$t('common.parent')">
            <MfSelect v-model="formState.parent" :placeholder="$t('menu.tip.selectParent')" clearable :options="menuOptions"/>
          </MfFormItem>
        </div>

        <div>
          <MfFormItem :label="$t('menu.common.redirect')">
            <MfSelect v-model="formState.redirect" :placeholder="$t('menu.tip.selectRedirect')" clearable :options="menuOptions"/>
          </MfFormItem>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-4">
        <div>
          <MfFormItem name="new" :label="$t('menu.common.new')">
            <MfSwitch v-model="formState.new"/>
          </MfFormItem>
        </div>

        <div>
          <MfFormItem name="i18nKey" :label="$t('menu.common.i18nKey')" :rules="[{ required: true, message: $t('menu.common.i18nKey') }]">
            <MfInput v-model="formState.i18nKey"/>
          </MfFormItem>
        </div>
      </div>

      <MfFormItem name="description" :label="$t('common.description')">
        <MfTextarea v-model="formState.description"/>
      </MfFormItem>

      <div class="flex justify-end">
        <MfButton variant="primary" type="submit" :loading="loading" :disabled="loading">
          {{ $t('common.save') }}
        </MfButton>
      </div>
    </MfForm>
  </MfDrawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage, useDismissableLayer } from 'metaforge-ui'
import { cloneDeep } from 'lodash'
import { SearchOutlined } from '@ant-design/icons-vue'
import { MenuModel, MenuRequest } from '@/model/menu'
import MenuService from '@/services/menu'
import { FilterModel } from '@/model/filter'
import { MENU_ICONS } from '@/config/menu-icons'

defineOptions({ name: 'MenuInfo' })

const props = withDefaults(defineProps<{ isVisible?: boolean; info?: MenuModel | null }>(), {
  isVisible: false,
  info: null
})
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const { t } = useI18n()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const title = ref<string | null>(null)
const formState = ref<MenuModel>(null as unknown as MenuModel)
const loading = ref(false)
const fullMenus = ref<MenuModel[]>([])
const saving = ref(false)

// 迁移至 MfSelect：选项为数组形式
const menuOptions = computed(() => fullMenus.value.map((menu: MenuModel) => ({ label: menu.name, value: menu.id })))

const iconPickerVisible = ref(false)
const iconPickerRoot = ref<HTMLElement>()
useDismissableLayer(iconPickerRoot, iconPickerVisible, () => {
  iconPickerVisible.value = false
})

const iconSearch = ref('')
const filteredIcons = computed(() => {
  const query = iconSearch.value.trim().toLowerCase()
  if (!query) {
    return MENU_ICONS
  }
  return Object.fromEntries(Object.entries(MENU_ICONS).filter(([name]) => name.toLowerCase().includes(query)))
})

const selectIcon = (name?: string) => {
  formState.value.icon = name
  iconPickerVisible.value = false
}

const clearIcon = () => {
  formState.value.icon = undefined
  iconSearch.value = ''
}

const handlerInitialize = () => {
  title.value = `${ t('menu.common.create') }`
  if (props.info) {
    formState.value = cloneDeep(props.info)
    title.value = `${ t('menu.common.modify').replace('$NAME', props.info.name as string) }`
  }
  else {
    formState.value = MenuRequest.of()
  }

  const filter: FilterModel = new FilterModel()
  loading.value = true
  MenuService.getAll(filter)
             .then((response) => {
               if (response.status) {
                 fullMenus.value = response.data.content
               }
             })
             .finally(() => (loading.value = false))
}

const onCancel = () => {
  visible.value = false
}

const onSubmit = () => {
  saving.value = true
  MenuService.saveOrUpdate(formState.value)
             .then(response => {
               if (response.status) {
                 MfMessage.success('successful')
                 onCancel()
               }
               else {
                 MfMessage.error(response.message)
               }
             })
             .finally(() => (saving.value = false))
}

handlerInitialize()
</script>
