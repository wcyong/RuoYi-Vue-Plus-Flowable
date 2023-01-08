<template>
    <el-dialog :title="title" :visible.sync="visible" @close="visible = false"  :width="width" append-to-body v-dialogDrag :close-on-click-modal="false">
      <div :style="[{height:`${height}px`}]" class="styleObj">
        <i-frame :src="url" />
      </div>
    </el-dialog>
  </template>
  <script>
  import iFrame from "@/components/iFrame/index";
  import {getToken} from "@/utils/auth";
  import { checkReportAuth } from "@/api/report/reportRegister";

  export default {
    props: {
        //报表编码
        reportCode: '',
        //参数
        params: '',
        //宽
        width: {
          type: String,
          default: '60%',
        },
        //高
        height: {
          type: Number,
          default: 500,
        },
        //标题
        title: {
          type: String,
          default: '查看报表',
        }
    },
    name: "JimuReport",
    components: { iFrame },
    data() {
      return {
        url: "",
        visible: false
      };
    },
    watch: {
        visible(val) {
            if(val){
                checkReportAuth(this.reportCode).then(response => {
                    if(this.paramId === null || this.paramId === ''){
                      this.url = process.env.VUE_APP_JMREPORT_URL + "/view/"+response.data+"/?token=" + getToken();
                    }else{
                      this.url = process.env.VUE_APP_JMREPORT_URL + "/view/"+response.data+"/?token=" + getToken()+this.params;
                    }
                })
            }
        },
    }
  };
  </script>
  <<style scoped>
   .styleObj{
    overflow: auto;
   }
  </style>
  