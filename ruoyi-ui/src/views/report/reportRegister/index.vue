<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="报表编码" prop="reportCode">
        <el-input
          v-model="queryParams.reportCode"
          placeholder="请输入报表编码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="报表名称" prop="reportName">
        <el-input
          v-model="queryParams.reportName"
          placeholder="请输入报表名称"
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
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['report:reportRegister:add']"
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
          v-hasPermi="['report:reportRegister:edit']"
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
          v-hasPermi="['report:reportRegister:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['report:reportRegister:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="reportRegisterList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键" align="center" prop="id" v-if="false"/>
      <el-table-column label="报表编码" align="center" prop="reportCode" />
      <el-table-column label="报表名称" align="center" prop="reportName" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['report:reportRegister:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['report:reportRegister:remove']"
          >删除</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-postcard"
            @click="handleAuth(scope.row)"
            v-hasPermi="['report:reportRegister:auth']"
          >授权</el-button>
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

    <!-- 添加或修改报表注册对话框 -->
    <el-dialog :title="title" :visible.sync="open" v-if="open" width="800px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="报表编码" prop="reportCode">
          <el-input v-model="form.reportCode" :disabled="true" placeholder="请输入报表编码" >
            <el-button slot="append" icon="el-icon-search" @click="openDbList"></el-button>
          </el-input>
        </el-form-item>
        <el-form-item label="报表名称" prop="reportName">
          <el-input v-model="form.reportName" :disabled="true" placeholder="请输入报表名称" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button :loading="buttonLoading" type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="报表列表" :visible.sync="openDb" v-if="openDb" width="800px" append-to-body>
      <div style="height:450px">
        <el-form :model="queryDbParams" ref="queryDbForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="报表编码" prop="dbCode">
            <el-input
              v-model="queryDbParams.dbCode"
              placeholder="请输入报表编码"
              clearable
              @keyup.enter.native="getDbList"
            />
          </el-form-item>
          <el-form-item label="报表名称" prop="name">
            <el-input
              v-model="queryDbParams.name"
              placeholder="请输入报表名称"
              clearable
              @keyup.enter.native="getDbList"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="getDbList">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetDbQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <el-table
          height="350px"
          :data="dbLiist"
          border
          :highlight-current-row="true"
          @row-click = "onRowClick"
          style="width: 100%">
          <el-table-column
            prop="dbCode"
            label="报表编码"
            width="180">
          </el-table-column>
          <el-table-column
            prop="name"
            label="报表名称"
            width="180">
          </el-table-column>
          <el-table-column
            prop="type"
            label="类型">
          </el-table-column>
        </el-table>
        <pagination
          v-show="dbTotal>0"
          :total="total"
          :page.sync="queryDbParams.pageNum"
          :limit.sync="queryDbParams.pageSize"
          @pagination="getDbList"
        />
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="openDb = false">取 消</el-button>
        <el-button type="primary" @click="confirmDbData()">确 定</el-button>
      </span>
    </el-dialog>
    <reoprtAuth :reportRegisterId="reportRegisterId" ref="reoprtAuthRef"/>
  </div>
</template>

<script>
import { selectReportDbPage, listReportRegister, getReportRegister, delReportRegister, addReportRegister, updateReportRegister } from "@/api/report/reportRegister";
import reoprtAuth from "@/views/report/reportRegister/reoprtAuth";

export default {
  name: "ReportRegister",
  components: {
    reoprtAuth
  },
  data() {
    return {
      // 按钮loading
      buttonLoading: false,
      // 遮罩层
      loading: true,
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
      // 报表注册表格数据
      reportRegisterList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        reportCode: undefined,
        reportName: undefined,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        id: [
          { required: true, message: "主键不能为空", trigger: "blur" }
        ],
        reportCode: [
          { required: true, message: "报表编码不能为空", trigger: "blur" }
        ],
        reportName: [
          { required: true, message: "报表名称不能为空", trigger: "blur" }
        ]
      },
      // 查询参数
      queryDbParams: {
        pageNum: 1,
        pageSize: 10,
        dbCode: undefined,
        name: undefined,
      },
      // 报表数据
      dbLiist: [],
      // 报表数据条数
      dbTotal: 0,
      // 是否显示弹出层
      openDb: false,
      // 当前行报表数据
      dbData: {},
      // 报表注册id
      reportRegisterId: ''
    };
  },
  created() {
    this.getList();
  },
  methods: {
    handleAuth(row){
      this.reportRegisterId = row.id
      this.$refs.reoprtAuthRef.visible = true
    },
    // 当前行数据
    onRowClick (row, event, column) {
      this.dbData = row
    },
    // 确认
    confirmDbData(){
      this.form.reportCode = this.dbData.dbCode
      this.form.reportName = this.dbData.name
      this.form.reportId = this.dbData.id
      this.openDb = false
    },
    // 打开报表数据
    openDbList(){
      this.openDb = true
      selectReportDbPage(this.queryDbParams).then(response => {
        this.dbLiist = response.rows;
        this.dbTotal = response.total;
      });
    },
    // 报表数据
    getDbList(){
      selectReportDbPage(this.queryDbParams).then(response => {
        this.dbLiist = response.rows;
        this.dbTotal = response.total;
      });
    },
    /** 重置按钮操作 */
    resetDbQuery() {
      this.resetForm("queryDbForm");
      this.getDbList();
    },
    /** 查询报表注册列表 */
    getList() {
      this.loading = true;
      listReportRegister(this.queryParams).then(response => {
        this.reportRegisterList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        id: undefined,
        reportCode: undefined,
        reportName: undefined,
        remark: undefined,
        createBy: undefined,
        updateBy: undefined,
        createTime: undefined,
        updateTime: undefined
      };
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
      this.reset();
      this.open = true;
      this.title = "添加报表注册";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.loading = true;
      this.reset();
      const id = row.id || this.ids
      getReportRegister(id).then(response => {
        this.loading = false;
        this.form = response.data;
        this.open = true;
        this.title = "修改报表注册";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.buttonLoading = true;
          if (this.form.id != null) {
            updateReportRegister(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            }).finally(() => {
              this.buttonLoading = false;
            });
          } else {
            addReportRegister(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            }).finally(() => {
              this.buttonLoading = false;
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除报表注册编号为"' + ids + '"的数据项？').then(() => {
        this.loading = true;
        return delReportRegister(ids);
      }).then(() => {
        this.loading = false;
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {
      }).finally(() => {
        this.loading = false;
      });
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('report/reportRegister/export', {
        ...this.queryParams
      }, `reportRegister_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
