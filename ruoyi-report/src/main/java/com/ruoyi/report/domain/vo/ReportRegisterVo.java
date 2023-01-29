package com.ruoyi.report.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;



/**
 * 报表注册视图对象 jimu_report_register
 *
 * @author gssong
 * @date 2023-01-07
 */
@Data
@ExcelIgnoreUnannotated
public class ReportRegisterVo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 报表编码
     */
    @ExcelProperty(value = "报表编码")
    private String reportCode;

    /**
     * 报表名称
     */
    @ExcelProperty(value = "报表名称")
    private String reportName;

    /**
     * 报表id
     */
    @ExcelProperty(value = "报表id")
    private String reportId;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
