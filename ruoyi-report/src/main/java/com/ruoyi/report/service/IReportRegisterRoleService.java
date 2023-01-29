package com.ruoyi.report.service;

import com.ruoyi.report.domain.ReportRegisterRole;

import java.util.List;

/**
 * 报表注册角色关联Service接口
 *
 * @author gssong
 * @date 2023-01-07
 */
public interface IReportRegisterRoleService {

    /**
     * 新增
     *
     * @param entity
     * @return
     */
    Boolean insert(ReportRegisterRole entity);

    /**
     * 批量新增
     *
     * @param list
     * @return
     */
    Boolean insertBatch(List<ReportRegisterRole> list);

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    Boolean deleteBatch(List<Long> ids);

    /**
     * 按照注册id与角色id删除
     *
     * @param reportRegisterId
     * @param roleId
     * @return
     */
    void deleteByReportRegisterIdAndRoleId(Long reportRegisterId, Long roleId);

    /**
     * 报表授权
     *
     * @param reportRegisterRole
     * @return
     */
    Boolean reportAuth(ReportRegisterRole reportRegisterRole);

    /**
     * 按照注册id查询角色
     *
     * @param reportRegisterId
     * @return
     */
    List<ReportRegisterRole> getByReportRegisterId(Long reportRegisterId);

    /**
     * 按照注册id与角色id查询
     *
     * @param reportRegisterId
     * @param roleIds
     * @return
     */
    List<ReportRegisterRole> getByReportRegisterIdAndRoleIds(Long reportRegisterId, List<Long> roleIds);
}
