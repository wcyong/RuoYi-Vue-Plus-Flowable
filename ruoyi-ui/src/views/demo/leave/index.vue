<template>
  <div class="app-container">
    <div v-if="dataVisible">
        <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
            <el-form-item label="申请人" prop="username">
                <el-input
                v-model="queryParams.username"
                placeholder="请输入申请人"
                clearable
                size="small"
                @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="请假时长" prop="duration">
                <el-input
                v-model="queryParams.duration"
                placeholder="请输入请假时长"
                clearable
                size="small"
                @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="请假类型" prop="leaveType">
                <el-select v-model="queryParams.leaveType" placeholder="请选择请假类型" clearable size="small">
                <el-option
                    v-for="dict in dict.type.bs_leave_type"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                />
                </el-select>
            </el-form-item>
            <el-form-item label="标题" prop="title">
                <el-input
                v-model="queryParams.title"
                placeholder="请输入标题"
                clearable
                size="small"
                @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item>
                <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
                <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
            </el-form-item>
            </el-form>

            <el-form label-width="68px">
            <el-form-item label="流程Key" prop="processKey">
                <el-input
                v-model="processKey"
                placeholder="请输入流程Key"
                clearable
                size="small"
                style="width:200px"
                />
            </el-form-item>
            </el-form>

            <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
                <el-button
                type="primary"
                plain
                icon="el-icon-plus"
                size="mini"
                @click="handleAdd"
                v-hasPermi="['demo:leave:add']"
                >新增</el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                type="success"
                plain
                icon="el-icon-edit"
                size="mini"
                :disabled="single"
                @click="handleUpdate"
                v-hasPermi="['demo:leave:edit']"
                >修改</el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                type="danger"
                plain
                icon="el-icon-delete"
                size="mini"
                :disabled="multiple"
                @click="handleDelete"
                v-hasPermi="['demo:leave:remove']"
                >删除</el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                type="warning"
                plain
                icon="el-icon-download"
                size="mini"
                :loading="exportLoading"
                @click="handleExport"
                v-hasPermi="['demo:leave:export']"
                >导出</el-button>
            </el-col>
            <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
            </el-row>

            <el-table v-loading="loading" :data="leaveList" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="55" align="center" />
            <el-table-column label="主键ID" align="center" prop="id" v-if="false"/>
            <el-table-column label="申请人" align="center" prop="username" />
            <el-table-column label="请假时长" align="center" prop="duration" />
            <el-table-column label="工作委托人" align="center" prop="principal" />
            <el-table-column label="联系电话" align="center" prop="contactPhone" />
            <el-table-column label="请假类型" align="center" prop="leaveType">
                <template slot-scope="scope">
                <dict-tag :options="dict.type.bs_leave_type" :value="scope.row.leaveType"/>
                </template>
            </el-table-column>
            <el-table-column label="标题" align="center" prop="title" />
            <el-table-column label="请假原因" align="center" prop="leaveReason" />
            <el-table-column label="请假开始时间" align="center" prop="startDate" width="180">
                <template slot-scope="scope">
                <span>{{ parseTime(scope.row.startDate, '{y}-{m}-{d}') }}</span>
                </template>
            </el-table-column>
            <el-table-column label="请假结束时间" align="center" prop="endDate" width="180">
                <template slot-scope="scope">
                <span>{{ parseTime(scope.row.endDate, '{y}-{m}-{d}') }}</span>
                </template>
            </el-table-column>
            <el-table-column label="流程状态" align="center" prop="actBusinessStatus.status">
                <template slot-scope="scope">
                <dict-tag :options="dict.type.act_status" :value="scope.row.actBusinessStatus.status"/>
                </template>
            </el-table-column>
            <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
                <template slot-scope="scope">
                <el-button
                    v-if="scope.row.actBusinessStatus.status==='draft'||scope.row.actBusinessStatus.status==='back'||scope.row.actBusinessStatus.status==='cancel'"
                    size="mini"
                    type="text"
                    icon="el-icon-edit"
                    @click="handleUpdate(scope.row)"
                    v-hasPermi="['demo:leave:edit']"
                >修改</el-button>
                <el-button
                    v-if="scope.row.actBusinessStatus.status==='draft'||scope.row.actBusinessStatus.status==='back'||scope.row.actBusinessStatus.status==='cancel'"
                    size="mini"
                    type="text"
                    icon="el-icon-delete"
                    @click="handleDelete(scope.row)"
                    v-hasPermi="['demo:leave:remove']"
                >删除</el-button>
                <el-button
                    v-if="scope.row.actBusinessStatus.status==='waiting'"
                    size="mini"
                    type="text"
                    icon="el-icon-back"
                    @click="cancelProcessApply(scope.row)"
                >撤销</el-button>
                <el-button
                    v-if="scope.row.actBusinessStatus.status!=='draft'"
                    size="mini"
                    type="text"
                    icon="el-icon-view"
                    @click="handleView(scope.row)"
                >查看</el-button>
                </template>
            </el-table-column>
            </el-table>

            <pagination
            v-show="total>0"
            :total="total"
            :page.sync="queryParams.pageNum"
            :limit.sync="queryParams.pageSize"
            @pagination="getList"
            />
    </div>
    
    <!-- 单据信息开始 -->
    <div class="form-container" v-if="formVisible">
        <div class="form-container-header"><i class="el-dialog__close el-icon el-icon-close" @click="closeForm"></i></div>
        <div style="height: 45px;margin-top: -30px;">
          <el-button :loading="buttonLoading" size="small" v-if="flag!=='view'" type="primary" @click="submitForm()">提交</el-button>
          <el-button @click="bpmnProcess" size="small" v-if="processInstanceId">流程进度</el-button>
          <el-button @click="bpmnRecord" size="small" v-if="processInstanceId">审批意见</el-button>
          <el-button @click="viewReport('leave_code')" v-if="flag!=='add'" size="small">查看报表</el-button>
          <el-button @click="closeForm" size="small">关闭</el-button>
        </div>
        <el-tabs  type="border-card" class="container-tab">
            <el-tab-pane label="业务单据" v-loading="loading">
                <el-form ref="form" :model="form" :rules="rules" label-width="120px">
                  <el-form-item label="申请人用户名" prop="username">
                      <el-input v-model="form.username" placeholder="请输入申请人用户名" />
                  </el-form-item>
                  <el-form-item label="请假时长" prop="duration">
                      <el-input v-model="form.duration" placeholder="请输入请假时长，单位：天" />
                  </el-form-item>
                  <el-form-item label="工作委托人" prop="principal">
                      <el-input v-model="form.principal" placeholder="请输入工作委托人" />
                  </el-form-item>
                  <el-form-item label="联系电话" prop="contactPhone">
                      <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
                  </el-form-item>
                  <el-form-item label="请假类型" prop="leaveType">
                      <el-select v-model="form.leaveType" placeholder="请选择请假类型">
                      <el-option
                          v-for="dict in dict.type.bs_leave_type"
                          :key="dict.value"
                          :label="dict.label"
                          :value="parseInt(dict.value)"
                      ></el-option>
                      </el-select>
                  </el-form-item>
                  <el-form-item label="标题" prop="title">
                      <el-input v-model="form.title" placeholder="请输入标题" />
                  </el-form-item>
                  <el-form-item label="请假原因" prop="leaveReason">
                      <el-input v-model="form.leaveReason" type="textarea" placeholder="请输入内容" />
                  </el-form-item>
                  <el-form-item label="请假开始时间" prop="startDate">
                      <el-date-picker clearable size="small"
                      v-model="form.startDate"
                      type="datetime"
                      value-format="yyyy-MM-dd HH:mm:ss"
                      placeholder="选择请假开始时间">
                      </el-date-picker>
                  </el-form-item>
                  <el-form-item label="请假结束时间" prop="endDate">
                      <el-date-picker clearable size="small"
                      v-model="form.endDate"
                      type="datetime"
                      value-format="yyyy-MM-dd HH:mm:ss"
                      placeholder="选择请假结束时间">
                      </el-date-picker>
                  </el-form-item>
                </el-form>
            </el-tab-pane>
        </el-tabs>
    </div>
    <!-- 单据信息结束 -->
    <!-- 提交 -->
    <verify ref="verifyRef" @submitCallback="submitCallback" :taskId="taskId" :taskVariables="taskVariables" :sendMessage="sendMessage"></verify>
    <!-- 流程进度 -->
    <HistoryBpmnDialog ref="historyBpmnRef"/>
    <!-- 审批意见 -->
    <HistoryRecordDialog ref="historyRecordRef"/>
    <!-- 报表 -->
    <JimuReport ref="jimuReportRef" :reportCode="reportCode" :params="params"/>
    
  </div>
</template>

<script>
import { listLeave, getLeave, delLeave, addLeave, updateLeave } from "@/api/demo/leave";
import processApi from "@/api/workflow/processInst";
import verify from "@/components/Process/Verify";
import HistoryBpmnDialog from "@/components/Process/HistoryBpmnDialog";
import HistoryRecordDialog from "@/components/Process/HistoryRecordDialog";
import JimuReport from "@/components/JimuReport/index";
export default {
  name: "Leave",
  dicts: ['bs_leave_type','act_status'],
  components: {
    HistoryBpmnDialog,
    HistoryRecordDialog,
    verify,
    JimuReport
  },
  data() {
    return {
      // 流程实例
      processInstanceId: '',
      // 按钮loading
      buttonLoading: false,
      // 遮罩层
      loading: true,
      // 导出遮罩层
      exportLoading: false,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 请假业务表格数据
      leaveList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      dataVisible: true,
      formVisible: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        username: undefined,
        duration: undefined,
        leaveType: undefined,
        title: undefined
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        id: [
          { required: true, message: "主键ID不能为空", trigger: "blur" }
        ],
        username: [
          { required: true, message: "申请人用户名不能为空", trigger: "blur" }
        ],
        duration: [
          { required: true, message: "请假时长，单位：天不能为空", trigger: "blur" }
        ],
        principal: [
          { required: true, message: "工作委托人不能为空", trigger: "blur" }
        ],
        contactPhone: [
          { required: true, message: "联系电话不能为空", trigger: "blur" }
        ],
        leaveType: [
          { required: true, message: "请假类型不能为空", trigger: "change" }
        ],
        title: [
          { required: true, message: "标题不能为空", trigger: "blur" }
        ],
        leaveReason: [
          { required: true, message: "请假原因不能为空", trigger: "blur" }
        ],
        startDate: [
          { required: true, message: "请假开始时间不能为空", trigger: "blur" }
        ],
        endDate: [
          { required: true, message: "请假结束时间不能为空", trigger: "blur" }
        ]
      },
      taskVariables: {}, //流程变量
      taskId: undefined, //任务id
      // 消息提醒
      sendMessage: {},
      processKey: 'leave',
      bpmnProcessInstanceId: undefined,
      flag:'',
      //报表code
      reportCode: '',
      //参数
      params: ''
    };
  },
  created() {
    this.getList();
  },
  methods: {
    //查看报表
    viewReport(reportCode){
      this.reportCode = reportCode
      this.$refs.jimuReportRef.visible = true
      this.params = '&paramId='+this.form.id
    },
    //流程进度
    bpmnProcess(){
      this.$refs.historyBpmnRef.init(this.processInstanceId)
    },
    //审批意见
    bpmnRecord(){
      this.$refs.historyRecordRef.init(this.processInstanceId)
    },
    /** 查询请假业务列表 */
    getList() {
      this.loading = true;
      listLeave(this.queryParams).then(response => {
        this.leaveList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 关闭表单
    closeForm(){
      this.dataVisible = true;
      this.formVisible = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        id: undefined,
        username: undefined,
        duration: undefined,
        principal: undefined,
        contactPhone: undefined,
        leaveType: undefined,
        title: undefined,
        leaveReason: undefined,
        startDate: undefined,
        endDate: undefined
      };
      this.processInstanceId = undefined
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.flag = 'add'
      this.reset();
      this.dataVisible = false;
      this.formVisible = true
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.flag = 'update'
      this.loading = true;
      this.reset();
      const id = row.id || this.ids
      getLeave(id).then(response => {
        this.loading = false;
        this.form = response.data;
        this.$nextTick(() => {
          this.processInstanceId=response.data.actBusinessStatus.processInstanceId
        })
        this.dataVisible = false;
        this.formVisible = true
        this.sendMessage = {
          title:'请假申请',
          messageContent:'单据【'+row.id+"】申请"
        }
      });
    },
    //查看
    handleView(row){
      this.flag = 'view'
      this.loading = true;
      this.reset();
      const id = row.id || this.ids
      getLeave(id).then(response => {
        this.loading = false;
        this.form = response.data;
        this.$nextTick(() => {
          this.processInstanceId=response.data.actBusinessStatus.processInstanceId
        })
        this.dataVisible = false;
        this.formVisible = true
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.buttonLoading = true;
          if (this.form.id != null) {
            updateLeave(this.form).then(response => {
              this.form = response.data
              this.submitFormApply(response.data)
            }).finally(() => {
              this.buttonLoading = false;
            });
          } else {
            addLeave(this.form).then(response => {
              this.form = response.data
              this.submitFormApply(response.data)
            }).finally(() => {
              this.buttonLoading = false;
            });
          }
        }
      });
    },
    // 提交成功回调
    submitCallback(){
      this.dataVisible = true;
      this.formVisible = false
      this.getList();
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除请假业务编号为"' + ids + '"的数据项？').then(() => {
        this.loading = true;
        return delLeave(ids);
      }).then(() => {
        this.loading = false;
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).finally(() => {
        this.loading = false;
      });
    },
    /** 导出按钮操作 */
    handleExport() {
         this.download('/demo/leave/export', {
        ...this.queryParams
      }, `demo_${new Date().getTime()}.xlsx`)
    },
    //提交流程
    submitFormApply(entity){
        let assigneeList = []
        assigneeList.push(1)
        assigneeList.push(2)
        //流程变量
        this.taskVariables = {
            entity: entity,
            userId: '1',
            //assigneeList: assigneeList
        }
        const data = {
            processKey: this.processKey, // key
            businessKey: entity.id, // 业务id
            variables: this.taskVariables,
            tableName: 'bs_leave'
        }
        // 启动流程
        processApi.startProcessApply(data).then(response => {
            this.taskId = response.data.taskId;
            this.$refs.verifyRef.visible = true
            this.$refs.verifyRef.reset()
        })
    },
    //撤回
    cancelProcessApply(row){
         this.$modal.confirm('是否撤销申请').then(() => {
            this.loading = true;
            return processApi.cancelProcessApply(row.actBusinessStatus.processInstanceId);
         }).then(() => {
            this.getList();
            this.$modal.msgSuccess("撤回成功");
         }).finally(() => {
            this.loading = false;
         });
    }
  }
};
</script>
<style scoped>
    .container-tab{
        height: calc(100vh - 160px);
        overflow-y: auto;
    }
    .form-container-header{
        height: 20px;
        padding-bottom: 10px;
    }
    .el-icon-close{
        float: right;
        cursor: pointer;
    }
</style>
