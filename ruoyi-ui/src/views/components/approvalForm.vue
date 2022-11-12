<template>
    <div>
        <el-tabs type="border-card">
            <el-tab-pane label="业务单据" v-loading="loading" class="container-tab">
                <component :is="currProcessForm" 
                    :businessKey="businessKey" 
                    :parentTaskId="parentTaskId" 
                    @closeForm="closeForm" 
                    :taskId="taskId" 
                    :buildData="dynamicFormData.formText"
                    v-model="dynamicFormData.formValue"
                    :dynamicFormData="dynamicFormData"
                ></component>
            </el-tab-pane>
        </el-tabs>
        <!-- 流程进度 -->
        <HistoryBpmnDialog ref="historyBpmnRef"/>
        <!-- 审批意见 -->
        <HistoryRecordDialog ref="historyRecordRef"/>
    </div>
</template>

<script>
// 导入@/views/components/form 目录下所有组件
const allComponents = require.context('@/views/components/form', false,/\.vue$/)
// 组件名: 引用的组件
let components = {}
allComponents.keys().forEach(fileName => {
  let componentName = allComponents(fileName)
  components[fileName.replace(/^\.\/(.*)\.\w+$/, '$1')] = componentName.default
})
import HistoryRecordDialog from "@/components/Process/HistoryRecordDialog";
components['HistoryRecordDialog'.replace(/^\.\/(.*)\.\w+$/, '$1')] = HistoryRecordDialog
import HistoryBpmnDialog from "@/components/Process/HistoryBpmnDialog";
components['HistoryBpmnDialog'.replace(/^\.\/(.*)\.\w+$/, '$1')] = HistoryBpmnDialog

export default {
    props: {
      processInstanceId: String, // 流程实例id
      businessKey: String, // 业务唯一标识
      taskId: String, // 任务id
      parentTaskId: String, // 父级任务id
      currProcessForm: String, // 当前流程表单组件
      dynamicFormData: {} //动态表单数据
    },
    components: components,
    data() {
      return {
        loading: false,
        visible: false,
        url: null,
        list: []
      }
    },
    methods: {
        // 关闭弹窗
        closeForm(){
          this.visible = false
          this.$emit("closeForm")
        },
        bpmnProcess(){
          this.$refs.historyBpmnRef.init(true,this.processInstanceId)
        },
        bpmnRecord(){
          this.$refs.historyRecordRef.init(true,this.processInstanceId)
        }
    }

}
</script>
<style scoped>
.container-tab{
    height: calc(100vh - 225px);
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
</style>
