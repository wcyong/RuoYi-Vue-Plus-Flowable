<template>
  <div class="containers">
    <el-header style="border-bottom: 1px solid rgb(218 218 218);height: auto;">
        <div style="display: flex; padding: 10px 0px; justify-content: space-between;">
          <div>
            <el-tooltip effect="dark" content="自适应屏幕" placement="bottom">
              <el-button size="mini" icon="el-icon-rank" @click="fitViewport">自适应屏幕</el-button>
            </el-tooltip>
            <el-tooltip effect="dark" content="放大" placement="bottom">
              <el-button size="mini" icon="el-icon-zoom-in" @click="zoomViewport(true)">放大</el-button>
            </el-tooltip>
            <el-tooltip effect="dark" content="缩小" placement="bottom">
              <el-button size="mini" icon="el-icon-zoom-out" @click="zoomViewport(false)">缩小</el-button>>
            </el-tooltip>
          </div>
        </div>
     </el-header>
     <div class="flow-containers" v-loading="loading" element-loading-background="#fff">
        <el-container style="align-items: stretch">
            <el-main style="padding: 0;">
            <div ref="canvas" class="canvas" />
            </el-main>
        </el-container>
    </div>
  </div>
</template>

<script>
import BpmnViewer from 'bpmn-js/lib/NavigatedViewer';
import processApi from "@/api/workflow/processInst";
import getInitStr from '@/components/Bpmn/package/flowable/init'
export default {
  name: 'WorkflowBpmnModeler',
  props: {
    processInstanceId: String
  },
  data() {
    return {
      modeler: null,
      taskList: [],
      zoom: 1,
      xml:'',
      loading: false
    }
  },
  watch: {
    xml: {
        handler(newVal,oldVal){
          if(newVal) {
            this.createNewDiagram(newVal)
          }
        },
        immediate: true,
        deep:true
      }
  },
  mounted() {
    this.modeler = new BpmnViewer({
      container: this.$refs.canvas,
      height: 'calc(100vh - 200px)',
    })
    processApi.getXml(this.processInstanceId).then(response=>{        
      this.xml = response.data.xml
      this.taskList = response.data.taskList
    }) 
    // 新增流程定义
    if (!this.xml) {
      this.createNewDiagram(getInitStr())
    } else {
      this.createNewDiagram(this.xml)
    }
  },
  methods: {
    async createNewDiagram(data) {
      try {
        this.loading = true
        await this.modeler.importXML(data)
        this.initZoomViewport(false)
        this.fillColor()
      } catch (err) {
        //console.error(err.message, err.warnings)
      }
    },
    // 让图能自适应屏幕
    fitViewport(){
      this.zoom = this.modeler.get('canvas').zoom('fit-viewport')
      const bbox = document.querySelector('.flow-containers .viewport').getBBox()
      const currentViewbox = this.modeler.get('canvas').viewbox()
      const elementMid = {
        x: bbox.x + bbox.width / 2 - 65,
        y: bbox.y + bbox.height / 2
      }
      this.modeler.get('canvas').viewbox({
        x: elementMid.x - currentViewbox.width / 2,
        y: elementMid.y - currentViewbox.height / 2,
        width: currentViewbox.width,
        height: currentViewbox.height
      })
      this.zoom = bbox.width / currentViewbox.width * 1.8
    },
    // 放大缩小
    zoomViewport(zoomIn = true) {
      this.zoom = this.modeler.get('canvas').zoom()
      this.zoom += (zoomIn ? 0.1 : -0.1)
      this.modeler.get('canvas').zoom(this.zoom)
    },
    initZoomViewport(zoomIn = true) {
      this.zoom = this.modeler.get('canvas').zoom()
      this.zoom += (zoomIn ? 0.1 : -0.1)
      this.modeler.get('canvas').zoom(this.zoom)
      setTimeout(() => {
        this.zoom = this.modeler.get('canvas').zoom("fit-viewport", "auto")
        this.loading = false
      },2000)
    },
    //上色
    fillColor() {
      const canvas = this.modeler.get('canvas')
      this.bpmnNodeList(this.modeler._definitions.rootElements[0].flowElements,canvas)
    },
    //递归上色
    bpmnNodeList(flowElements,canvas){
        flowElements.forEach(n => {
            if (n.$type === 'bpmn:UserTask') {
                const completeTask = this.taskList.find(m => m.key === n.id)
                const todoTask = this.taskList.find(m => !m.completed)
                const endTask = this.taskList[this.taskList.length - 1]
                if (completeTask) {
                    canvas.addMarker(n.id, completeTask.completed ? 'highlight' : 'highlight-todo')
                    n.outgoing?.forEach(nn => {
                        const targetTask = this.taskList.find(m => m.key === nn.targetRef.id)
                        if (targetTask) {
                            canvas.addMarker(nn.id, targetTask.completed ? 'highlight' : 'highlight-todo')
                        } else if (nn.targetRef.$type === 'bpmn:ExclusiveGateway') {
                            canvas.addMarker(nn.id, completeTask.completed ? 'highlight' : 'highlight-todo')
                            canvas.addMarker(nn.targetRef.id, completeTask.completed ? 'highlight' : 'highlight-todo')
                        } else if (nn.targetRef.$type === 'bpmn:EndEvent') {
                            if (!todoTask && endTask.key === n.id) {
                                canvas.addMarker(nn.id, 'highlight')
                                canvas.addMarker(nn.targetRef.id, 'highlight')
                            }
                            if (!completeTask.completed) {
                                canvas.addMarker(nn.id, 'highlight-todo')
                                canvas.addMarker(nn.targetRef.id, 'highlight-todo')
                            }
                        }
                    })
                }
            } else if (n.$type === 'bpmn:ExclusiveGateway') {
                n.outgoing.forEach(nn => {
                    const targetTask = this.taskList.find(m => m.key === nn.targetRef.id)
                    if (targetTask) {
                        canvas.addMarker(nn.id, targetTask.completed ? 'highlight' : 'highlight-todo')
                    }
                })
            } else if (n.$type === 'bpmn:SubProcess') {
                this.bpmnNodeList(n.flowElements,canvas)
            }
            if (n.$type === 'bpmn:StartEvent') {
                n.outgoing.forEach(nn => {
                    const completeTask = this.taskList.find(m => m.key === nn.targetRef.id)
                    if (completeTask) {
                        canvas.addMarker(nn.id, 'highlight')
                        canvas.addMarker(n.id, 'highlight')
                        return
                    }
                })
            }
      })
    }
  }
}
</script>

<style lang="scss">
.containers {
    position: absolute;
    background-color: #ffffff;
    top:0;
    left: 0;
    width: 100%;
    height: 100%;
}
.canvas {
    width: 100%;
    height: 100%;
}
.view-mode {
  .el-header, .el-aside, .djs-palette, .bjs-powered-by {
    display: none;
  }
  .el-loading-mask {
    background-color: initial;
  }
  .el-loading-spinner {
    display: none;
  }
}
.flow-containers {
    width: 100%;
    height: 100%;
    .canvas {
        width: 100%;
        height: 100%;
    }
    .load {
        margin-right: 10px;
    }
    .el-form-item__label{
        font-size: 13px;
    }

    .djs-palette{
        left: 0px!important;
        top: 0px;
        border-top: none;
    }

    .djs-container svg {
        min-height: 650px;
    }

    .highlight.djs-shape .djs-visual > :nth-child(1) {
        fill: green !important;
        stroke: green !important;
        fill-opacity: 0.2 !important;
    }
    .highlight.djs-shape .djs-visual > :nth-child(2) {
        fill: green !important;
    }
    .highlight.djs-shape .djs-visual > path {
        fill: green !important;
        fill-opacity: 0.2 !important;
        stroke: green !important;
    }
    .highlight.djs-connection > .djs-visual > path {
        stroke: green !important;
    }
    .highlight-todo.djs-connection > .djs-visual > path {
        stroke: orange !important;
        stroke-dasharray: 4px !important;
        fill-opacity: 0.2 !important;
        marker-end: url(#sequenceflow-end-_E7DFDF-_E7DFDF-803g1kf6zwzmcig1y2ulm5egr);
    }
    .highlight-todo.djs-shape .djs-visual > :nth-child(1) {
        fill: orange !important;
        stroke: orange !important;
        stroke-dasharray: 4px !important;
        fill-opacity: 0.2 !important;
    }
}
</style>
