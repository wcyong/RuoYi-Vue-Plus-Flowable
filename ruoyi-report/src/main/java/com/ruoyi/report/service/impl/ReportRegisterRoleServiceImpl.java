package com.ruoyi.report.service.impl;


import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.report.domain.ReportRegisterRole;
import com.ruoyi.report.mapper.ReportRegisterRoleMapper;
import com.ruoyi.report.service.IReportRegisterRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 报表注册角色关联Service业务层处理
 *
 * @author gssong
 * @date 2023-01-07
 */
@RequiredArgsConstructor
@Service
public class ReportRegisterRoleServiceImpl implements IReportRegisterRoleService {

    private final ReportRegisterRoleMapper baseMapper;

    /**
     * 新增
     *
     * @param entity
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insert(ReportRegisterRole entity) {
        return baseMapper.insert(entity) > 0;
    }

    /**
     * 批量新增
     *
     * @param list
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertBatch(List<ReportRegisterRole> list) {
        return baseMapper.insertBatch(list);
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteBatch(List<Long> ids) {
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 按照注册id与角色id删除
     *
     * @param reportRegisterId
     * @param roleId
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByReportRegisterIdAndRoleId(Long reportRegisterId, Long roleId) {
        LambdaQueryWrapper<ReportRegisterRole> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ReportRegisterRole::getReportRegisterId, reportRegisterId);
        wrapper.eq(ReportRegisterRole::getRoleId, roleId);
        baseMapper.delete(wrapper);
    }

    /**
     * 报表授权
     *
     * @param reportRegisterRole
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean reportAuth(ReportRegisterRole reportRegisterRole) {
        LambdaQueryWrapper<ReportRegisterRole> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ReportRegisterRole::getReportRegisterId, reportRegisterRole.getReportRegisterId());
        baseMapper.delete(wrapper);
        List<ReportRegisterRole> registerRoles = new ArrayList<>();
        List<Long> roleIds = reportRegisterRole.getRoleIds().stream().distinct().collect(Collectors.toList());
        if (CollUtil.isNotEmpty(reportRegisterRole.getRoleIds())) {
            for (Long roleId : roleIds) {
                ReportRegisterRole registerRole = new ReportRegisterRole();
                registerRole.setReportRegisterId(reportRegisterRole.getReportRegisterId());
                registerRole.setRoleId(roleId);
                registerRoles.add(registerRole);
            }
            return baseMapper.insertBatch(registerRoles);
        }
        return true;
    }

    /**
     * 按照注册id查询角色
     *
     * @param reportRegisterId
     * @return
     */
    @Override
    public List<ReportRegisterRole> getByReportRegisterId(Long reportRegisterId) {
        LambdaQueryWrapper<ReportRegisterRole> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ReportRegisterRole::getReportRegisterId, reportRegisterId);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 按照注册id与角色id查询
     *
     * @param reportRegisterId
     * @param roleIds
     * @return
     */
    @Override
    public List<ReportRegisterRole> getByReportRegisterIdAndRoleIds(Long reportRegisterId, List<Long> roleIds) {
        LambdaQueryWrapper<ReportRegisterRole> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ReportRegisterRole::getReportRegisterId, reportRegisterId);
        wrapper.in(ReportRegisterRole::getRoleId, roleIds);
        return baseMapper.selectList(wrapper);
    }
}
