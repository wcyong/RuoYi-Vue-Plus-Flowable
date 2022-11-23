<template>
  <div>
    <el-form label-width="110px" :model="formData" :rules="rulesFrom" ref="formDataRef">
        <el-form-item label="流程定义Key" prop="processDefinitionKey">
          <el-input v-model="formData.processDefinitionKey" disabled></el-input>
        </el-form-item>
        <el-form-item label="流程定义名称" prop="processDefinitionName">
          <el-input v-model="formData.processDefinitionName" disabled></el-input>
        </el-form-item>
        <el-form-item label="表单类型" prop="businessType">
          <el-radio-group @change="change($event)" v-model="formData.businessType">
            <el-radio-button :label="1">业务表单</el-radio-button>
            <el-radio-button :label="0">动态表单</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="表单Key" prop="formKey" v-if="formData.businessType === 0">
          <el-input v-model="formData.formKey" placeholder="请选择表单" disabled style="width:185px;padding-right: 5px;"/>
          <el-button type="primary" @click="handerOpenForm" icon="el-icon-search"></el-button>
        </el-form-item>
        <el-form-item label="表单名称" prop="formName" v-if="formData.businessType === 0">
          <el-input v-model="formData.formName" disabled></el-input>
        </el-form-item>
        <el-form-item label="表单参数" v-if="formData.businessType === 0">
          <el-input type="textarea" placeholder="请输入表单参数,动态表单中参数id,多个用英文逗号隔开" v-model="formData.formVariable" @input="change($event)"/>
        </el-form-item>
        <el-form-item label="表名称" prop="tableName" v-if="formData.businessType === 1">
          <el-input v-model="formData.tableName" placeholder="请选择表名称" disabled style="width:185px;padding-right: 5px;"/>
          <el-button type="primary" @click="handerOpenTable" icon="el-icon-search"></el-button>
        </el-form-item>
        <el-form-item label="组件名称" prop="componentName" v-if="formData.businessType === 1">
          <el-input placeholder="请输入组件名称" v-model="formData.componentName"/>
        </el-form-item>
        <el-form-item label="备注" prop="remork" v-if="formData.businessType === 1">
          <el-input type="textarea" v-model="formData.remork"></el-input>
        </el-form-item>
    </el-form>
    <!-- 动态表单开始 -->
    <el-dialog title="表单" :visible.sync="formVisible" v-if="formVisible" width="70%" :close-on-click-modal="false" append-to-body>
      <div class="app-container">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="表单key" prop="formKey">
            <el-input
              v-model="queryParams.formKey"
              placeholder="请输入表单key"
              clearable
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item label="表单名称" prop="formName">
            <el-input
              v-model="queryParams.formName"
              placeholder="请输入表单名称"
              clearable
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :highlight-current-row="true" :data="dynamicFormList" @row-click="handleFormClick">
          <el-table-column label="主键" align="center" prop="id" v-if="false"/>
          <el-table-column label="表单key" align="center" prop="formKey" />
          <el-table-column label="表单名称" align="center" prop="formName" />
          <el-table-column label="表单备注" align="center" prop="formRemark" />
        </el-table>

        <pagination
          v-show="total>0"
          :total="total"
          :page.sync="queryParams.pageNum"
          :limit.sync="queryParams.pageSize"
          @pagination="getList"
        />
      </div>
    </el-dialog>
    <!-- 动态表单结束 -->
    <!-- 导入表开始 -->
    <el-dialog title="导入表" :visible.sync="tableVisible" width="800px" top="5vh" append-to-body>
        <el-form :model="tableQueryParams" ref="tableQueryForm" size="small" :inline="true">
        <el-form-item label="表名称" prop="tableName">
            <el-input
            v-model="tableQueryParams.tableName"
            placeholder="请输入表名称"
            clearable
            @keyup.enter.native="handleQuery"
            />
        </el-form-item>
        <el-form-item label="表描述" prop="tableComment">
            <el-input
            v-model="tableQueryParams.tableComment"
            placeholder="请输入表描述"
            clearable
            @keyup.enter.native="handleQuery"
            />
        </el-form-item>
        <el-form-item label="数据源名称" prop="dataName">
            <el-input
            v-model="tableQueryParams.dataName"
            placeholder="请输入数据源名称"
            clearable
            @keyup.enter.native="handleQuery"
            />
        </el-form-item>
        <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleTableQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetTableQuery">重置</el-button>
        </el-form-item>
        </el-form>
        <el-row>
            <el-table :highlight-current-row="true" @row-click="handleTableClick" ref="table" :data="dbTableList" height="260px">
                <el-table-column prop="tableName" label="表名称" :show-overflow-tooltip="true"></el-table-column>
                <el-table-column prop="tableComment" label="表描述" :show-overflow-tooltip="true"></el-table-column>
                <el-table-column prop="createTime" label="创建时间"></el-table-column>
                <el-table-column prop="updateTime" label="更新时间"></el-table-column>
            </el-table>
            <pagination
                v-show="dbTableTotal>0"
                :total="dbTableTotal"
                :page.sync="tableQueryParams.pageNum"
                :limit.sync="tableQueryParams.pageSize"
                @pagination="getDbList"
            />
        </el-row>
    </el-dialog>
    <!-- 导入表结束 -->
    <span class="btn-footer">
      <el-button type="danger" v-if="this.formData.id" @click="deleteForm()">重 置</el-button>
      <el-button type="primary" @click="submitForm('formDataRef')">确 定</el-button>
    </span>
  </div>
</template>

<script>
import { listDynamicFormEnable} from "@/api/workflow/dynamicForm";
import { listDbTable } from "@/api/tool/gen";
import { addProcessDefSetting,checkProcessDefSetting,delProcessDefSetting } from "@/api/workflow/processDefSetting";
export default {
  props:{
    dataObj: {
      type: Object,
      default:()=>{}
    }
  },
  data() {
    return {
      // 显示隐藏
      formVisible: false,
      tableVisible: false,
      // 按钮loading
      buttonLoading: false,
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 流程单表格数据
      dynamicFormList: [],
      // 弹出层标题
      title: "",
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        formKey: undefined,
        formName: undefined,
      },
      // 表数据
      dbTableList: [],
      // 总条数
      dbTableTotal: 0,
      // 查询参数
      tableQueryParams: {
        pageNum: 1,
        pageSize: 10,
        tableName: undefined,
        tableComment: undefined,
        dataName: 'master',
      },
      // 表单校验
      rulesFrom: {
        processDefinitionKey: [
          { required: true, message: '流程定义Key不能为空', trigger: 'blur' }
        ],
        processDefinitionName: [
          { required: true, message: '流程定义名称不能为空', trigger: 'blur' }
        ],
        formKey: [
          { required: true, message: '表单Key不能为空', trigger: 'blur' }
        ],
        formName: [
          { required: true, message: '表单名称不能为空', trigger: 'blur' }
        ],
        businessType: [
          { required: true, message: '业务类型不能为空', trigger: 'blur' }
        ],
        tableName: [
          { required: true, message: '表名称不能为空', trigger: 'blur' }
        ],
        componentName: [
          { required: true, message: '组件名称不能为空', trigger: 'blur' }
        ]
      },
      formData: {}
    };
  },
  watch: {
      // 根据名称筛选分类树
      dataObj(val) {
        this.formData = val
      }
   },
  methods: {
    change(e){
        this.$forceUpdate(e)
        this.$refs["formDataRef"].clearValidate()
    },
    /** 查询流程单列表 */
    getList() {
      this.loading = true;
      listDynamicFormEnable(this.queryParams).then(response => {
        this.dynamicFormList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
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
    // 选中数据
    handleFormClick(row) {
      this.$set(this.formData,'formId',row.id)
      this.$set(this.formData,'formKey',row.formKey)
      this.$set(this.formData,'formName',row.formName)
      this.formVisible = false;
    },
    // 选中数据
    handleTableClick(row){
      this.$set(this.formData,'tableName',row.tableName)
      this.tableVisible = false;
    },
    // 打开表单
    handerOpenForm(){
      this.getList();
      this.formVisible = true
    },
    // 表数据
    getDbList(){
      localStorage.setItem("dataName", this.tableQueryParams.dataName);
      listDbTable(this.tableQueryParams).then(res => {
        if (res.code === 200) {
          this.dbTableList = res.rows;
          this.dbTableTotal = res.total;
        }
      });
    },
    /** 打开表弹窗 */
    handerOpenTable() {
      this.tableVisible = true;
      this.getDbList()
    },
     /** 搜索按钮操作 */
    handleTableQuery() {
      this.tableQueryParams.pageNum = 1;
      this.getDbList();
    },
    /** 重置按钮操作 */
    resetTableQuery() {
      this.resetForm("tableQueryForm");
      this.handleTableQuery();
    },
    // 确认
    submitForm(formName){
      this.loading = true;
      this.$refs[formName].validate((valid) => {
       if (valid) {
        let param = {}
        if(this.formData.businessType === 0){
            param = {
                processDefinitionId: this.formData.processDefinitionId,
                businessType: 0,
                formId: this.formData.formId,
                id: this.formData.id
            } 
        }else{
            param = {
                processDefinitionId: this.formData.processDefinitionId,
                businessType: 1,
                componentName: this.formData.componentName,
                tableName: this.formData.tableName,
                id: this.formData.id
            } 
        }
        checkProcessDefSetting(param).then(response => {
          if(response.data){
            this.$confirm(response.msg, '提示', {
              confirmButtonText: '确定',
              cancelButtonText: '取消',
              type: 'warning'
            }).then(() => {
              this.formData.settingId = response.data
              addProcessDefSetting(this.formData).then(response => {
                this.formData = response.data
                this.$modal.msgSuccess("保存成功");
                this.loading = false;
                this.$emit("callbackFn")
              });
            })
          }else{
              addProcessDefSetting(this.formData).then(response => {
                this.formData = response.data
                this.$modal.msgSuccess("保存成功");
                this.loading = false;
                this.$emit("callbackFn")
              });
          }
        })
       }
      })
    },
    //删除设置
    deleteForm(){
      if(this.formData.id){
        this.$modal.confirm('确定删除重置？').then(() => {
          delProcessDefSetting(this.formData.id).then(response=>{
            this.$modal.msgSuccess("重置成功");
          })
        })
      }
    }
  }
};
</script>
<style scoped>
.line{
  padding-bottom: 20px;
}
.btn-footer{
  float: right;
  margin-top: 100px;
}
</style>
