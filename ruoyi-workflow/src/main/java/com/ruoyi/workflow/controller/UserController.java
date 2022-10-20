package com.ruoyi.workflow.controller;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.workflow.domain.bo.SysUserBo;
import com.ruoyi.workflow.domain.bo.SysUserMultiBo;
import com.ruoyi.workflow.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 工作流用户信息
 *
 * @author gssong
 * @date 2022-03-25
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/user")
public class UserController {

    private final IUserService iUserService;

    /**
     * 分页查询工作流选人,角色，部门等
     * @param: sysUserBo
     * @return: com.ruoyi.common.core.domain.R<java.util.Map<java.lang.String,java.lang.Object>>
     * @author: gssong
     * @date: 2021/12/10
     */
    @PostMapping("/getWorkflowUserListByPage")
    public R<Map<String,Object>> getWorkflowUserListByPage(@RequestBody SysUserBo sysUserBo){
        return R.ok(iUserService.getWorkflowUserListByPage(sysUserBo));
    }

    /**
     * 分页查询工作流选择加签人员
     * @param: sysUserMultiBo
     * @return: com.ruoyi.common.core.domain.R<java.util.Map<java.lang.String,java.lang.Object>>
     * @author: gssong
     * @date: 2022/4/22 21:17
     */
    @PostMapping("/getWorkflowAddMultiListByPage")
    public R<Map<String,Object>> getWorkflowAddMultiListByPage(@RequestBody SysUserMultiBo sysUserMultiBo){
        return R.ok(iUserService.getWorkflowAddMultiListByPage(sysUserMultiBo));
    }


    /**
     * 分页查询用户
     * @param: sysUserBo
     * @return: com.ruoyi.common.core.domain.R<java.util.Map<java.lang.String,java.lang.Object>>
     * @author: gssong
     * @date: 2022/10/12 12:40
     */
    @PostMapping("/getUserListByPage")
    public R<Map<String,Object>> getUserListByPage(@RequestBody SysUserBo sysUserBo){
        return R.ok(iUserService.getUserListByPage(sysUserBo));
    }

    /**
     * 分页查询角色
     * @param: sysUserBo
     * @return: com.ruoyi.common.core.domain.R<java.util.Map<java.lang.String,java.lang.Object>>
     * @author: gssong
     * @date: 2022/10/12 12:40
     */
    @PostMapping("/getRoleListByPage")
    public R<Map<String,Object>> getRoleListByPage(@RequestBody SysUserBo sysUserBo){
        return R.ok(iUserService.getRoleListByPage(sysUserBo));
    }


    /**
     * 查询部门
     * @return: com.ruoyi.common.core.domain.R<java.util.List<com.ruoyi.common.core.domain.entity.SysDept>>
     * @author: gssong
     * @date: 2022/10/12 12:40
     */
    @GetMapping("/getDeptList")
    public R<List<SysDept>> getDeptList(){
        return R.ok(iUserService.getDeptList());
    }
}
