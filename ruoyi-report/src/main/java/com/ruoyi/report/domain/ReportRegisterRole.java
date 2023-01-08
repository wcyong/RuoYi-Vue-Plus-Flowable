package com.ruoyi.report.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@TableName("jimu_report_register_role")
public class ReportRegisterRole  extends BaseEntity implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 角色id
     */
    private Long roleId;

    /**
     * 报表注册id
     */
    private Long reportRegisterId;

    /**
     * 角色ids
     */
    @TableField(exist = false)
    private List<Long> roleIds;
}
