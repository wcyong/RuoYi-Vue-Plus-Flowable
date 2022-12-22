package com.ruoyi.workflow.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 流程定义设置 act_process_def_setting
 *
 * @author gssong
 * @date 2022-08-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("act_process_def_setting")
public class ActProcessDefSetting extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;
    /**
     * 流程定义id
     */
    private String processDefinitionId;
    /**
     * 流程定义key
     */
    private String processDefinitionKey;
    /**
     * 流程定义名称
     */
    private String processDefinitionName;
    /**
     * 表名
     */
    private String tableName;
    /**
     * 组件名称
     */
    private String componentName;
    /**
     * 备注
     */
    private String remark;
    /**
     * true 为审批节点选人 false 为申请人选择全部人员
     */
    private Boolean defaultProcess;

}
