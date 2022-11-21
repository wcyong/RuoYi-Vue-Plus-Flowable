<template>
    <el-dialog  title="流程定义" :visible.sync="bpmnVisible" v-if="bpmnVisible" width="80%">
      <div class="containers" v-loading="loading">
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
         <div class="flow-containers">
            <el-container class="bpmn-el-container" style="align-items: stretch">
                <el-main style="padding: 0;">
                <div ref="canvas" class="canvas" />
                </el-main>
            </el-container>
        </div>
        <processUserSetting ref="processUserSettingRef"/>
      </div>
    </el-dialog>
    </template>
    
    <script>
    import processUserSetting from './processUserSetting'
    import {getXml} from "@/api/workflow/definition";
    import { CustomViewer as BpmnViewer } from "@/components/Bpmn/package/customBpmn";
    export default {
      components: {
        processUserSetting
      },
      data() {
        return {
          modeler: null,
          taskList: [],
          zoom: 1,
          xml:'',
          loading: false,
          bpmnVisible: false,
          processDefinitionId:''
        }
      },
      methods: {
        init(processDefinitionId) {
          this.processDefinitionId = processDefinitionId
          this.loading = true
          this.bpmnVisible = true
          this.$nextTick(()=>{
            if (this.modeler) this.modeler.destroy();
            this.modeler = new BpmnViewer({
              container: this.$refs.canvas
            })
            getXml(processDefinitionId).then(response=>{        
              this.xml = response.data.xmlStr
              this.createDiagram(this.xml)
            })
          })
        },
        async createDiagram(data) {
          try {
            await this.modeler.importXML(data)
            this.loading = false
            this.fitViewport()
            this.addEventBusListener()
          } catch (err) {
            //console.error(err.message, err.warnings)
          }
        },
        addEventBusListener() {
            const that = this;
            const eventBus = this.modeler.get("eventBus");

            eventBus.on("element.click", function(e) {
                if (e.element.businessObject.$type == "bpmn:UserTask") {
                    that.$refs.processUserSettingRef.changeSteps(that.processDefinitionId,e.element.businessObject.id,e.element.businessObject.name)
                }
            });
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
        }
      }
    }
    </script>
    
    <style lang="scss">
    @import "../../../../../node_modules/bpmn-js/dist/assets/diagram-js.css";
    @import "../../../../../node_modules/bpmn-js/dist/assets/bpmn-font/css/bpmn.css";
    @import "../../../../../node_modules/bpmn-js/dist/assets/bpmn-font/css/bpmn-codes.css";
    @import "../../../../../node_modules/bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css";
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
    .bpmn-el-container{
      height: 550px;
    }
    .djs-element > .djs-hit-all{
        cursor: pointer !important;
    }
    </style>
    