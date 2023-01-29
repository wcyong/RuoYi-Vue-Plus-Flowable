package com.ruoyi.report.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.report.domain.ReportRegister;
import com.ruoyi.report.domain.ReportRegisterRole;
import com.ruoyi.report.domain.vo.ReportDbVo;
import com.ruoyi.report.service.IReportRegisterRoleService;
import com.ruoyi.system.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.*;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.report.domain.vo.ReportRegisterVo;
import com.ruoyi.report.domain.bo.ReportRegisterBo;
import com.ruoyi.report.service.IReportRegisterService;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 报表注册
 *
 * @author gssong
 * @date 2023-01-07
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/report/reportRegister")
public class ReportRegisterController extends BaseController {

    private final IReportRegisterService iReportRegisterService;

    private final IReportRegisterRoleService iReportRegisterRoleService;

    private final SysRoleMapper sysRoleMapper;

    /**
     * 分页查询
     *
     * @param reportDbVo
     * @param pageQuery
     * @return
     */
    @GetMapping("/selectReportDbPage")
    public TableDataInfo<ReportDbVo> selectReportDbPage(ReportDbVo reportDbVo, PageQuery pageQuery) {
        return iReportRegisterService.selectReportDbPage(reportDbVo, pageQuery);
    }

    /**
     * 查询报表注册列表
     */
    @SaCheckPermission("report:reportRegister:list")
    @GetMapping("/list")
    public TableDataInfo<ReportRegisterVo> list(ReportRegisterBo bo, PageQuery pageQuery) {
        return iReportRegisterService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出报表注册列表
     */
    @SaCheckPermission("report:reportRegister:export")
    @Log(title = "报表注册", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ReportRegisterBo bo, HttpServletResponse response) {
        List<ReportRegisterVo> list = iReportRegisterService.queryList(bo);
        ExcelUtil.exportExcel(list, "报表注册", ReportRegisterVo.class, response);
    }

    /**
     * 获取报表注册详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("report:reportRegister:query")
    @GetMapping("/{id}")
    public R<ReportRegisterVo> getInfo(@NotNull(message = "主键不能为空")
                                       @PathVariable Long id) {
        return R.ok(iReportRegisterService.queryById(id));
    }

    /**
     * 新增报表注册
     */
    @SaCheckPermission("report:reportRegister:add")
    @Log(title = "报表注册", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ReportRegisterBo bo) {
        return toAjax(iReportRegisterService.insertByBo(bo));
    }

    /**
     * 修改报表注册
     */
    @SaCheckPermission("report:reportRegister:edit")
    @Log(title = "报表注册", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ReportRegisterBo bo) {
        return toAjax(iReportRegisterService.updateByBo(bo));
    }

    /**
     * 删除报表注册
     *
     * @param ids 主键串
     */
    @SaCheckPermission("report:reportRegister:remove")
    @Log(title = "报表注册", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(iReportRegisterService.deleteByIds(Arrays.asList(ids)));
    }

    /**
     * 报表授权
     *
     * @param: reportRegisterRole
     * @return: com.ruoyi.common.core.domain.R<java.lang.Void>
     * @author: gssong
     * @date: 2023/1/7 19:27
     */
    @SaCheckPermission("report:reportRegister:auth")
    @PostMapping("/reportAuth")
    @Log(title = "报表注册", businessType = BusinessType.UPDATE)
    public R<Void> reportAuth(@RequestBody ReportRegisterRole reportRegisterRole) {
        return toAjax(iReportRegisterRoleService.reportAuth(reportRegisterRole));
    }

    /**
     * 按照注册id与角色id查询角色
     *
     * @param: reportRegisterRole
     * @return: com.ruoyi.common.core.domain.R<java.util.List < com.ruoyi.common.core.domain.entity.SysRole>>
     * @author: gssong
     * @date: 2023/1/7 19:27
     */
    @PostMapping("/getRoleListByReportRegisterIdAndRoleIds")
    public R<List<SysRole>> getRoleListByReportRegisterIdAndRoleIds(@RequestBody ReportRegisterRole reportRegisterRole) {
        List<ReportRegisterRole> registerRoles = iReportRegisterRoleService.getByReportRegisterId(reportRegisterRole.getReportRegisterId());
        if (CollUtil.isNotEmpty(registerRoles)) {
            List<Long> roleIds = new ArrayList<>();
            if (CollUtil.isNotEmpty(reportRegisterRole.getRoleIds())) {
                roleIds.addAll(reportRegisterRole.getRoleIds());
            }
            roleIds.addAll(registerRoles.stream().map(ReportRegisterRole::getRoleId).collect(Collectors.toList()));
            LambdaQueryWrapper<SysRole> wrapper = Wrappers.lambdaQuery();
            wrapper.in(SysRole::getRoleId, roleIds);
            return R.ok(sysRoleMapper.selectList(wrapper));
        }
        return R.ok(Collections.emptyList());
    }

    /**
     * 按照注册id与角色id删除
     *
     * @param: reportRegisterId
     * @param: roleId
     * @return: com.ruoyi.common.core.domain.R<java.lang.Void>
     * @author: gssong
     * @date: 2023/1/8 12:25
     */
    @DeleteMapping("/deleteByReportRegisterIdAndRoleId/{reportRegisterId}/{roleId}")
    public R<Void> deleteByReportRegisterIdAndRoleId(@PathVariable Long reportRegisterId, @PathVariable Long roleId) {
        iReportRegisterRoleService.deleteByReportRegisterIdAndRoleId(reportRegisterId, roleId);
        return R.ok();
    }

    /**
     * 获取当前登录人员拥有的报表
     *
     * @return: com.ruoyi.common.core.domain.R<java.util.List < com.ruoyi.report.domain.ReportRegister>>
     * @author: gssong
     * @date: 2023/1/8 15:26
     */
    @GetMapping("/getReportListByCurrentRole")
    public R<List<ReportRegister>> getReportListByCurrentRole() {
        return R.ok(iReportRegisterService.getReportListByCurrentRole());
    }

    /**
     * 校验是否有权限
     *
     * @param: reportCode
     * @return: com.ruoyi.common.core.domain.R<java.lang.String>
     * @author: gssong
     * @date: 2023/1/8 16:25
     */
    @GetMapping("/checkReportAuth/{reportCode}")
    public R<String> checkReportAuth(@PathVariable String reportCode) {
        return iReportRegisterService.checkReportAuth(reportCode);
    }
}
