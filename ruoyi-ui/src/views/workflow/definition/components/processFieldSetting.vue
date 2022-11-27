<template>
    <el-drawer append-to-body title="字段配置" :visible.sync="drawer" :before-close="handleClose" direction="rtl" size="60%">
      <div style="padding:0 10px 0 10px">
        <div style="padding-bottom:10px;float: right;">
          <el-button type="primary" size="small" @click="addfield">添加</el-button>
          <el-button type="primary" size="small" @click="handerOpenTable">选择表</el-button>
          <el-button type="danger" size="small" @click="clearField">清空</el-button>
          <el-button type="success" size="small" @click="handleClose">确定</el-button>
        </div>
        <el-table :data="fieldList" border>
          <el-table-column align="center" type="index" label="序号" width="50"></el-table-column>
          <el-table-column label="字段属性" align="center" prop="field" width="180">
              <template slot-scope="scope">
                  <el-input v-model="scope.row.field"/>
              </template>
          </el-table-column>
          <el-table-column label="是否编辑" align="center" prop="edit" width="135">
              <template slot-scope="scope">
                  <el-radio-group v-model="scope.row.edit">
                    <el-radio-button :label="false">是</el-radio-button>
                    <el-radio-button :label="true">否</el-radio-button>
                  </el-radio-group>
              </template>
          </el-table-column>
          <el-table-column label="是否隐藏" align="center" prop="hidden" width="135">
              <template slot-scope="scope">
                  <el-radio-group v-model="scope.row.hidden">
                    <el-radio-button :label="false">是</el-radio-button>
                    <el-radio-button :label="true">否</el-radio-button>
                  </el-radio-group>
              </template>
          </el-table-column>
          <el-table-column label="是否必填" align="center" prop="required" width="135">
              <template slot-scope="scope">
                  <el-radio-group v-model="scope.row.required" @change="changeRequired(scope.row.required,scope.$index)">
                    <el-radio-button :label="true">是</el-radio-button>
                    <el-radio-button :label="false">否</el-radio-button>
                  </el-radio-group>
              </template>
          </el-table-column>
          <el-table-column label="提示信息" align="center" prop="message" >
              <template slot-scope="scope">
                  <el-input v-model="scope.row.message" v-show="scope.row.required"/>
              </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="80px">
              <template slot-scope="scope">
                  <el-button @click="deleteField(scope.$index)" type="danger" size="small">删除</el-button>
              </template>
          </el-table-column>
        </el-table>
      </div>
    <!-- 业务表 -->
    <sysImportTable ref="sysImportTableRef" @handleTableClick="handleTableClick"/>
    </el-drawer>
</template>

<script>
import sysImportTable from "@/views/workflow/definition/components/sysImportTable";
import { getGenTable } from "@/api/tool/gen";
export default {
    components: { sysImportTable },
    data() {
      return {
        drawer: false,
        fieldList: []
      }
    },
    methods: {
        //初始化
        init(fieldList){
            this.drawer = true
            this.fieldList = fieldList
        },
        // 添加字段属性
        addfield(){
            if(this.fieldList === undefined || this.fieldList === null) {
              this.fieldList = []
            }
            let param = {
                field:'',
                edit:true,
                hidden: true,
                required:true
            }
            this.fieldList.push(param);
            this.$forceUpdate()
        },
        // 删除字段属性
        deleteField(index){
          this.fieldList.splice(index,1)
        },
        // 删除字段属性
        clearField(){
          this.$modal.confirm('是否清空？').then(() => {
            this.fieldList=[]
            this.$emit("fieldClick",this.fieldList)
            this.$forceUpdate()
          })
        },
        //切换必填
        changeRequired(required,index){
          if(!required){
            this.fieldList[index].message = ''
          }
        },
        // 选中数据
        handleTableClick(row){
          this.$modal.confirm('是否确认，确认后将清空当前数据？').then(() => { 
            getGenTable(row.tableId).then(response=>{
              this.fieldList = []
              if(response.data.rows && response.data.rows.length>0){
                response.data.rows.forEach(element => {
                  let param = {
                      field:element.javaField,
                      edit:true,
                      hidden: true,
                      required:true,
                      message: element.columnComment+"不能为空"
                  }
                  this.fieldList.push(param);
                });
              }
              this.$emit("fieldClick",this.fieldList)
            })
          })
        },
        /** 打开表弹窗 */
        handerOpenTable() {
          this.$refs.sysImportTableRef.handerOpenTable()
        },
        //关闭
        handleClose(){
          this.drawer = false
          this.$emit("fieldClick",this.fieldList)
        }
    }
}
</script>


