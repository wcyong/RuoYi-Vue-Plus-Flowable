package com.ruoyi.report.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 报表注册对象 jimu_report_register
 *
 * @author gssong
 * @date 2023-01-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("jimu_report_register")
public class ReportRegister extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;
    /**
     * 报表编码
     */
    private String reportCode;
    /**
     * 报表名称
     */
    private String reportName;
    /**
     * 报表id
     */
    private String reportId;
    /**
     * 备注
     */
    private String remark;

}
