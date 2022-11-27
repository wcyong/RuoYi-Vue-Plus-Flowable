<template>
  <div>
    <!-- 导入表开始 -->
    <el-dialog title="导入表(请在代码生成菜单导入)" :visible.sync="tableVisible" v-if="tableVisible" width="800px" top="5vh" append-to-body>
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
            <el-table :highlight-current-row="true" @row-click="handleTableClick" ref="table" :data="tableList" height="260px">
                <el-table-column prop="tableName" label="表名称" :show-overflow-tooltip="true"></el-table-column>
                <el-table-column prop="tableComment" label="表描述" :show-overflow-tooltip="true"></el-table-column>
                <el-table-column prop="createTime" label="创建时间"></el-table-column>
                <el-table-column prop="updateTime" label="更新时间"></el-table-column>
            </el-table>
            <pagination
                v-show="tableTotal>0"
                :total="tableTotal"
                :page.sync="tableQueryParams.pageNum"
                :limit.sync="tableQueryParams.pageSize"
                @pagination="getDbList"
            />
        </el-row>
    </el-dialog>
    <!-- 导入表结束 -->
  </div>
</template>

<script>
import { listTable } from "@/api/tool/gen";
export default {
  data() {
    return {
      // 表数据
      tableList: [],
      // 总条数
      tableTotal: 0,
      // 查询参数
      tableQueryParams: {
        pageNum: 1,
        pageSize: 10,
        tableName: undefined,
        tableComment: undefined,
        dataName: 'master',
      },
      tableVisible: false
    };
  },
  methods: {
       // 选中数据
    handleTableClick(row){
      this.$emit("handleTableClick",row)
      this.tableVisible = false;
    },
    // 表数据
    getDbList(){
      localStorage.setItem("dataName", this.tableQueryParams.dataName);
      listTable(this.tableQueryParams).then(res => {
        if (res.code === 200) {
          this.tableList = res.rows;
          this.tableTotal = res.total;
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
