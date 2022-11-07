<template>
    <div class="app-container">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="分类名称" prop="categoryName">
          <el-input
            v-model="queryParams.categoryName"
            placeholder="请输入分类名称"
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
            v-hasPermi="['system:category:add']"
          >新增</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="info"
            plain
            icon="el-icon-sort"
            size="mini"
            @click="toggleExpandAll"
          >展开/折叠</el-button>
        </el-col>
        <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>
  
      <el-table
        v-if="refreshTable"
        v-loading="loading"
        row-key="id"
        :default-expand-all="isExpandAll"
        :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
        :data="categoryList"
      >
        <el-table-column label="分类名称" prop="categoryName" />
        <el-table-column label="排序" align="center" prop="orderNum" />
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-plus"
              @click="handleAdd(scope.row)"
              v-hasPermi="['system:category:add']"
            >新增</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['system:category:remove']"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
  
      <!-- 添加或修改流程分类对话框 -->
      <el-dialog :title="title" :visible.sync="open" v-if="open" width="500px" append-to-body>
        <el-form ref="form" :model="form" :rules="rules" label-width="80px">
          <el-form-item label="父级分类" prop="parentId">
            <treeselect v-model="form.parentId" :options="categoryOptions" :normalizer="normalizer" placeholder="请选择父级分类" />
          </el-form-item>
          <el-form-item label="分类名称" prop="categoryName">
            <el-input v-model="form.categoryName" placeholder="请输入分类名称" />
          </el-form-item>
          <el-form-item label="排序" prop="orderNum">
            <el-input-number v-model="form.orderNum" placeholder="请输入排序" controls-position="right" :min="1" style="width:100%"/>
          </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">
          <el-button :loading="buttonLoading" type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </el-dialog>
    </div>
  </template>
  
  <script>
import {delCategory,addCategory,categoryList} from "@/api/workflow/category";
  import Treeselect from "@riophae/vue-treeselect";
  import "@riophae/vue-treeselect/dist/vue-treeselect.css";
  
  export default {
    name: "Category",
    components: {
      Treeselect
    },
    data() {
      return {
        // 按钮loading
        buttonLoading: false,
        // 遮罩层
        loading: true,
        // 显示搜索条件
        showSearch: true,
        // 流程分类表格数据
        categoryList: [],
        // 流程分类树选项
        categoryOptions: [],
        // 弹出层标题
        title: "",
        // 是否显示弹出层
        open: false,
        // 是否展开，默认全部展开
        isExpandAll: true,
        // 重新渲染表格状态
        refreshTable: true,
        // 查询参数
        queryParams: {
          categoryName: undefined
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
      };
    },
    created() {
      this.getList();
    },
    methods: {
      /** 查询流程分类列表 */
      getList() {
        this.loading = true;
        categoryList(this.queryParams).then(response => {
          this.categoryList = this.handleTree(response.data, "id", "parentId");
          this.loading = false;
        });
      },
      /** 转换流程分类数据结构 */
      normalizer(node) {
        if (node.children && !node.children.length) {
          delete node.children;
        }
        return {
          id: node.id,
          label: node.categoryName,
          children: node.children
        };
      },
      /** 查询流程分类下拉树结构 */
      getTreeselect() {
        categoryList().then(response => {
          this.categoryOptions = [];
          const data = { id: 0, categoryName: '顶级节点', children: [] };
          data.children = this.handleTree(response.data, "id", "parentId");
          this.categoryOptions.push(data);
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
          id: null,
          categoryName: null,
          parentId: null,
          createBy: null,
          createTime: null,
          updateBy: null,
          updateTime: null,
          orderNum: null
        };
        this.resetForm("form");
      },
      /** 搜索按钮操作 */
      handleQuery() {
        this.getList();
      },
      /** 重置按钮操作 */
      resetQuery() {
        this.resetForm("queryForm");
        this.handleQuery();
      },
      /** 新增按钮操作 */
      handleAdd(row) {
        this.reset();
        this.getTreeselect();
        if (row != null && row.id) {
          this.form.parentId = row.id;
        } else {
          this.form.parentId = 0;
        }
        this.open = true;
        this.title = "添加流程分类";
      },
      /** 展开/折叠操作 */
      toggleExpandAll() {
        this.refreshTable = false;
        this.isExpandAll = !this.isExpandAll;
        this.$nextTick(() => {
          this.refreshTable = true;
        });
      },
      /** 提交按钮 */
      submitForm() {
        this.$refs["form"].validate(valid => {
          if (valid) {
            this.buttonLoading = true;
            if (this.form.id != null) {
              updateCategory(this.form).then(response => {
                this.$modal.msgSuccess("修改成功");
                this.open = false;
                this.getList();
              }).finally(() => {
                this.buttonLoading = false;
              });
            } else {
              addCategory(this.form).then(response => {
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
        this.$modal.confirm("是否确认删除【" + row.categoryName + "】?").then(() => {
          this.loading = true;
          return delCategory(row.id);
        }).then(() => {
          this.loading = false;
          this.getList();
          this.$modal.msgSuccess("删除成功");
        }).catch(() => {
        }).finally(() => {
          this.loading = false;
        });
      }
    }
  };
  </script>
  