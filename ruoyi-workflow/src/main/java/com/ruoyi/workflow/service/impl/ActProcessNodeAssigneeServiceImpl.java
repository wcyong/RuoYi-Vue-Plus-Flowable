package com.ruoyi.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ruoyi.workflow.domain.ActProcessNodeAssignee;
import com.ruoyi.workflow.mapper.ActProcessNodeAssigneeMapper;
import com.ruoyi.workflow.service.IActProcessNodeAssigneeService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 流程节点人员Service业务层处理
 *
 * @author gssong
 * @date 2022-12-18
 */
@RequiredArgsConstructor
@Service
public class ActProcessNodeAssigneeServiceImpl implements IActProcessNodeAssigneeService {

    private final ActProcessNodeAssigneeMapper baseMapper;

    /**
     * 查询办理人
     * @param processInstanceId
     * @return
     */
    @Override
    public List<ActProcessNodeAssignee> queryByProcessInstanceId(String processInstanceId) {
        LambdaQueryWrapper<ActProcessNodeAssignee> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ActProcessNodeAssignee::getProcessInstanceId,processInstanceId);
        return baseMapper.selectVoList(wrapper);
    }

    /**
     * 新增流程节点人员
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertBatch(List<ActProcessNodeAssignee> actProcessNodeAssigneeList) {
        return baseMapper.insertBatch(actProcessNodeAssigneeList);
    }

    /**
     * 批量删除流程节点人员
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByProcessInstanceId(String processInstanceId) {
        LambdaQueryWrapper<ActProcessNodeAssignee> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ActProcessNodeAssignee::getProcessInstanceId,processInstanceId);
        return baseMapper.delete(wrapper)>0;
    }
}
