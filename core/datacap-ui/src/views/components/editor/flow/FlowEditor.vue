<template>
  <ShadcnLayout style="height: calc(100vh - 35px)" @drop="onDrop">
    <ShadcnLayoutWrapper>
      <ShadcnLayoutSider>
        <FlowSider v-if="data" :data="data"/>
      </ShadcnLayoutSider>

      <ShadcnLayoutContent>
        <VueFlow :default-viewport="{ zoom: 1.5 }"
                 :min-zoom="0.2"
                 :max-zoom="4"
                 @dragover="onDragOver"
                 @nodeClick="onNodeClick($event, true)">
          <Background pattern-color="#aaa" :gap="8"/>
          <Controls/>
          <Panel position="top-right">
            <div class="space-x-2">
              <ShadcnTooltip :content="$t('pipeline.common.resetTransform')">
                <ShadcnButton circle @click="resetTransform">
                  <ShadcnIcon icon="RefreshCcw"/>
                </ShadcnButton>
              </ShadcnTooltip>

              <ShadcnTooltip :content="$t('common.save')">
                <ShadcnButton circle @click="saveConfigure(configure)">
                  <ShadcnIcon icon="Save"/>
                </ShadcnButton>
              </ShadcnTooltip>
            </div>
          </Panel>
        </VueFlow>
      </ShadcnLayoutContent>
    </ShadcnLayoutWrapper>
  </ShadcnLayout>

  <FlowConfigure v-if="configureVisible"
                 :isVisible="configureVisible"
                 :data="contextData"
                 @onChange="onChangeConfigure"
                 @close="onNodeClick(null, false)"/>
</template>

<script lang="ts">
import { defineComponent, getCurrentInstance, nextTick, watch } from 'vue'
import { Panel, Position, useVueFlow, VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { v4 as uuidv4 } from 'uuid'
import { useI18n } from 'vue-i18n'
import { Configuration } from '@/views/components/editor/flow/Configuration.ts'
import FlowSider from '@/views/components/editor/flow/components/FlowSider.vue'
import FlowConfigure from '@/views/components/editor/flow/components/FlowConfigure.vue'

export default defineComponent({
  name: 'FlowEditor',
  props: {
    data: {
      type: Array as () => Configuration[]
    }
  },
  components: { FlowConfigure, FlowSider, VueFlow, Background, Controls, Panel },
  setup(props, { emit })
  {
    console.debug(props.data)

    const i18n = useI18n()
    const { findNode, onConnect, addEdges, addNodes, project, vueFlowRef, setTransform, toObject } = useVueFlow({ nodes: [] })

    onConnect((params) => addEdges(params))

    /**
     * Handles the drop event.
     *
     * @param {Object} event - The drop event object.
     * @param {Object} event.dataTransfer - The dataTransfer object.
     * @param {Function} event.dataTransfer.getData - A function to get data from the dataTransfer object.
     * @param {number} event.clientX - The x-coordinate of the cursor position.
     * @param {number} event.clientY - The y-coordinate of the cursor position.
     */
    const onDrop = (event: any) => {
      const data = JSON.parse(event.dataTransfer?.getData('application/vueflow'))
      const type = data?.type
      const { left, top } = (vueFlowRef as any).value.getBoundingClientRect()
      const position = project({ x: event.clientX - left, y: event.clientY - top })
      const newNode = { id: `${ uuidv4() }`, type, position, label: `${ data.configure.name }`, data: data.configure } as any
      if (type === 'input') {
        newNode['sourcePosition'] = Position.Right
      }
      else if (type === 'output') {
        newNode['targetPosition'] = Position.Left
      }
      addNodes([newNode])
      // align node position after drop, so it's centered to the mouse
      nextTick(() => {
        const node = findNode(newNode.id) as any
        const stop = watch(
            () => node.dimensions,
            (dimensions) => {
              if (dimensions.width > 0 && dimensions.height > 0) {
                node.position = { x: node.position.x - node.dimensions.width / 2, y: node.position.y - node.dimensions.height / 2 }
                stop()
              }
            },
            { deep: true, flush: 'post' }
        )
      })
    }

    /**
     * Handles the drag over event.
     *
     * @param {object} event - The drag over event.
     * @param {function} event.preventDefault - Prevents the default behavior of the drag over event.
     * @param {object} event.dataTransfer - The data transfer object of the drag over event.
     * @param {string} event.dataTransfer.dropEffect - The drop effect of the drag over event.
     */
    const onDragOver = (event: any) => {
      event.preventDefault()
      if (event.dataTransfer) {
        event.dataTransfer.dropEffect = 'move'
      }
    }

    const resetTransform = () => {
      return setTransform({ x: 0, y: 0, zoom: 1 })
    }

    const { proxy } = getCurrentInstance()!

    const saveConfigure = (configure: any) => {
      const data = toObject()
      if (!configure.from) {
        // @ts-ignore
        proxy?.$Message.error({
          content: i18n.t('pipeline.validator.from'),
          showIcon: true
        })
        return
      }

      if (!configure.to) {
        // @ts-ignore
        proxy?.$Message.error({
          content: i18n.t('pipeline.validator.to'),
          showIcon: true
        })
        return
      }

      if (data.edges.length === 0) {
        // @ts-ignore
        proxy?.$Message.error({
          content: i18n.t('pipeline.validator.edge'),
          showIcon: true
        })
        return
      }
      configure.flow = data
      emit('onCommit', configure)
    }

    return {
      onDrop,
      onDragOver,
      resetTransform,
      saveConfigure
    }
  },
  data()
  {
    return {
      configureVisible: false,
      contextData: null,
      configure: { executor: 'Seatunnel', from: null, to: null, flow: null }
    }
  },
  methods: {
    onNodeClick(event: any, isOpen: boolean)
    {
      this.configureVisible = isOpen
      this.contextData = event?.node?.data
    },
    onChangeConfigure(value: any)
    {
      if (value.type === 'input') {
        this.configure.from = value
      }
      else if (value.type === 'output') {
        this.configure.to = value
      }
      this.onNodeClick(null, false)
    }
  }
})
</script>
