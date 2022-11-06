<template>
  <div class="app-container">
    <el-row :gutter="20">
        <el-col :span="4" :xs="24">
            <div class="head-container">
                <el-input
                    v-model="categoryName"
                    placeholder="请输入分类名称"
                    clearable
                    size="small"
                    prefix-icon="el-icon-search"
                    style="margin-bottom: 20px;margin-top: 3px;"
                />
            </div>
            <el-tree
                :data="deptOptions"
                :props="defaultProps"
                :expand-on-click-node="false"
                :filter-node-method="filterNode"
                ref="tree"
                @node-click="handleNodeClick"
                default-expand-all
            >
            <span class="custom-tree-node" slot-scope="{ data }">
                <span @click="handleNodeClick(data)" v-if="data.label.length < 12">{{ data.label }}</span>
                <el-tooltip v-else effect="dark" :content="data.label" placement="bottom-end">
                    <span @click="handleNodeClick(data)">{{`${data.label.substring(0, 12)}...`}}</span>
                </el-tooltip>
            </span>
            </el-tree>
        </el-col>
        <el-col :span="20" :xs="24">
            <!-- 检索 -->
            <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
                <el-form-item label="模型名称" prop="name">
                <el-input
                    v-model="queryParams.name"
                    placeholder="请输入模型名称"
                    clearable
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
                </el-form-item>
                <el-form-item label="标识Key" prop="key">
                <el-input
                    v-model="queryParams.key"
                    placeholder="请输入标识Key"
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
            <el-row :gutter="10" class="mb8">
            <el-button icon="el-icon-upload" size="small" type="primary" @click="clickDeploy">部署流程文件</el-button>
            <el-alert style="margin:10px 0" title="说明：当有多个相同标识Key的流程时，只显示其最新版本流程(PS:如若部署请部署从本项目模型管理导出的数据)。" type="warning" show-icon :closable="false" />
            <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
            </el-row>
            <!-- 表格数据 -->
            <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
                <el-table-column align="center" prop="name" label="流程定义名称" width="150" :show-overflow-tooltip="true"></el-table-column>
                <el-table-column align="center" prop="key" label="标识Key" width="100"></el-table-column>
                <el-table-column align="center" prop="version" label="版本号" width="90" >
                <template slot-scope="{row}"> v{{row.version}}.0</template>
                </el-table-column>
                <el-table-column align="center" prop="resourceName" label="流程XML" min-width="80" :show-overflow-tooltip="true">
                <template slot-scope="{row}">
                <el-link type="primary" @click.native="clickExportXML(row.id)">{{ row.resourceName }}</el-link>
                </template>
                </el-table-column>
                <el-table-column align="center" prop="diagramResourceName" label="流程图片" min-width="80" :show-overflow-tooltip="true">
                <template slot-scope="{row}">
                <el-link type="primary" @click="clickPreviewImg(row.id)">{{ row.diagramResourceName }}</el-link>
                </template>
                </el-table-column>
                <el-table-column  align="center" prop="suspensionState" label="状态"  min-width="70">
                <template slot-scope="scope">
                    <el-tag type="success" v-if="scope.row.suspensionState==1">激活</el-tag>
                    <el-tag type="danger" v-else>挂起</el-tag>
                </template>
                </el-table-column>
                <el-table-column  align="center" prop="deploymentTime" label="部署时间" :show-overflow-tooltip="true" width="100"></el-table-column>
                <el-table-column  align="center" prop="actProcessDefSettingVo.businessType" label="表单" width="80">
                <template slot-scope="scope">
                    <el-tag type="success" v-if="scope.row.actProcessDefSettingVo && scope.row.actProcessDefSettingVo.businessType===0">动态表单</el-tag>
                    <el-tag type="primary" v-else-if="scope.row.actProcessDefSettingVo && scope.row.actProcessDefSettingVo.businessType===1">业务表单</el-tag>
                </template>
                </el-table-column>
                <el-table-column  align="center" label="表单Key/组件名称" width="140">
                <template slot-scope="scope">
                    <span v-if="scope.row.actProcessDefSettingVo && scope.row.actProcessDefSettingVo.businessType===0">{{scope.row.actProcessDefSettingVo.formKey}}</span>
                    <span v-if="scope.row.actProcessDefSettingVo && scope.row.actProcessDefSettingVo.businessType===1">{{scope.row.actProcessDefSettingVo.componentName}}</span>
                </template>
                </el-table-column>
                
                <el-table-column  align="center" prop="description" :show-overflow-tooltip="true" label="挂起或激活原因" width="120"></el-table-column>
                <el-table-column label="操作" align="center" width="220" class-name="small-padding fixed-width">
                <template slot-scope="scope">
                <el-row :gutter="20" class="mb8">
                    <el-col :span="1.5">
                    <el-button
                        size="mini"
                        type="text"
                        icon="el-icon-sort"
                        @click="convertToModel
                        (scope.row)"
                    >转换模型</el-button>
                    </el-col>
                    <el-col :span="1.5">
                    <el-button
                        v-if="scope.row.suspensionState == 1"
                        @click="openDialog(scope.row)"
                        type="text"
                        size="mini"
                        icon="el-icon-lock"
                    >挂起流程</el-button>
                    <el-button
                        v-else type="text"
                        @click="openDialog(scope.row)"
                        size="mini"
                        icon="el-icon-unlock"
                    >激活流程</el-button>
                    </el-col>
                    <el-col :span="1.5">
                    <el-button
                        size="mini"
                        type="text"
                        icon="el-icon-delete"
                        @click="handleDelete(scope.row)"
                        >删除</el-button>
                    </el-col>
                </el-row>
                <el-row :gutter="20" class="mb8">
                    <el-col :span="1.5">
                    <el-button
                        size="mini"
                        type="text"
                        icon="el-icon-tickets"
                        @click="clickHistList
                        (scope.row)"
                    >历史版本</el-button>
                    </el-col>
                    <el-col :span="1.5">
                    <el-button
                        size="mini"
                        type="text"
                        icon="el-icon-user"
                        @click="handleUserSetting(scope.row)"
                        >人员设置</el-button>
                    </el-col>
                    <el-col :span="1.5">
                    <el-button
                        size="mini"
                        type="text"
                        icon="el-icon-setting"
                        @click="handleFormSetting(scope.row)"
                        >设置</el-button>
                    </el-col>
                </el-row>
                </template>
            </el-table-column>
            </el-table>
            <pagination v-show="total>0"
                :total="total"
                :page.sync="queryParams.pageNum"
                :limit.sync="queryParams.pageSize"
                @pagination="getList" />

            <!-- 历史数据开始 -->
            <el-dialog title="历史版本" :visible.sync="histVisible" width="90%" :close-on-click-modal="false" append-to-body>
            <el-table v-loading="loading" :data="histList" @selection-change="handleSelectionChange">
                <el-table-column align="center" type="index" label="序号" width="50"></el-table-column>
                <el-table-column align="center" prop="name" label="流程定义名称"  min-width="160"></el-table-column>
                <el-table-column align="center" prop="key" label="标识Key"  min-width="120"></el-table-column>
                <el-table-column align="center" prop="version" label="版本号" width="80" >
                <template slot-scope="{row}"> v{{row.version}}.0</template>
                </el-table-column>
                <el-table-column align="center" prop="resourceName" label="流程XML" min-width="150">
                <template slot-scope="{row}">
                <el-link type="primary" @click.native="clickExportXML(row.id)">{{ row.resourceName }}</el-link>
                </template>
                </el-table-column>
                <el-table-column align="center" prop="diagramResourceName" label="流程图片" min-width="150">
                <template slot-scope="{row}">
                <el-link type="primary" @click="clickPreviewImg(row.id)">{{ row.diagramResourceName }}</el-link>
                </template>
                </el-table-column>
                <el-table-column align="center" prop="suspensionState" label="状态"  min-width="50">
                <template slot-scope="scope">
                    <el-tag type="success" v-if="scope.row.suspensionState==1">激活</el-tag>
                    <el-tag type="danger" v-else>挂起</el-tag>
                </template>
                </el-table-column>
                <el-table-column align="center" prop="deploymentTime" label="部署时间" width="150"></el-table-column>
                <el-table-column  align="center" prop="description" :show-overflow-tooltip="true" label="挂起或激活原因" width="150"></el-table-column>
                <el-table-column label="操作" align="center" width="240" class-name="small-padding fixed-width">
                    <template slot-scope="scope">
                    <el-row :gutter="20" class="mb8">
                        <el-col :span="1.5">
                        <el-button
                            v-if="scope.row.suspensionState == 1"
                            @click="openDialog(scope.row)"
                            type="text"
                            size="mini"
                            icon="el-icon-lock"
                        >挂起流程</el-button>
                        <el-button
                            v-else type="text"
                            @click="openDialog(scope.row)"
                            size="mini"
                            icon="el-icon-unlock"
                        >激活流程</el-button>
                        </el-col>
                        <el-col :span="1.5">
                        <el-button
                            type="text"
                            @click="convertToModel(scope.row)"
                            size="mini"
                            icon="el-icon-sort"
                        >转换模型</el-button>
                        </el-col>
                        <el-col :span="1.5">
                        <el-button
                            size="mini"
                            type="text"
                            icon="el-icon-delete"
                            @click="handleDelete(scope.row)"
                        >删除</el-button>
                        </el-col>
                    </el-row>
                    <el-row :gutter="20" class="mb8">
                        <el-col :span="1.5">
                          <el-button
                              size="mini"
                              type="text"
                              icon="el-icon-user"
                              @click="handleUserSetting(scope.row)"
                          >人员设置</el-button>
                        </el-col>
                        <el-col :span="1.5">
                          <el-tooltip class="item" effect="dark" content="复制节点人员设置" placement="top-start">
                            <el-button
                                size="mini"
                                type="text"
                                icon="el-icon-copy-document"
                                @click="copySetting(scope.row)"
                            >复制节点</el-button>
                          </el-tooltip>
                        </el-col>
                        <el-col :span="1.5">
                          <el-button
                              size="mini"
                              type="text"
                              icon="el-icon-setting"
                              @click="handleFormSetting(scope.row)"
                              >设置</el-button>
                          </el-col>
                    </el-row>
                    </template>
                </el-table-column>
            </el-table>
            </el-dialog>
            <!-- 历史数据结束 -->

            <el-dialog
            title="挂起或激活流程"
            :close-on-click-modal="false"
            :visible.sync="dialogVisible"
            v-if="dialogVisible"
            width="60%">
            <el-input  type="textarea" v-model="description" maxlength="300" placeholder="请输入原因"
            :autosize="{ minRows: 4 }" show-word-limit ></el-input>
            <span slot="footer" class="dialog-footer">
                <el-button type="primary" size="small" v-loading = "buttonLoading" @click="clickUpdateProcDefState">确 定</el-button>
                <el-button size="small" @click="dialogVisible = false">取 消</el-button>
            </span>
            </el-dialog>
            <!-- 部署流程文件 -->
            <process-deploy ref="deployProcess" />

            <!-- 预览图片或xml -->
            <process-preview ref="previewRef" :url="url" :type="type"/>

            <!-- 流程设置 -->
            <process-setting ref="settingRef"/>

            <!-- 表单设置 -->
            <process-form-list ref="formRef" @callbackFn="getList" :formData="formData"/>
        </el-col>
    </el-row>
  </div>
</template>
<script>
import {list,del,updateProcDefState,getXml,getHistByPage} from "@/api/workflow/definition";
import { getProcessDefSettingByDefId } from "@/api/workflow/processDefSetting";
import {convertToModel} from "@/api/workflow/model";
import processDeploy from './components/processDeploy'
import processPreview from './components/processPreview'
import processSetting from './components/processSetting'
import processFormList from './components/processFormList'
import {copy} from "@/api/workflow/actNodeAssginee";
import {queryTreeList} from "@/api/workflow/category";

export default {
    name: 'Definition', // 和对应路由表中配置的name值一致
    components: { processDeploy, processPreview,processSetting,processFormList },
    data() {
        return {
            // 弹窗
            dialogVisible: false,
            histVisible: false,
            // 原因
            description: '',
            // 流程图
            url: [],
            // 流程定义id
            definitionId: null,
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
            // 是否显示弹出层
            open: false,
            // 表格数据
            dataList: [],
            // 历史表格数据
            histList: [],
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                name: undefined,
                key : undefined,
                category : undefined
            },
            // 设置弹窗
            settingVisible: false,
            // 流程定义对象
            procedefData: {},
            type: '',//png,xml
            // 表单数据
            formData: {},
            deptOptions: [],
            defaultProps: {
                children: "children",
                label: "label"
            },
            // 分类名称
            categoryName:'',
        }
    },
    watch: {
        // 根据名称筛选分类树
        categoryName(val) {
            this.$refs.tree.filter(val);
        }
    },
    created() {
      this.getList();
      this.getTreeCategoryList();
    },
    methods: {
      /** 搜索按钮操作 */
      handleQuery() {
        this.queryParams.pageNum = 1;
        this.getList();
      },
      /** 重置按钮操作 */
      resetQuery() {
        this.daterangeCreateTime = [];
        this.resetForm("queryForm");
        this.handleQuery();
      },
      // 多选框选中数据
      handleSelectionChange(selection) {
        this.ids = selection.map(item => item.id)
        this.single = selection.length!==1
        this.multiple = !selection.length
      },
      // 将流程定义转换为模型
      convertToModel(row){
         this.$modal.confirm('是否转换模型？').then(() => {
            this.loading = true;
            return convertToModel(row.id)
         }).then(() => {
           this.loading = false;
           this.getList();
           this.$modal.msgSuccess("转换成功");
         }).finally(() => {
           this.loading = false;
         });
      },
      // 分页
      getList(){
          this.loading = true;
          list(this.queryParams).then(response => {
            this.dataList = response.rows;
            this.total = response.total;
            this.loading = false;
          })
      },
      /** 删除按钮操作 */
      handleDelete(row) {
         const deploymentId = row.deploymentId;
         this.$modal.confirm('是否确认删除标识key为"' + row.key + '"的数据项？').then(() => {
           this.loading = true;
           return del(deploymentId,row.id);
         }).then(() => {
           this.loading = false;
           this.getList();
           this.$modal.msgSuccess("删除成功");
         }).finally(() => {
           this.loading = false;
         });
      },
      // 弹出部署流程文件窗口
      clickDeploy() {
        this.$refs.deployProcess.visible = true
      },
      // 导出xml流程文件
      clickExportXML(id) {
        this.type = 'xml'
        getXml(id).then(response => {
          this.url = []
          this.url = response.data
          this.$refs.previewRef.visible = true
        })
      },

      // 预览图片 downFile
      clickPreviewImg(id) {
        console.log(id)
        this.type = 'png'
        this.url = []
        this.url.push(process.env.VUE_APP_BASE_API+'/workflow/definition/export/png/'+id)
        this.$refs.previewRef.visible = true
      },
      //打开弹窗
      openDialog(row){
        this.procedefData = row
        if(row.description === null){
          this.description = ''
        }else{
          this.description = row.description
        }
        this.dialogVisible = true
      },
      //激活或挂起流程
      clickUpdateProcDefState(){
        let msg='';
        if(this.procedefData.suspensionState===1){
          msg=`暂停后，此流程下的所有任务都不允许往后流转，您确定挂起【${this.procedefData.name || this.procedefData.key}】吗？`
        }else{
          msg=`启动后，此流程下的所有任务都允许往后流转，您确定激活【${this.procedefData.name || this.procedefData.key}】吗？`
        }
        let params = {
          definitionId: this.procedefData.id,
          description: this.description
        }
        this.$modal.confirm(msg).then(() => {
           this.buttonLoading = true;
           return updateProcDefState(params);
         }).then(() => {
           this.buttonLoading = false;
           this.getList();
           this.$modal.msgSuccess("操作成功");
           this.dialogVisible = false
         }).finally(() => {
           this.buttonLoading = false;
         });
      },
      //历史版本
      clickHistList(row) {
          this.definitionId = row.id
          this.histVisible = true
          let data = {id:this.definitionId,key:row.key}
          this.getHistList(data)
      },
       //查询历史列表
       getHistList(data){
          this.loading = true;
          getHistByPage(data).then(response => {
            this.histList = response.data;
            this.loading = false;
          })
      },
      //打开设置
      handleUserSetting(row){
        this.$nextTick(() => {
          this.$refs.settingRef.visible = true
          this.$refs.settingRef.init(row.id)
        })
      },
      //打开表单
      handleFormSetting(row){
        this.loading = true
        getProcessDefSettingByDefId(row.id).then(response => {
          this.formData = {}
          if(response.data){
            this.formData = response.data
          }else{
            this.formData.businessType = 0
          }
          this.formData.processDefinitionId = row.id
          this.formData.processDefinitionKey = row.key
          this.formData.processDefinitionName = row.name         
          this.loading = false
          this.$refs.formRef.visible = true
        })
      },
      //复制节点设置
      copySetting(row){
        this.$modal.confirm('是否确认将此设置复制为最新节点设置？').then(() => {
           this.loading = true;
           copy(row.id,row.key).then(response => {
              if(response.code === 200){
                this.$modal.msgSuccess("操作成功");
              }else{
                this.$modal.msgError(response.msg);
              }
            })
         }).finally(() => {
           this.loading = false;
         });
      },
       // 流程分类
       getTreeCategoryList() {
        queryTreeList().then(response => {
            this.deptOptions = response.data;
        });
      },
      // 节点单击事件
      handleNodeClick(data) {
        if(data.id === -1){
            this.queryParams.category = undefined
        }else{
            this.queryParams.category = data.label;
        }
        this.getList()
      },
      // 筛选节点
      filterNode(value, data) {
        if (!value) return true;
        return data.label.indexOf(value) !== -1;
      }
    }
}
</script>
<style scoped>
  .custom-tree-node {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 14px;
    padding-right: 8px;
  }
</style>

