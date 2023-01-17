<template>
    <el-dialog title="授权" :visible.sync="visible" v-if="visible" width="850px" append-to-body>
        <div class="app-container" style="height:520px;overflow-y: auto;">
            <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch">
                <el-form-item label="角色名称" prop="roleName">
                <el-input
                    v-model="queryParams.roleName"
                    placeholder="请输入角色名称"
                    clearable
                    style="width: 240px"
                    @keyup.enter.native="handleQuery"
                />
                </el-form-item>
                <el-form-item label="权限字符" prop="roleKey">
                <el-input
                    v-model="queryParams.roleKey"
                    placeholder="请输入权限字符"
                    clearable
                    style="width: 240px"
                    @keyup.enter.native="handleQuery"
                />
                </el-form-item>
                <el-form-item label="创建时间">
                <el-date-picker
                    v-model="dateRange"
                    style="width: 240px"
                    value-format="yyyy-MM-dd"
                    type="daterange"
                    range-separator="-"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                ></el-date-picker>
                </el-form-item>
                <el-form-item>
                <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
                <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
                </el-form-item>
            </el-form>
        
            <el-row :gutter="10" class="mb8">
                <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
            </el-row>
        
            <el-table v-loading="loading" @select="selectCheckBox" border max-height="250px" ref="roleRef" :row-key="(row) => {return row.roleId}" :data="roleList" @selection-change="handleSelectionChange">
                <el-table-column type="selection" :reserve-selection="true" width="55" align="center" />
                <el-table-column label="角色编号" prop="roleId" width="120" />
                <el-table-column label="角色名称" prop="roleName" :show-overflow-tooltip="true" width="150" />
                <el-table-column label="权限字符" prop="roleKey" :show-overflow-tooltip="true" width="150" />
                <el-table-column label="显示顺序" prop="roleSort" width="100" />
                <el-table-column label="创建时间" align="center" prop="createTime" width="180">
                <template slot-scope="scope">
                    <span>{{ parseTime(scope.row.createTime) }}</span>
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
            <!-- 选中的角色 -->
            <div>
             <el-tag v-for="(role,index) in chooseRoleList" :key="role.roleId" closable @close="handleCloseTag(role,index)" style="margin:12px 2px 0px 2px">{{role.roleName}} </el-tag>
            </div>
        </div>
        <span slot="footer" class="dialog-footer">
            <el-button @click="visible = false">取 消</el-button>
            <el-button type="primary" @click="submit()">确 定</el-button>
        </span>
    </el-dialog>
  </template>
  
  <script>
  import { listRole } from "@/api/system/role";
  import { getRoleListByReportRegisterIdAndRoleIds,reportAuth,deleteByReportRegisterIdAndRoleId } from "@/api/report/reportRegister";

  export default {
    props: {
        //报表注册id
        reportRegisterId: '',
    },
    data() {
      return {
        // 遮罩层
        loading: true,
        // 选中数组
        ids: [],
        // 显示搜索条件
        showSearch: true,
        // 总条数
        total: 0,
        // 角色表格数据
        roleList: [],
        // 日期范围
        dateRange: [],
        // 查询参数
        queryParams: {
          pageNum: 1,
          pageSize: 10,
          roleName: undefined,
          roleKey: undefined,
          status: undefined
        },
        visible: false,
        chooseRoleList: []
      };
    },
    watch: {
        visible(val) {
            if(val){
                this.chooseRoleList = []
                this.roleList = []
                this.queryParams.pageNum = 1;
                this.$nextTick(()=>{
                    this.$refs.roleRef.clearSelection();
                })
                this.getList();
            }
        }
    },
    methods: {
      /** 查询角色列表 */
      getList() {
        this.loading = true;
        listRole(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
            this.roleList = response.rows;
            this.total = response.total;
            this.loading = false;
            let param = {
                reportRegisterId: this.reportRegisterId,
                roleIds: this.ids
            }
            getRoleListByReportRegisterIdAndRoleIds(param).then(response => {
                if(response.data.length > 0){
                    response.data.forEach(row => {
                        this.roleList.forEach(e => {
                            if(row.roleId === e.roleId){
                                this.$refs.roleRef.toggleRowSelection(e,true);
                            }
                        })
                    })
                }
                this.chooseRoleList = response.data
            })
          }
        );
      },
      /** 搜索按钮操作 */
      handleQuery() {
        this.queryParams.pageNum = 1;
        this.getList();
      },
      /** 重置按钮操作 */
      resetQuery() {
        this.dateRange = [];
        this.resetForm("queryForm");
        this.handleQuery();
      },
      // 多选框选中数据
      handleSelectionChange(selection) {
        this.ids = selection.map(item => item.roleId)
        this.chooseRoleList = selection.filter((element,index,self)=>{
            return self.findIndex(x=>x.roleId===element.roleId) === index
        })
        selection.forEach(u=>{
            if(!this.chooseRoleList.includes(u)){
                this.$refs.roleRef.toggleRowSelection(u, false);
            }
        })
      },
      // 删除tag
      handleCloseTag(role,index){
        this.chooseRoleList.splice(index, 1);
        this.roleList.forEach(e => {
            if(role.roleId === e.roleId){
                this.$refs.roleRef.toggleRowSelection(e,false);
            }
        })
        if(this.ids && this.ids.length > 0){
            this.ids.forEach((roleId,i)=>{
                if(roleId === role.roleId){
                    this.ids.splice(i, 1);
                }
            })
        }
        deleteByReportRegisterIdAndRoleId(this.reportRegisterId,role.roleId)
      },
      // 确认
      submit(){
        let roleIds = this.chooseRoleList.map(item => item.roleId)
        let param = {
            reportRegisterId: this.reportRegisterId,
            roleIds: roleIds
        }
        reportAuth(param).then(response => {
            this.$modal.msgSuccess("授权成功");
            this.visible = false
        })
      },
      selectCheckBox(selection, row){
        let roleIds = this.chooseRoleList.map(item => item.roleId)
        if(roleIds.includes(row.roleId)){
            deleteByReportRegisterIdAndRoleId(this.reportRegisterId,row.roleId)
        }
      }
    }
  };
  </script>
  
  