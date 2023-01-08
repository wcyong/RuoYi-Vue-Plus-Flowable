package com.ruoyi.report.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.ruoyi.common.core.domain.dto.RoleDTO;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.report.domain.ReportRegisterRole;
import com.ruoyi.report.domain.vo.ReportDbVo;
import com.ruoyi.report.mapper.ReportRegisterRoleMapper;
import com.ruoyi.report.service.IReportRegisterRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ruoyi.report.domain.bo.ReportRegisterBo;
import com.ruoyi.report.domain.vo.ReportRegisterVo;
import com.ruoyi.report.domain.ReportRegister;
import com.ruoyi.report.mapper.ReportRegisterMapper;
import com.ruoyi.report.service.IReportRegisterService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * 报表注册Service业务层处理
 *
 * @author gssong
 * @date 2023-01-07
 */
@RequiredArgsConstructor
@Service
public class ReportRegisterServiceImpl implements IReportRegisterService {

    private final ReportRegisterMapper baseMapper;

    private final ReportRegisterRoleMapper reportRegisterRoleMapper;

    private final IReportRegisterRoleService iReportRegisterRoleService;

    /**
     * 分页查询
     *
     * @param reportDbVo
     * @param pageQuery
     * @return
     */
    @Override
    public TableDataInfo<ReportDbVo> selectReportDbPage(ReportDbVo reportDbVo, PageQuery pageQuery) {
        List<ReportDbVo> list = baseMapper.selectReportDbPage(reportDbVo.getDbCode(), reportDbVo.getName(), pageQuery.getPageNum(), pageQuery.getPageSize());
        Integer total = baseMapper.selectReportDbCount(reportDbVo.getDbCode(), reportDbVo.getName(), pageQuery.getPageNum(), pageQuery.getPageSize());
        return new TableDataInfo<>(list, total);
    }

    /**
     * 查询报表注册
     */
    @Override
    public ReportRegisterVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询报表注册列表
     */
    @Override
    public TableDataInfo<ReportRegisterVo> queryPageList(ReportRegisterBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ReportRegister> lqw = buildQueryWrapper(bo);
        Page<ReportRegisterVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询报表注册列表
     */
    @Override
    public List<ReportRegisterVo> queryList(ReportRegisterBo bo) {
        LambdaQueryWrapper<ReportRegister> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<ReportRegister> buildQueryWrapper(ReportRegisterBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<ReportRegister> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getReportCode()), ReportRegister::getReportCode, bo.getReportCode());
        lqw.like(StringUtils.isNotBlank(bo.getReportName()), ReportRegister::getReportName, bo.getReportName());
        return lqw;
    }

    /**
     * 新增报表注册
     */
    @Override
    public Boolean insertByBo(ReportRegisterBo bo) {
        ReportRegister add = BeanUtil.toBean(bo, ReportRegister.class);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改报表注册
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(ReportRegisterBo bo) {
        ReportRegister update = BeanUtil.toBean(bo, ReportRegister.class);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 批量删除报表注册
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(Collection<Long> ids) {
        LambdaQueryWrapper<ReportRegisterRole> wrapper = Wrappers.lambdaQuery();
        wrapper.in(ReportRegisterRole::getReportRegisterId, ids);
        reportRegisterRoleMapper.delete(wrapper);
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 获取当前登录人员拥有的报表
     *
     * @return
     */
    @Override
    public List<ReportRegister> getReportListByCurrentRole() {
        List<RoleDTO> roles = LoginHelper.getLoginUser().getRoles();
        List<Long> roleIds = roles.stream().map(RoleDTO::getRoleId).collect(Collectors.toList());
        LambdaQueryWrapper<ReportRegisterRole> wrapper = Wrappers.lambdaQuery();
        wrapper.in(ReportRegisterRole::getRoleId, roleIds);
        List<ReportRegisterRole> registerRoles = reportRegisterRoleMapper.selectList(wrapper);
        if (CollUtil.isNotEmpty(registerRoles)) {
            List<Long> reportRegisterIds = registerRoles.stream().map(ReportRegisterRole::getReportRegisterId).collect(Collectors.toList());
            return baseMapper.selectBatchIds(reportRegisterIds);
        }
        return Collections.emptyList();
    }

    /**
     * 校验是否有权限
     *
     * @param reportCode
     * @return
     */
    @Override
    public String checkReportAuth(String reportCode) {
        List<RoleDTO> roles = LoginHelper.getLoginUser().getRoles();
        List<Long> roleIds = roles.stream().map(RoleDTO::getRoleId).collect(Collectors.toList());
        ReportRegister reportRegister = baseMapper.selectOne(new LambdaQueryWrapper<ReportRegister>().eq(ReportRegister::getReportCode, reportCode));
        if (reportRegister == null) {
            throw new ServiceException("报表不存在");
        }
        List<ReportRegisterRole> reportRegisterRoles = iReportRegisterRoleService.getByReportRegisterIdAndRoleIds(reportRegister.getId(), roleIds);
        if (CollUtil.isNotEmpty(reportRegisterRoles)) {
            return reportRegister.getReportId();
        }
        throw new ServiceException("没有权限，请联系管理员！");
    }
}
