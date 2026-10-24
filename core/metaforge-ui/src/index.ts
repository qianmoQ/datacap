import type { App } from 'vue'
import MfAlert from './components/MfAlert.vue'
import MfAvatar from './components/MfAvatar.vue'
import MfBadge from './components/MfBadge.vue'
import MfButton from './components/MfButton.vue'
import MfCard from './components/MfCard.vue'
import MfCheckbox from './components/MfCheckbox.vue'
import MfConfigProvider from './components/MfConfigProvider.vue'
import MfDivider from './components/MfDivider.vue'
import MfInput from './components/MfInput.vue'
import MfModal from './components/MfModal.vue'
import MfRadio from './components/MfRadio.vue'
import MfRadioGroup from './components/MfRadioGroup.vue'
import MfSelect from './components/MfSelect.vue'
import type { MfSelectOption } from './components/MfSelect.vue'
import MfSpace from './components/MfSpace.vue'
import MfSpinner from './components/MfSpinner.vue'
import MfSwitch from './components/MfSwitch.vue'
import MfTag from './components/MfTag.vue'
import MfTextarea from './components/MfTextarea.vue'
import MfTooltip from './components/MfTooltip.vue'

export const version = '2026.0.0'

export const components = {
    MfAlert,
    MfAvatar,
    MfBadge,
    MfButton,
    MfCard,
    MfCheckbox,
    MfConfigProvider,
    MfDivider,
    MfInput,
    MfModal,
    MfRadio,
    MfRadioGroup,
    MfSelect,
    MfSpace,
    MfSpinner,
    MfSwitch,
    MfTag,
    MfTextarea,
    MfTooltip,
}

export type { MfSelectOption }

export function install(app: App): void {
    for (const [name, component] of Object.entries(components)) {
        app.component(name, component)
    }
}

export {
    MfAlert,
    MfAvatar,
    MfBadge,
    MfButton,
    MfCard,
    MfCheckbox,
    MfConfigProvider,
    MfDivider,
    MfInput,
    MfModal,
    MfRadio,
    MfRadioGroup,
    MfSelect,
    MfSpace,
    MfSpinner,
    MfSwitch,
    MfTag,
    MfTextarea,
    MfTooltip,
}

export * from './theme/theme'

export default { install, version }
