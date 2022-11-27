package com.ruoyi.demo.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.demo.domain.BsLeave;
import com.ruoyi.demo.domain.bo.BsLeaveBo;
import com.ruoyi.demo.domain.vo.BsLeaveVo;
import com.ruoyi.demo.mapper.BsLeaveMapper;
import com.ruoyi.demo.service.IBsLeaveService;
import com.ruoyi.workflow.domain.ActNodeAssignee;
import com.ruoyi.workflow.domain.vo.FieldList;
import com.ruoyi.workflow.service.IActNodeAssigneeService;
import com.ruoyi.workflow.service.IProcessInstanceService;
import com.ruoyi.workflow.utils.WorkFlowUtils;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 请假业务Service业务层处理
 *
 * @author gssong
 * @date 2021-10-10
 */
@RequiredArgsConstructor
@Service
public class BsLeaveServiceImpl implements IBsLeaveService {

    private final BsLeaveMapper baseMapper;

    private final IActNodeAssigneeService iActNodeAssigneeService;

    private final TaskService taskService;

    private final IProcessInstanceService iProcessInstanceService;

    @Override
    public BsLeaveVo queryById(String id) {
        BsLeaveVo vo = baseMapper.selectVoById(id);
        WorkFlowUtils.setStatusFieldValue(vo, vo.getId());
        return vo;
    }

    @Override
    public BsLeaveVo queryById(String id, String taskId) {
        BsLeaveVo vo = baseMapper.selectVoById(id);
        WorkFlowUtils.setStatusFieldValue(vo, vo.getId());
        WorkFlowUtils.setActNodeAssignee(vo, taskId);
        return vo;
    }

    @Override
    public TableDataInfo<BsLeaveVo> queryPageList(BsLeaveBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<BsLeave> lqw = buildQueryWrapper(bo);
        Page<BsLeaveVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        List<BsLeaveVo> records = result.getRecords();
        if (CollectionUtil.isNotEmpty(records)) {
            List<String> collectIds = records.stream().map(BsLeaveVo::getId).collect(Collectors.toList());
            WorkFlowUtils.setStatusListFieldValue(records, collectIds, "id");
        }
        result.setRecords(records);
        return TableDataInfo.build(result);
    }

    @Override
    public List<BsLeaveVo> queryList(BsLeaveBo bo) {
        LambdaQueryWrapper<BsLeave> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<BsLeave> buildQueryWrapper(BsLeaveBo bo) {
        LambdaQueryWrapper<BsLeave> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getUsername()), BsLeave::getUsername, bo.getUsername());
        lqw.eq(bo.getDuration() != null, BsLeave::getDuration, bo.getDuration());
        lqw.eq(StringUtils.isNotBlank(bo.getPrincipal()), BsLeave::getPrincipal, bo.getPrincipal());
        lqw.like(StringUtils.isNotBlank(bo.getContactPhone()), BsLeave::getContactPhone, bo.getContactPhone());
        lqw.eq(bo.getLeaveType() != null, BsLeave::getLeaveType, bo.getLeaveType());
        lqw.eq(StringUtils.isNotBlank(bo.getTitle()), BsLeave::getTitle, bo.getTitle());
        return lqw;
    }

    @Override
    public BsLeave insertByBo(BsLeaveBo bo) {
        BsLeave add = BeanUtil.toBean(bo, BsLeave.class);
        baseMapper.insert(add);
        return add;
    }

    @Override
    public BsLeave updateByBo(BsLeaveBo bo) {
        BsLeave update = BeanUtil.toBean(bo, BsLeave.class);
        baseMapper.updateById(update);
        return update;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<String> ids) {
        for (String id : ids) {
            String processInstanceId = iProcessInstanceService.getProcessInstanceId(id);
            if (StringUtils.isNotBlank(processInstanceId)) {
                iProcessInstanceService.deleteRuntimeProcessAndHisInst(processInstanceId);
            }
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
