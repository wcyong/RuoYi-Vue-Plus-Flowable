package com.ruoyi.workflow.service;

import com.ruoyi.workflow.domain.ActProcessNodeAssignee;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 流程节点人员Service接口
 *
 * @author gssong
 * @date 2022-12-18
 */
public interface IActProcessNodeAssigneeService {

    /**
     * 查询流程节点人员
     */
    List<ActProcessNodeAssignee> queryByProcessInstanceId(String processInstanceId);

    /**
     * 新增流程节点人员
     */
    Boolean insertBatch(List<ActProcessNodeAssignee> actProcessNodeAssigneeList);

    /**
     * 批量删除流程节点人员信息
     */
    Boolean deleteByProcessInstanceId(String processInstanceId);
}
