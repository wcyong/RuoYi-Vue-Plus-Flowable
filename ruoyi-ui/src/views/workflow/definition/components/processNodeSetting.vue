<template>
  <el-dialog title="人员设置" v-dialogDrag :visible.sync="visible" v-if="visible" width="50%" :close-on-click-modal="false" append-to-body>
    <div class="container" v-loading="loading">
      <el-form style="height:inherit" ref="form" size="small" label-position="left" :model="form">
          <el-form-item label="环节名称">
            <el-tag v-if="nodeName">{{nodeName}}</el-tag><el-tag v-else>无</el-tag>
          </el-form-item>
          <el-row>
            <el-form-item>
                <el-col :span="24" style="line-height: 20px">
                  <el-alert title="每个节点设置，如有修改都请保存一次，跳转节点后数据不会自动保存！" type="warning" show-icon :closable="false"/>
                </el-col>
            </el-form-item>
          </el-row>
          <el-form-item v-if="form.index === 1" prop="chooseWay" label="选人方式">
            <el-radio-group @change="clearSelect(form.chooseWay)" v-model="form.chooseWay">
              <el-radio border label="person">选择人员</el-radio>
              <el-radio border label="role">选择角色</el-radio>
              <el-radio border label="dept">选择部门</el-radio>
              <el-radio border label="rule">业务规则</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-row v-if="form.index === 1">
            <el-col class="line" :span="8">
              <el-form-item label="是否弹窗选人" prop="isShow">
                <el-switch v-model="form.isShow"></el-switch>
              </el-form-item>
            </el-col>
            <el-col class="line" :span="8">
              <el-form-item label="是否能会签" prop="multiple">
                <el-switch disabled v-model="form.multiple"></el-switch>
              </el-form-item>
            </el-col>
            <el-col class="line" :span="8">
                <el-form-item label="是否能退回" prop="isBack">
                  <el-switch v-model="form.isBack"></el-switch>
                </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col class="line" :span="8">
              <el-form-item label="是否可以委托" prop="isDelegate">
                <el-switch v-model="form.isDelegate"></el-switch>
              </el-form-item>
            </el-col>
            <el-col class="line" :span="8">
              <el-form-item label="是否能转办" prop="isTransmit">
                <el-switch v-model="form.isTransmit"></el-switch>
              </el-form-item>
            </el-col>
            <el-col class="line" :span="8">
                <el-form-item label="是否能抄送" prop="isCopy">
                  <el-switch :disabled="form.end" v-model="form.isCopy"></el-switch>
                </el-form-item>
            </el-col>
          </el-row>
          <el-row v-if="form.index === 1">
            <el-col class="line" :span="8">
              <el-form-item label="是否自动审批" prop="autoComplete">
                <el-switch v-model="form.autoComplete"></el-switch>
                <el-tooltip class="item" effect="dark" content="当前节点与上一节点审批人相同自动审批，下一节点如果为弹窗选人则默认下一节点全部人员为候选人" placement="top-start">
                  <i class="el-icon-info" style="cursor: pointer;font-size: 15px;line-height: 15px;vertical-align: middle;padding-left: 10px;"></i>
                </el-tooltip>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row v-if="form.multiple">
            <el-col class="line" :span="8">
              <el-form-item label-width="100px" label="会签集合" prop="multipleColumn">
                <el-tag>{{form.multipleColumn}}</el-tag>
              </el-form-item>
            </el-col>
            <el-col class="line" :span="8">
              <el-form-item label="是否能加签" prop="addMultiInstance">
                <el-switch v-model="form.addMultiInstance"></el-switch>
              </el-form-item>
            </el-col>
            <el-col class="line" :span="8">
                <el-form-item label="是否能减签" prop="deleteMultiInstance">
                  <el-switch v-model="form.deleteMultiInstance"></el-switch>
                </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="20">
              <el-form-item label-width="100px" label="节点任务" prop="task">
                <el-popover
                  placement="right"
                  width="500"
                  trigger="click">
                  <el-button type="primary" size="mini" @click="addListener">添加</el-button>
                  <el-link style="padding-left:15px" type="info" :underline="false">说明：当前任务节点完成前或完成后执行(自动办理任务不会执行)</el-link>
                  <el-table :data="form.taskListenerList">
                    <el-table-column label="事件类型" width="150" align="center" prop="paramType" >
                        <template slot-scope="scope">
                            <el-select v-model="scope.row.eventType" placeholder="请选择">
                            <el-option
                              v-for="item in options"
                              :key="item.value"
                              :label="item.label"
                              :value="item.value">
                            </el-option>
                          </el-select>
                        </template>
                    </el-table-column>
                    <el-table-column label="bean名称" align="center" prop="paramType" >
                        <template slot-scope="scope">
                            <el-input v-model="scope.row.beanName"/>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="80">
                        <template slot-scope="scope">
                            <el-button @click="deleteListener(scope.$index)" type="danger" size="small">删除</el-button>
                        </template>
                    </el-table-column>
                  </el-table>
                  <el-badge :value="form.taskListenerList.length" slot="reference" class="item">
                    <el-button type="primary" icon="el-icon-circle-plus-outline">节点任务</el-button>
                  </el-badge>
                </el-popover>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="20">
              <el-form-item label-width="100px" label="字段配置">
                <el-badge :value="form.fieldList.length" class="item">
                  <el-button type="primary" @click="openFieldClick" icon="el-icon-s-help">字段配置</el-button>
                </el-badge>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row v-if="form.index === 1">
            <el-col :span="20">
              <el-form-item label-width="100px" label="审批人员" prop="assignee">
                <el-input readonly v-model="form.assignee" placeholder="审批人员">
                  <el-button type="primary" slot="append" @click="openSelect" v-text="btnText"></el-button>
                  <el-button type="success" slot="append" @click="clearSelect">清空</el-button>
                </el-input>
                <el-input v-model="form.assigneeId" v-show="false" placeholder="审批人员ID"/>
              </el-form-item>
            </el-col>
          </el-row>
      </el-form>
      <div style="float:right;position:relative;bottom:20px;">
        <el-button type="primary" size="small" @click="onSubmit">保存</el-button>
        <el-button type="danger" size="small" v-if="form.index === 1" @click="del">重置</el-button>
      </div>
    </div>
    <!-- 选择人员 -->
    <sys-dept-user ref="userRef" @confirmUser="clickUser" :propUserList = 'propUserList'/>
    <!-- 选择角色 -->
    <sys-role ref="roleRef" @confirmUser="clickRole" :propRoleList = 'propRoleList'/>
    <!-- 选择部门 -->
    <sys-dept ref="deptRef" @confirmUser="clickDept" :propDeptList = 'propDeptList'/>
    <!-- 选择业务规则 -->
    <process-Rule ref="processRuleRef" @primary="clickRule" :propDeptList = 'propDeptList'/>
    <!-- 字段配置 -->
    <processFieldSetting ref="processFieldSettingRef" @fieldClick="fieldClick"/>
  </el-dialog>
</template>

<script>
import  SysDeptUser from "@/views/components/user/sys-dept-user";
import  SysRole from "@/views/components/role/sys-role";
import  SysDept from "@/views/components/dept/sys-dept";
import  ProcessRule from "@/views/workflow/definition/components/processRule";
import  processFieldSetting from "@/views/workflow/definition/components/processFieldSetting";
import {getInfoSetting,add,del} from "@/api/workflow/actNodeAssginee";

export default {
    components: {
       SysDeptUser,
       SysRole,
       SysDept,
       ProcessRule,
       processFieldSetting
    },
    data() {
      return {
        loading: false,
        visible: false,
        form: {
          isShow: true,
          isBack: false,
          multiple: false,
          chooseWay: undefined,
          isDelegate: false,
          isTransmit: false,
          isCopy: false,
          autoComplete: false,
          addMultiInstance: false,
          deleteMultiInstance: false,
          taskListenerList:[],
          fieldList:[]
        },
        // 按钮值
        btnText:"选择人员",
        // 流程定义id
        definitionId: null,
        // 环节名称
        nodeName: null,
        // 人员选择
        propUserList: [],
        // 角色选择
        propRoleList: [],
        // 部门选择
        propDeptList: [],
        options: [{
          value: 'before',
          label: '完成前'
        }, {
          value: 'after',
          label: '完成后'
        }]
      }
    },
    methods: {
        //切换节点
        changeSteps(definitionId,nodeId,nodeName) {
          this.visible = true
          this.form.assignee = undefined
          this.form.multipleColumn = undefined
          this.form.multiple = false
          this.loading = true
          this.nodeName = nodeName
          getInfoSetting(definitionId,nodeId).then(response => {
              if(response.code === 200){
                  this.form = response.data
                  this.form.nodeName = response.data.nodeName
                  this.loading = false
                  if(this.form.id === undefined){
                      this.form.isBack = false
                  }
                  if(this.form.chooseWay === "person"){
                      this.btnText = "选择人员"
                  }else if(this.form.chooseWay === "role"){
                      this.btnText = "选择角色"
                  }else if(this.form.chooseWay === "dept"){
                      this.btnText = "选择部门"
                  }else if(this.form.chooseWay === "rule"){
                      this.btnText = "选择规则"
                  }
                  this.$forceUpdate()
              }
          })
        },
        //保存设置
        onSubmit(){
          if(this.nodeName){
            this.loading = true
            this.form.nodeName = this.nodeName
            add(this.form).then(response => {
              this.form = response.data
              this.visible = false
              this.loading = false
              this.$modal.msgSuccess("保存成功")
            })
          }else{
            this.$modal.msgError("请选择节点")
          }
        },
        // 删除
        del(){
          if(this.form.id){
             del(this.form.id).then(response => {
              if(response.code === 200){
                this.$modal.msgSuccess("重置成功")
                this.form.id = null
              }
             })
          }else{
              this.$modal.msgSuccess("重置成功")
          }
        },
        // 重置
        reset(){
          this.form.assigneeId =undefined
          this.form.assignee = undefined
          this.form.chooseWay = 'person'
          this.form.processDefinitionId = this.definitionId
        },
        // 添加参数
        addListener(){
            let param = {
                eventType:'',
                beanName:''
            }
            this.form.taskListenerList.push(param);
        },
        // 删除参数
        deleteListener(index){
          this.form.taskListenerList.splice(index,1)
        },
        //清空选择的人员
        clearSelect(chooseWay){
          this.form.assigneeId = ""
          this.form.assignee = ""
          if(chooseWay === "person"){
            this.btnText = "选择人员"
          }else if(chooseWay === "role"){
            this.btnText = "选择角色"
          }else if(chooseWay === "dept"){
            this.btnText = "选择部门"
          }else if(chooseWay === "rule"){
            this.btnText = "选择规则"
          }
        },
        //选择弹出层
        async openSelect(){
          if(this.form.chooseWay === 'person'){
            this.propUserList = [];
            if(this.form.assigneeId){
              let userIds = this.form.assigneeId.split( ',' )
              if(userIds.length>0){
                this.propUserList = userIds
              }
            }
            this.$refs.userRef.visible = true
          }else if(this.form.chooseWay === 'role'){
           this.propRoleList = [];
            if(this.form.assigneeId){
              let roleIds = this.form.assigneeId.split( ',' )
              if(roleIds.length>0){
                this.propRoleList = roleIds
              }
            }
            this.$refs.roleRef.visible = true
          }else if(this.form.chooseWay === 'dept'){
            this.propDeptList = [];
            if(this.form.assigneeId){
              let deptIds = this.form.assigneeId.split( ',' )
              if(deptIds.length>0){
                this.propDeptList = deptIds
              }
            }
            this.$refs.deptRef.visible = true
          }else if(this.form.chooseWay === 'rule'){
            this.$refs.processRuleRef.visible = true
          }
        },
        //选择人员
        clickUser(userList){
          let arrAssignee= userList.map(item => {
            return item.nickName
          })
          let arrAssigneeId= userList.map(item => {
            return item.userId
          })
          let resultAssignee = arrAssignee.join(",")
          let resultAssigneeId = arrAssigneeId.join(",")
          this.$set(this.form,'assignee',resultAssignee)
          this.$set(this.form,'assigneeId',resultAssigneeId)
          this.$refs.userRef.visible = false
          this.$forceUpdate()
        },
        //选择角色
        clickRole(roleList){
          let arrAssignee= roleList.map(item => {
            return item.roleName
          })
          let arrAssigneeId= roleList.map(item => {
            return item.roleId
          })
          let resultAssignee = arrAssignee.join(",")
          let resultAssigneeId = arrAssigneeId.join(",")
          this.$set(this.form,'assignee',resultAssignee)
          this.$set(this.form,'assigneeId',resultAssigneeId)
          this.$refs.roleRef.visible = false
        },
        //选择部门
        clickDept(deptList){
          let arrAssignee= deptList.map(item => {
            return item.deptName
          })
          let arrAssigneeId= deptList.map(item => {
            return item.deptId
          })
          let resultAssignee = arrAssignee.join(",")
          let resultAssigneeId = arrAssigneeId.join(",")
          this.$set(this.form,'assignee',resultAssignee)
          this.$set(this.form,'assigneeId',resultAssigneeId)
          this.$refs.deptRef.visible = false
        },
        //业务规则
        clickRule(rule){
          this.$set(this.form,'assignee',rule.beanName+"."+rule.method)
          this.$set(this.form,'assigneeId',rule.beanName+"."+rule.method)
          this.$set(this.form,'businessRuleId',rule.id)
          this.$refs.processRuleRef.visible = false
        },
        //字段配置
        openFieldClick(){
          this.$refs.processFieldSettingRef.init(this.form.fieldList)
        },
        //字段配置确认
        fieldClick(fieldList){
          this.form.fieldList = fieldList
        },
    }
}
</script>
<style scoped>
    .container {
        height: 550px;
    }
    .tabs{
        height: 550px;
    }
</style>



