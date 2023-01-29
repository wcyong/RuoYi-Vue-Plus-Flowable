package com.ruoyi.report.service;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.report.domain.ReportRegister;
import com.ruoyi.report.domain.vo.ReportDbVo;
import com.ruoyi.report.domain.vo.ReportRegisterVo;
import com.ruoyi.report.domain.bo.ReportRegisterBo;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 报表注册Service接口
 *
 * @author gssong
 * @date 2023-01-07
 */
public interface IReportRegisterService {

    /**
     * 分页查询
     *
     * @param reportDbVo
     * @param pageQuery
     * @return
     */
    TableDataInfo<ReportDbVo> selectReportDbPage(ReportDbVo reportDbVo, PageQuery pageQuery);

    /**
     * 查询报表注册
     */
    ReportRegisterVo queryById(Long id);

    /**
     * 查询报表注册列表
     */
    TableDataInfo<ReportRegisterVo> queryPageList(ReportRegisterBo bo, PageQuery pageQuery);

    /**
     * 查询报表注册列表
     */
    List<ReportRegisterVo> queryList(ReportRegisterBo bo);

    /**
     * 新增报表注册
     */
    Boolean insertByBo(ReportRegisterBo bo);

    /**
     * 修改报表注册
     */
    Boolean updateByBo(ReportRegisterBo bo);

    /**
     * 校验并批量删除报表注册信息
     */
    Boolean deleteByIds(Collection<Long> ids);

    /**
     * 获取当前登录人员拥有的报表
     *
     * @return
     */
    List<ReportRegister> getReportListByCurrentRole();

    /**
     * 校验是否有权限
     *
     * @param reportCode
     * @return
     */
    R<String> checkReportAuth(String reportCode);
}
