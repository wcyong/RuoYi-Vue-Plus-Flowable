<template>
<el-dialog title="用户" :visible.sync="visible" @close="close"  width="70%" append-to-body v-dialogDrag :close-on-click-modal="false">
  <div class="app-container">
    <el-row :gutter="20" >
      <!--部门数据-->
      <el-col :span="4" :xs="24">
        <!--部门/角色数据-->
        <div class="item-font" v-if="this.dataObj.chooseWay === 'role'">角色名称</div>
        <div class="item-font" v-if="this.dataObj.chooseWay === 'dept'">部门名称</div>
        <template class="left-container" v-if="this.dataObj.chooseWay === 'role'">
          <div v-for="(item,index) in roleList" :key="index" class="item">
              <div @click="handdle(item,index)" class="selected" v-if="item.roleName.length < 13" >{{item.roleName}}({{item.roleKey}})</div>
              <el-tooltip v-else effect="dark" :content="item.roleName" placement="bottom-end">
                  <div @click="handdle(item,index)">{{`${item.roleName.substring(0, 13)}...`}}</div>
              </el-tooltip>
          </div>
        </template>
        <template class="left-container" v-else-if="this.dataObj.chooseWay === 'dept'">
          <div v-for="(item,index) in deptList" :key="index" class="item">
              <div @click="handdle(item,index)" :class="currentIndex === index ? 'selected' : ''" v-if="item.deptName.length < 13" >{{item.deptName}}</div>
              <el-tooltip v-else effect="dark" :content="item.deptName" placement="bottom-end">
                  <div @click="handdle(item,index)">{{`${item.deptName.substring(0, 13)}...`}}</div>
              </el-tooltip>
          </div>
        </template>
      </el-col>
      <!--用户数据-->
      <el-col :span="span" :xs="24">
        <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="归属部门" prop="deptId" v-if="this.dataObj.chooseWay === 'person'">
            <treeselect v-model="queryParams.deptId" style="width: 240px" :options="deptOptions" :show-count="true" placeholder="请选择归属部门" />
          </el-form-item>
          <el-form-item label="用户名称" prop="userName">
            <el-input
              v-model="queryParams.userName"
              placeholder="请输入用户名称"
              clearable
              style="width: 240px"
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item label="用户昵称" prop="nickName">
            <el-input
              v-model="queryParams.nickName"
              placeholder="请输入用户昵称"
              clearable
              style="width: 240px"
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search"  @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh"  @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" height="250px" border :data="userList" ref="multipleTable" :row-key="(row) => {return row.userId}" @selection-change="handleSelectionChange">
          <el-table-column type="selection" :reserve-selection="true" width="50" align="center" />
          <el-table-column label="用户编号" align="center" key="userId" prop="userId" v-if="columns[0].visible" :show-overflow-tooltip="true"/>
          <el-table-column label="用户名称" align="center" key="userName" prop="userName" v-if="columns[1].visible" :show-overflow-tooltip="true" />
          <el-table-column label="用户昵称" align="center" key="nickName" prop="nickName" v-if="columns[2].visible" :show-overflow-tooltip="true" />
          <el-table-column label="部门" align="center" key="deptName" prop="dept.deptName" v-if="columns[3].visible" :show-overflow-tooltip="true" />
          <el-table-column label="手机号码" align="center" key="phonenumber" prop="phonenumber" v-if="columns[4].visible" width="120" />
          <el-table-column label="创建时间" align="center" prop="createTime" v-if="columns[5].visible" width="160">
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
        <!-- 选中的用户 -->
        <div>
          <el-tag v-for="(user,index) in chooseUserList" :key="user.userId" closable @close="handleCloseTag(user,index)" style="margin:12px 2px 0px 2px">{{user.userName}} </el-tag>
        </div>
      </el-col>
    </el-row>
  </div>
  <div slot="footer" class="dialog-footer">
        <el-button :loading="buttonLoading" type="primary" @click="confirmUser">确认</el-button>
        <el-button @click="close">取 消</el-button>
  </div>
</el-dialog>
</template>

<script>
import { getWorkflowUserListByPage } from "@/api/workflow/workflowUser";
import { deptTreeSelect } from "@/api/system/user";
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";

export default {
  props: {
    //是否多选
    multiple: {
      type: Boolean,
      default: true,
    },
    //传入的条件
    dataObj: {
      type: Object,
      default: {}
    },
    // 节点id
    nodeId: {
      type: String,
      default: ''
    }
  },
  name: "User",
  dicts: ['sys_user_sex'],
  components: { Treeselect },
  data() {
    return {
      // 遮罩层
      loading: true,
      buttonLoading: false,
      // 是否显示弹出层
      visible: false,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 用户表格数据
      userList: [],
      // 角色表格数据
      roleList: [],
      // 部门表格数据
      deptList: [],
      // 部门树选项
      deptOptions: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: undefined,
        nickName: undefined,
        deptId: undefined,
        type: undefined,
        params: undefined,
        paramIds: undefined,
        ids:[]
      },
      // 列信息
      columns: [
        { key: 0, label: `用户编号`, visible: true },
        { key: 1, label: `用户名称`, visible: true },
        { key: 2, label: `用户昵称`, visible: true },
        { key: 3, label: `部门`, visible: true },
        { key: 4, label: `手机号码`, visible: true },
        { key: 5, label: `创建时间`, visible: true }
      ],
      // 保存选择的用户
      chooseUserList: [],
      currentIndex:-1,
      span: 24
    };
  },
  watch: {
    visible(val) {
        if(val){
            this.$nextTick(()=>{
              this.$refs.multipleTable.clearSelection();
            })
            this.currentIndex = -1
            this.queryParams.params = this.dataObj.assigneeId
            this.queryParams.paramIds = this.dataObj.assigneeId
            this.queryParams.type = this.dataObj.chooseWay
            this.queryParams.ids = this.dataObj.ids
            this.getTreeselect();
            this.getList()
            if(this.dataObj.chooseWay === 'role' || this.dataObj.chooseWay === 'dept'){
              this.span = 20
            }else{
              this.span = 24
            }
        }
    },
  },
  methods: {
    /** 查询用户列表 */
    getList() {
      this.loading = true;
      getWorkflowUserListByPage(this.queryParams).then(response => {
        if(response.data){
            let res = response.data.page
            if(res){
                this.userList = res.rows;
                this.total = res.total;
            }
            //反选
            if(response.data.list){
              this.chooseUserList = response.data.list
              response.data.list.forEach(row => {
                this.$refs.multipleTable.toggleRowSelection(row,true);
              })
            }
            if(this.dataObj.chooseWay === 'role'){
                this.roleList = response.data.roleList
            }
            if(this.dataObj.chooseWay === 'dept'){
                this.deptList = response.data.deptList
            }
            this.loading = false;
         }
      });
    },
    /** 查询部门下拉树结构 */
    getTreeselect() {
      deptTreeSelect().then(response => {
        this.deptOptions = response.data;
      });
    },
    //按条件查询
    handdle(row,index){
        if(this.dataObj.chooseWay === 'role'){
            this.queryParams.params = row.roleId
        }
        if(this.dataObj.chooseWay === 'dept'){
            this.queryParams.params = row.deptId
        }
        this.currentIndex = index
        this.getList()
    },
    // 关闭
    close() {
      this.chooseUserList = []
      this.queryParams.ids = []
      this.visible = false 
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.queryParams.params = this.dataObj.assigneeId
      this.currentIndex = -1
      this.getList()
    },
    // 多选框选中数据
    handleSelectionChange(val) {
        if(this.multiple){
          this.chooseUserList = val.filter((element,index,self)=>{
             return self.findIndex(x=>x.userId===element.userId) === index
          })
          val.forEach(u=>{
            if(!this.chooseUserList.includes(u)){
              this.$refs.multipleTable.toggleRowSelection(u, false);
            }
          })
          this.queryParams.ids = this.chooseUserList.map((item) => {return item.userId});
        }else{
          this.chooseUserList = val
          if (val.length > 1) {
            let delRow = val.shift();
            this.$refs.multipleTable.toggleRowSelection(delRow, false);
            this.queryParams.ids = this.chooseUserList.map((item) => {return item.userId});
          }
          if(val.length === 0){
            this.chooseUserList = null
          }
        }
    },
    // 确认
    confirmUser(){
      if(this.chooseUserList.length>0){
        this.$emit("confirmUser",this.chooseUserList,this.nodeId)
        this.chooseUserList = []
        this.queryParams.ids = []
        this.visible = false
      }else{
        this.$modal.msgWarning("请选择人员！");
      }
    },
    // 删除tag
    handleCloseTag(user,index){
      if(this.$refs.multipleTable.selection && this.$refs.multipleTable.selection.length > 0){
        this.$refs.multipleTable.selection.forEach((e,i)=>{
          if(user.userId === e.userId){
            this.$refs.multipleTable.selection.splice(i, 1);
          }
        })
      }
      this.chooseUserList.splice(index, 1);
      this.$refs.multipleTable.toggleRowSelection(user, false)

      if(this.queryParams.ids && this.queryParams.ids.length > 0){
        this.queryParams.ids.forEach((userId,i)=>{
          if(userId === user.userId){
            this.queryParams.ids.splice(i, 1);
          }
        })
      }
    },
  }
};
</script>
<style scoped>
  .app-container{
    height: 550px;
    overflow: auto;
  }
  .item{
    border-bottom: 1px solid #eff3fa;
    white-space: nowrap;
    line-height: 42px;
    cursor: pointer;
    font-size: 13px;
  }
  .left-container{
    height: 500px;
    overflow-y: scroll;
  }
  .item-font{
    white-space: nowrap;
    line-height: 42px;
    font-weight: 600;
  }
  .selected{
    font-weight: bold;
    background-color: #F5F7FA;
  }

</style>
