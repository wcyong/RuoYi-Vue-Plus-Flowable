package com.ruoyi.common.core.service;

import java.util.List;

/**
 * 报表注册Service服务
 *
 * @author gssong
 * @date 2023-01-17
 */
public interface ReportRegisterService {

    /**
     * 按照角色id删除报表权限
     *
     * @param roleIds
     * @return
     */
    Boolean deleteByRoleIds(List<Long> roleIds);
}
