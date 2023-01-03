<template>
    <div>
        <component :is="currProcessForm" 
            :businessKey="businessKey" 
            :parentTaskId="parentTaskId" 
            @closeForm="closeForm" 
            :taskId="taskId" 
            :processInstanceId="processInstanceId"
        ></component>
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

export default {
    props: {
      processInstanceId: String, // 流程实例id
      businessKey: String, // 业务唯一标识
      taskId: String, // 任务id
      parentTaskId: String, // 父级任务id
      currProcessForm: String, // 当前流程表单组件
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
        }
    }

}
</script>

