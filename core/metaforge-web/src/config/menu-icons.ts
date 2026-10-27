import * as AntdIcons from '@ant-design/icons-vue'
import {
    BgColorsOutlined,
    CodeOutlined,
    DashboardOutlined,
    HistoryOutlined,
    HomeOutlined,
    ProjectOutlined,
    RightOutlined,
    SettingOutlined,
    ToolOutlined,
    UnorderedListOutlined
} from '@ant-design/icons-vue'

/**
 * Lucide 风格别名：种子数据中的菜单 icon 使用 lucide 命名（Settings2、SquareKanban 等），
 * 这里把它们映射到最接近的 antd 图标，保证存量菜单可渲染。
 */
const LUCIDE_ALIASES: Record<string, any> = {
    Contrast: BgColorsOutlined,
    Gauge: DashboardOutlined,
    Hammer: ToolOutlined,
    History: HistoryOutlined,
    Home: HomeOutlined,
    List: UnorderedListOutlined,
    Setting: SettingOutlined,
    Settings2: SettingOutlined,
    SquareChevronRight: RightOutlined,
    SquareDashedBottomCode: CodeOutlined,
    SquareKanban: ProjectOutlined
}

/**
 * 菜单图标注册表：key 为存储在菜单 icon 字段的图标名，value 为渲染组件。
 * 全量收录 @ant-design/icons-vue 的 Outlined 图标（名称去掉 Outlined 后缀），
 * lucide 别名优先（不被 antd 同名图标覆盖）。
 */
export const MENU_ICONS: Record<string, any> = { ...LUCIDE_ALIASES }

for (const [name, component] of Object.entries(AntdIcons)) {
    if (name.endsWith('Outlined')) {
        const key = name.replace(/Outlined$/, '')
        if (!MENU_ICONS[key]) {
            MENU_ICONS[key] = component
        }
    }
}
