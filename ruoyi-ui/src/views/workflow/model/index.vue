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
                default-expand-all
            >
            <span class="custom-tree-node" slot-scope="{ node, data }">
                <span @click="handleNodeClick(data)">{{ data.label }}</span>
                <span  v-if="data.id!==-1">
                    <el-button
                        type="text"
                        size="mini"
                        @click="() => append(node, data)">
                        新增
                    </el-button>
                    <el-button
                        type="text"
                        size="mini"
                        @click="() => remove(node, data)">
                        删除
                    </el-button>
                </span>
            </span>
            </el-tree>
         </el-col>
         <el-col :span="20" :xs="24">
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
                <el-col :span="1.5">
                <el-button
                    type="primary"
                    plain
                    icon="el-icon-plus"
                    size="mini"
                    @click="handleAdd"
                    v-hasPermi="['demo:demo:add']"
                >新增</el-button>
                </el-col>
                <el-col :span="1.5">
                <el-button
                    type="danger"
                    plain
                    icon="el-icon-delete"
                    size="mini"
                    :disabled="multiple"
                    @click="handleDelete"
                    v-hasPermi="['demo:demo:remove']"
                >删除</el-button>
                </el-col>
                <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
            </el-row>

            <el-table v-loading="loading" :data="modelList" @selection-change="handleSelectionChange">
                <el-table-column type="selection" width="55" align="center" />
                <el-table-column fixed align="center" type="index" label="序号" width="50"></el-table-column>
                <el-table-column fixed align="center" prop="name" label="模型名称"></el-table-column>
                <el-table-column  align="center" prop="key" label="标识Key" ></el-table-column>
                <el-table-column align="center" prop="version" label="版本号" width="90" >
                <template slot-scope="{row}"> v{{row.version}}.0</template>
                </el-table-column>
                <el-table-column  align="center" prop="metaInfo" label="备注说明"  min-width="130"></el-table-column>
                <el-table-column  align="center" prop="createTime" label="创建时间" width="160"></el-table-column>
                <el-table-column  align="center" prop="lastUpdateTime" label="更新时间" width="160"></el-table-column>
                <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
                <template slot-scope="scope">
                <el-row :gutter="10" class="mb8">
                    <el-col :span="1.5">
                    <el-button type="text" size="small" icon="el-icon-thumb" @click="clickDesign(scope.row.id)">设计流程</el-button>
                    </el-col>
                    <el-col :span="1.5">
                    <el-button type="text" size="small" icon="el-icon-download" @click="clickExportZip(scope.row)">导出</el-button>
                    </el-col>
                </el-row>
                <el-row :gutter="10" class="mb8">
                    <el-col :span="1.5">
                    <el-button type="text" size="small" icon="el-icon-c-scale-to-original" @click="clickDeploy(scope.row.id,scope.row.key)">流程部署</el-button>
                    </el-col>
                    <el-col :span="1.5">
                    <el-button size="mini" type="text"  icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
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
            <!-- 设计流程开始 -->
            <el-dialog title="设计模型" :before-close="handleClose" :visible.sync="bpmnJsModelVisible"
            v-if="bpmnJsModelVisible" fullscreen :modal-append-to-body='false'>
                <bpmnJs ref="bpmnJsModel" @close-bpmn="closeBpmn" :categorysBpmn="categorysBpmn" :modelId="modelId"/>
            </el-dialog>
            <!-- 设计流程结束 -->

            <!-- 流程分类开始 -->
            <el-dialog title="新增流程分类" :visible.sync="categoryVisible" width="40%" v-if="categoryVisible" :modal-append-to-body='false'>
                <el-form :model="categoryForm" ref="categoryForm" :rules="rules" v-show="showSearch" label-width="80px">
                    <el-form-item label="父级分类" prop="parentId">
                        <treeselect v-model="categoryForm.parentId" :options="deptOptions" :show-count="true" placeholder="请选择流程分类" />
                    </el-form-item>
                    <el-form-item label="分类名称" prop="categoryName">
                        <el-input v-model="categoryForm.categoryName" placeholder="请输入分类名称" />
                    </el-form-item>
                    <el-form-item label="排序" prop="orderNum">
                        <el-input-number style="width:100%" v-model="categoryForm.orderNum" controls-position="right" :min="1"/>
                    </el-form-item>
                </el-form>
                <span slot="footer" class="dialog-footer">
                    <el-button @click="categoryVisible = false">取 消</el-button>
                    <el-button type="primary" @click="saveCategory">确 定</el-button>
                </span>
            </el-dialog>
            <!-- 流程分类结束 -->
         </el-col>
        </el-row>
    </div>
</template>

<script>
import {list,add,del,deploy} from "@/api/workflow/model";
import {queryTreeList,delCategory,addCategory} from "@/api/workflow/category";
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";
import BpmnJs from './bpmnJs'
export default {
    dicts: ['act_category'],
    name: 'Model', // 和对应路由表中配置的name值一致
    components: {BpmnJs,Treeselect},
    data() {
        return {
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
            // 是否显示弹出层
            bpmnJsModelVisible: false,
            categoryVisible: false,
            // 模型定义表格数据
            modelList: [],
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                name: undefined,
                key : undefined,
                category : undefined
            },
            // 表单参数
            form: {},
            // 表单校验
            rules: {
              categoryName: [
                { required: true, message: "分类名称不能为空", trigger: "blur" }
              ],
              parentId: [
                { required: true, message: "父级分类不能为空", trigger: "blur" }
              ],
              orderNum: [
                { required: true, message: "排序不能为空", trigger: "blur" }
              ]
            },
            modelId: null, // 模型id
            categorys:[],
            deptOptions: [],
            defaultProps: {
                children: "children",
                label: "label"
            },
            // 分类名称
            categoryName:'',
            // 流程分类
            categoryForm:{
                parentId:'',
                categoryName:'',
                orderNum: 0
            }
        }
    },
    watch: {
        // 根据名称筛选分类树
        categoryName(val) {
            this.$refs.tree.filter(val);
        }
    },
    created() {
      this.categorysBpmn = this.dict.type.act_category
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
      //分页
      getList(){
        this.loading = true;
        list(this.queryParams).then(response => {
          this.modelList = response.rows;
          this.total = response.total;
          this.loading = false;
        })
      },
      /** 删除按钮操作 */
      handleDelete(row) {
         const ids = row.id || this.ids;
         this.$modal.confirm('是否确认删除模型id为"' + ids + '"的数据项？').then(() => {
           this.loading = true;
           return del(ids);
         }).then(() => {
           this.loading = false;
           this.getList();
           this.$modal.msgSuccess("删除成功");
         }).finally(() => {
           this.loading = false;
         });
      },
      // 流程部署
      clickDeploy(id,key){
        this.$modal.confirm('是否部署模型key为【'+key+'】流程？').then(() => {
           this.loading = true;
           return deploy(id);
         }).then(() => {
           this.loading = false;
           this.getList();
           this.$modal.msgSuccess("部署成功");
         }).finally(() => {
           this.loading = false;
         });
      },
      handleAdd(){
        this.modelId = "new"
        this.bpmnJsModelVisible = true
      },
      /** 提交按钮 */
      submitForm() {
        this.$refs["form"].validate(valid => {
          if (valid) {
              this.buttonLoading = true;
              add(this.form).then(response => {
                this.$modal.msgSuccess("新增成功");
                this.open = false;
                this.getList();
              }).finally(() => {
                this.buttonLoading = false;
              });
            }
        });
      },
      // 打开设计流程
      clickDesign(id) {
        this.modelId = id
        this.bpmnJsModelVisible = true
      },
      // 关闭设计流程
      closeBpmn(){
        this.getList()
        this.bpmnJsModelVisible = false
      },
      handleClose() {
        this.$confirm('请记得点击保存按钮，确定关闭设计窗口?', '确认关闭',{
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.bpmnJsModelVisible = false
          // 刷新数据
          this.getList()
        }).catch(() => {})
      },
      // 导出流程模型
      clickExportZip(data){
          this.$download.zip('/workflow/model/export/zip/'+data.id,data.name+"-"+data.key+"-"+data.category);
      },
      // 筛选节点
      filterNode(value, data) {
        if (!value) return true;
        return data.label.indexOf(value) !== -1;
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
            this.queryParams.category = data.id;
        }
        this.getList()
      },
      // 流程分类删除
      remove(node,data){
        this.$confirm('是否确认删除【'+data.label+'】?',{
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
            delCategory(data.id).then(response=>{
                this.$modal.msgSuccess("删除成功");
                this.getList()
                this.getTreeCategoryList()
            })
        }).catch(() => {})
      },
      // 流程分类添加
      append(node,data){
        this.categoryForm.parentId = data.id
        this.categoryVisible = true
      },
      // 流程分类保存
      saveCategory(){
        addCategory(this.categoryForm).then(response=>{
            this.$modal.msgSuccess("新增成功");
            this.getList()
            this.categoryForm = {
                orderNum:1
            }
            this.getTreeCategoryList()
            this.categoryVisible = false
        })
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
