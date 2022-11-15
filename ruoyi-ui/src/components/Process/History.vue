<template>
    <div>
      <el-tabs  type="border-card" >
        <el-tab-pane label="审批意见" v-loading="loading" class="container-tab">
            <el-table v-if="historicProcessInstance.length>0" :data="historicProcessInstance" style="width: 100%" max-height="570" v-loading="loading"> 
                <el-table-column prop="deleteReason" label="作废理由" align="center" ></el-table-column>
            </el-table>
            <el-table :data="list" style="width: 100%" max-height="570">
              <el-table-column label="流程审批历史记录" align="center">
                <el-table-column type="index" label="序号" align="center" width="50"></el-table-column>
                <el-table-column prop="name" label="任务名称" align="center" ></el-table-column>
                <el-table-column prop="nickName" label="办理人" align="center" ></el-table-column>
                <el-table-column prop="status" label="状态" align="center" ></el-table-column>
                <el-table-column prop="comment" label="审批意见" align="center" ></el-table-column>
                <el-table-column prop="fileList" width="100" label="附件" align="center" >
                  <template slot-scope="scope">
                    <el-popover v-if="scope.row.fileList.length>0" placement="right" trigger="click">
                      <el-table :data="scope.row.fileList">
                        <el-table-column width="200" prop="name" :show-overflow-tooltip="true" label="附件名称"></el-table-column>
                        <el-table-column width="50" label="操作">
                          <template slot-scope="scope">
                            <el-button size="mini" type="text" @click="downloadFile(scope.row)">下载</el-button>
                          </template>
                        </el-table-column>
                      </el-table>
                      <el-button size="mini" type="text" slot="reference">查看附件</el-button>
                    </el-popover>
                  </template>
                </el-table-column>
                <el-table-column prop="startTime" label="开始时间" align="center" ></el-table-column>
                <el-table-column prop="endTime" label="结束时间" align="center" ></el-table-column>
                <el-table-column prop="runDuration" label="运行时长" align="center" ></el-table-column>
                <el-table-column fixed="right" label="操作" v-if="editMessage" align="center" width="80">
                  <template slot-scope="scope">
                    <el-button @click="handleClick(scope.row)" type="text" v-if="scope.row.commentId" size="small">编辑意见</el-button>
                  </template>
                </el-table-column>
              </el-table-column>
            </el-table>
        </el-tab-pane>
        <el-tab-pane label="流程进度" v-if="processInstanceId" class="container-tab">
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
            </div>
        </el-tab-pane>
      </el-tabs>
      <el-dialog title="编辑意见" :close-on-click-modal="false" :visible.sync="dialogVisible" v-if="dialogVisible" append-to-body width="60%">
        <el-form>
          <el-form-item label="审批意见" prop="comment" label-width="120px">
            <el-input  type="textarea" v-model="comment" maxlength="300" placeholder="请输入意见" :autosize="{ minRows: 4 }" show-word-limit />
          </el-form-item>
          <el-form-item label="附件" prop="message" label-width="120px">
            <el-upload ref="redmineUpload" :file-list="attachmentList"
                        :multiple="true"
                        :limit="5"
                        action="'#'"
                        :on-change="handleFileChange"
                        :on-preview="downloadFile"
                        :before-remove="beforeRemove"
                        :auto-upload="false"
                        :show-file-list="true" >
            <el-button slot="trigger" size="small" type="primary" icon="el-icon-upload">点击上传</el-button>
            </el-upload>
        </el-form-item>
        </el-form>
        <span slot="footer" class="dialog-footer">
          <el-button type="primary" size="small" @click="clickUpdate">确 定</el-button>
          <el-button size="small" @click="dialogVisible = false">取 消</el-button>
        </span>
      </el-dialog>
    </div>
</template>

<script>
import apiProcessInst from '@/api/workflow/processInst'
import taskApi from '@/api/workflow/task'
import HistoryBpmn from "@/components/Process/HistoryBpmn";
import BpmnViewer from 'bpmn-js/lib/NavigatedViewer';
import processApi from "@/api/workflow/processInst";
export default {
    props: {
      processInstanceId: String,
      editMessage: {
        type: Boolean,
        default: false
      },
    },
    components:{
      HistoryBpmn
    },
    data() {
      return {
        loading: false,
        list: [],
        dialogVisible: false,
        commentId: undefined,
        comment: undefined,
        attachmentList: [],
        taskId: undefined,
        historicProcessInstance: []
      }
    },
    watch: {
      processInstanceId: {
        handler(newVal,oldVal){
          if(newVal) {
            this.loading = true
            // 审批历史数据
            this.getHistoryInfoList()
            this.init(newVal)
          }
        },
        immediate: true,
        deep:true
      }
    },

    methods: {
        // 查询审批历史记录
        async getHistoryInfoList() {
            const { data } = await apiProcessInst.getHistoryInfoList(this.processInstanceId)
            this.list = data
            if(this.list[0].historicProcessInstance){
              this.historicProcessInstance.push(this.list[0].historicProcessInstance)
            }
            this.loading = false
        },
        // 打开编辑意见
        handleClick(row){
            this.attachmentList = []
            this.commentId = row.commentId
            this.comment = row.comment
            this.taskId = row.id
            if(row.fileList.length>0){
                this.attachmentList = row.fileList
            }
            this.dialogVisible = true
        },
        // 附件上传
        handleFileChange(file, fileList) {
            this.attachmentList = fileList
        },
        // 删除附件
        beforeRemove(file,fileList){
            this.$confirm('是否确认删除？', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                fileList.forEach((item,index) => {
                    if(item.uid === file.uid){
                        fileList.splice(index,1)
                    }
                })
                if(fileList.length === 0){
                    this.attachmentList = []
                }
                if(file.id){
                    taskApi.deleteAttachment(file.id).then(response => {
                        this.$modal.msgSuccess("删除成功");
                        this.getHistoryInfoList()
                    })
                } 
            })
            return false
        },
        // 附件上传
        async submitUpload() {
            if(this.attachmentList.length>0){
                const formData = new FormData()
                this.attachmentList.forEach((file) => {
                    if(file.raw){
                        formData.append('file', file.raw)
                    }
                })
                await taskApi.editAttachment(formData,this.taskId,this.processInstanceId)
            }
        },
        // 编辑意见
        async clickUpdate(){
            // 上传附件
            this.submitUpload()
            const {code,msg} = await taskApi.editComment(this.commentId,this.comment)
            if(code === 200){
                this.$modal.msgSuccess(msg);
                this.dialogVisible = false
                this.getHistoryInfoList()
            }
        },
        // 附件下载
        downloadFile(row) {
            if(row.id){
                this.download('workflow/task/downloadAttachment/'+row.id,{}, row.name+`${new Date().getTime()}`+'.'+row.type)
            }
        },
        init(processInstanceId) {
            this.loading = true
            this.$nextTick(()=>{
                if (this.modeler) this.modeler.destroy();
                this.modeler = new BpmnViewer({
                container: this.$refs.canvas,
                additionalModules:[
                    {
                    //禁止滚轮滚动
                    zoomScroll: ["value",""]
                    }
                ]
                })
                processApi.getXml(processInstanceId).then(response=>{        
                    this.xml = response.data.xml
                    this.taskList = response.data.taskList
                    this.createDiagram(this.xml)
                })
            })
            },
            async createDiagram(data) {
                try {
                    await this.modeler.importXML(data)
                    this.modeler.get('canvas').zoom(0.6)
                    this.fillColor()
                    this.loading = false
                } catch (err) {
                    //console.error(err.message, err.warnings)
                }
            },
            // 让图能自适应屏幕
            fitViewport(){
                this.autoViewport()
                this.autoViewport()
            },
            autoViewport(){
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
/* 修改滚动条样式 */
.el-table__body-wrapper::-webkit-scrollbar-thumb {
	border-radius: 10px;
}
.el-table__body-wrapper::-webkit-scrollbar {
  width: 5px;
}
.container-tab{
    height: calc(100vh - 440px);
    overflow-y: auto;
    padding: 10px;
}
/* 修改滚动条样式 */
.container-tab::-webkit-scrollbar {
    width: 4px;
}
.container-tab::-webkit-scrollbar-thumb {
    border-radius: 10px;
    background-color: #ccc;
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
/* 修改滚动条样式 */
.flow-containers::-webkit-scrollbar-thumb {
	border-radius: 10px;
}
.flow-containers::-webkit-scrollbar {
  width: 5px;
}
.bpmn-el-container{
  height: 500px;
}
.flow-containers {
    width: 100%;
    height: 100%;
    overflow-y: auto;
    .canvas {
        width: 100%;
        width: 100%;
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
