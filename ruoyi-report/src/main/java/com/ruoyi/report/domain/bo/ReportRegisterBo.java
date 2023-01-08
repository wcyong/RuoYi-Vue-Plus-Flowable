package com.ruoyi.report.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.*;


import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 报表注册业务对象 jimu_report_register
 *
 * @author gssong
 * @date 2023-01-07
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class ReportRegisterBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 报表编码
     */
    @NotBlank(message = "报表编码不能为空", groups = { AddGroup.class, EditGroup.class })
    private String reportCode;

    /**
     * 报表名称
     */
    @NotBlank(message = "报表名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String reportName;

    /**
     * 报表id
     */
    @NotBlank(message = "报表id不能为空", groups = { AddGroup.class, EditGroup.class })
    private String reportId;

    /**
     * 备注
     */
    private String remark;


}
