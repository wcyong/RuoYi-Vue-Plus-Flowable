package com.ruoyi.workflow.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.ruoyi.common.core.domain.BaseEntity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 流程节点人员对象 act_process_node_assignee
 *
 * @author gssong
 * @date 2022-12-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("act_process_node_assignee")
public class ActProcessNodeAssignee extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;
    /**
     * 流程定义id
     */
    @NotBlank(message = "流程定义id不能为空", groups = { AddGroup.class })
    private String processDefinitionId;
    /**
     * 流程实例id
     */
    @NotBlank(message = "流程实例id不能为空", groups = { AddGroup.class })
    private String processInstanceId;
    /**
     * 节点id
     */
    @NotBlank(message = "节点id不能为空", groups = { AddGroup.class })
    private String nodeId;
    /**
     * 办理人
     */
    @NotBlank(message = "办理人不能为空", groups = { AddGroup.class })
    private String assignee;
    /**
     * 办理人id
     */
    @NotBlank(message = "办理人不能为空", groups = { AddGroup.class })
    private String assigneeId;
    /**
     * 节点名称
     */
    @NotBlank(message = "节点名称不能为空", groups = { AddGroup.class })
    private String nodeName;
    /**
     * 是否会签
     */
    @NotNull(message = "是否会签不能为空", groups = { AddGroup.class })
    private Boolean multiple;
    /**
     * 会签保存人员KEY值
     */
    private String multipleColumn;

}
