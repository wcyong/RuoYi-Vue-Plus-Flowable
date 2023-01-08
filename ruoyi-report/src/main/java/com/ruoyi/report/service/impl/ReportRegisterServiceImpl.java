package com.ruoyi.report.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.report.domain.ReportRegisterRole;
import com.ruoyi.report.domain.vo.ReportDbVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ruoyi.report.domain.bo.ReportRegisterBo;
import com.ruoyi.report.domain.vo.ReportRegisterVo;
import com.ruoyi.report.domain.ReportRegister;
import com.ruoyi.report.mapper.ReportRegisterMapper;
import com.ruoyi.report.service.IReportRegisterService;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collection;

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

    /**
     * 分页查询
     * @param reportDbVo
     * @param pageQuery
     * @return
     */
    @Override
    public TableDataInfo<ReportDbVo> selectReportDbPage(ReportDbVo reportDbVo, PageQuery pageQuery) {
        List<ReportDbVo> list = baseMapper.selectReportDbPage(reportDbVo.getDbCode(), reportDbVo.getName(), pageQuery.getPageNum(), pageQuery.getPageSize());
        Integer total = baseMapper.selectReportDbCount(reportDbVo.getDbCode(), reportDbVo.getName(), pageQuery.getPageNum(), pageQuery.getPageSize());
        return new TableDataInfo<>(list,total);
    }

    /**
     * 查询报表注册
     */
    @Override
    public ReportRegisterVo queryById(Long id){
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
        validEntityBeforeSave(add);
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
    public Boolean updateByBo(ReportRegisterBo bo) {
        ReportRegister update = BeanUtil.toBean(bo, ReportRegister.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(ReportRegister entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除报表注册
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
