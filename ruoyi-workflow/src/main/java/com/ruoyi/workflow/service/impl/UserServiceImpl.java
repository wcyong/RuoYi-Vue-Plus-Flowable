package com.ruoyi.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.UserStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysUserRole;
import com.ruoyi.system.mapper.*;
import com.ruoyi.workflow.domain.bo.SysUserBo;
import com.ruoyi.workflow.domain.bo.SysUserMultiBo;
import com.ruoyi.workflow.domain.vo.MultiVo;
import com.ruoyi.workflow.service.IUserService;
import com.ruoyi.workflow.utils.WorkFlowUtils;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.ruoyi.workflow.common.constant.ActConstant.*;

/**
 * @description: 选人业务层
 * @author: gssong
 * @date: 2021/10/17 14:57
 */
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements IUserService {
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final TaskService taskService;
    private final RuntimeService runtimeService;

    /**
     * @description: 按照用户id查询用户集合
     * @param: userIds
     * @return: java.util.List<com.ruoyi.common.core.domain.entity.SysUser>
     * @author: gssong
     * @date: 2021/12/10
     */
    @Override
    public List<SysUser> selectListUserByIds(List<Long> userIds) {
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getUserId, userIds));
    }

    /**
     * @description: 按照用户id查询用户
     * @param: userId
     * @return: com.ruoyi.common.core.domain.entity.SysUser
     * @author: gssong
     * @date: 2021/12/10
     */
    @Override
    public SysUser selectUserById(Long userId) {
        return userMapper.selectUserById(userId);
    }

    /**
     * @description: 分页查询工作流选人, 角色，部门等
     * @param: sysUserBo
     * @return: java.util.Map<java.lang.String, java.lang.Object>
     * @author: gssong
     * @date: 2021/12/10
     */
    @Override
    public Map<String, Object> getWorkflowUserListByPage(SysUserBo sysUserBo) {
        Map<String, Object> map = new HashMap<>(16);
        if (StringUtils.isNotEmpty(sysUserBo.getParams())) {
            LambdaQueryWrapper<SysUser> queryWrapper = Wrappers.lambdaQuery();
            //检索条件
            queryWrapper.eq(StringUtils.isNotEmpty(sysUserBo.getDeptId()), SysUser::getDeptId, sysUserBo.getDeptId());
            queryWrapper.eq(SysUser::getStatus, UserStatus.OK.getCode());
            queryWrapper.like(StringUtils.isNotEmpty(sysUserBo.getUserName()), SysUser::getUserName, sysUserBo.getUserName());
            queryWrapper.like(StringUtils.isNotEmpty(sysUserBo.getPhonenumber()), SysUser::getPhonenumber, sysUserBo.getPhonenumber());
            Page<SysUser> page = new Page<>(sysUserBo.getPageNum(), sysUserBo.getPageSize());
            // 按用户id查询
            List<Long> paramList = Arrays.stream(sysUserBo.getParams().split(",")).map(Long::valueOf).collect(Collectors.toList());
            if (WORKFLOW_PERSON.equals(sysUserBo.getType()) || WORKFLOW_RULE.equals(sysUserBo.getType())) {
                queryWrapper.in(SysUser::getUserId, paramList);
                Page<SysUser> userPage = userMapper.selectPage(page, queryWrapper);
                if (CollectionUtil.isNotEmpty(sysUserBo.getIds())) {
                    List<SysUser> list = userMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getUserId, sysUserBo.getIds()));
                    map.put("list", list);
                }
                map.put("page", TableDataInfo.build(recordPage(userPage)));
                return map;
                //按角色id查询用户
            } else if (WORKFLOW_ROLE.equals(sysUserBo.getType())) {
                List<SysRole> sysRoles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>().in(SysRole::getRoleId, paramList));
                if (CollectionUtil.isNotEmpty(sysRoles)) {
                    List<Long> collectRoleId = sysRoles.stream().map(SysRole::getRoleId).collect(Collectors.toList());
                    List<SysUserRole> sysUserRoles = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getRoleId, collectRoleId));
                    queryWrapper.in(SysUser::getUserId, sysUserRoles.stream().map(SysUserRole::getUserId).collect(Collectors.toList()));
                    Page<SysUser> userPage = userMapper.selectPage(page, queryWrapper);
                    if (CollectionUtil.isNotEmpty(sysUserBo.getIds())) {
                        List<SysUser> list = userMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getUserId, sysUserBo.getIds()));
                        map.put("list", list);
                    }
                    LambdaQueryWrapper<SysRole> wrapper = Wrappers.lambdaQuery();
                    wrapper.in(SysRole::getRoleId, Arrays.stream(sysUserBo.getParamIds().split(",")).map(Long::valueOf).collect(Collectors.toList()));
                    List<SysRole> roleList = roleMapper.selectList(wrapper);
                    map.put("roleList", roleList);
                    map.put("page", TableDataInfo.build(recordPage(userPage)));
                    return map;
                }
                //按部门id查询用户
            } else if (WORKFLOW_DEPT.equals(sysUserBo.getType())) {
                queryWrapper.in(SysUser::getDeptId, paramList);
                Page<SysUser> userPage = userMapper.selectPage(page, queryWrapper);
                if (CollectionUtil.isNotEmpty(sysUserBo.getIds())) {
                    List<SysUser> list = userMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getUserId, sysUserBo.getIds()));
                    map.put("list", list);
                }
                LambdaQueryWrapper<SysDept> wrapper = Wrappers.lambdaQuery();
                wrapper.in(SysDept::getDeptId, Arrays.stream(sysUserBo.getParamIds().split(",")).map(Long::valueOf).collect(Collectors.toList()));
                List<SysDept> deptList = deptMapper.selectList(wrapper);
                map.put("deptList", deptList);
                map.put("page", TableDataInfo.build(recordPage(userPage)));
                return map;
            }
        }
        return map;
    }

    /**
     * 翻译部门
     *
     * @param page
     * @return
     */
    private Page<SysUser> recordPage(Page<SysUser> page) {
        List<SysUser> records = page.getRecords();
        if (CollectionUtil.isEmpty(records)) {
            return page;
        }
        List<Long> collectDeptId = records.stream().map(SysUser::getDeptId).filter(Objects::nonNull).collect(Collectors.toList());
        if (CollectionUtil.isEmpty(collectDeptId)) {
            return page;
        }
        List<SysDept> sysDeptList = deptMapper.selectBatchIds(collectDeptId);
        records.forEach(e -> {
            SysDept sysDept = sysDeptList.stream().filter(d -> d.getDeptId().equals(e.getDeptId())).findFirst().orElse(null);
            if (ObjectUtil.isNotNull(sysDept)) {
                e.setDept(sysDept);
            }
        });
        page.setRecords(records);
        return page;
    }

    /**
     * @description: 分页查询工作流选择加签人员
     * @param: sysUserMultiBo
     * @return: java.util.Map<java.lang.String, java.lang.Object>
     * @author: gssong
     * @date: 2022/4/22 21:17
     */
    @Override
    public Map<String, Object> getWorkflowAddMultiListByPage(SysUserMultiBo sysUserMultiBo) {
        Map<String, Object> map = new HashMap<>(16);
        Task task = taskService.createTaskQuery().taskId(sysUserMultiBo.getTaskId()).singleResult();
        MultiVo multiInstance = WorkFlowUtils.isMultiInstance(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
        LambdaQueryWrapper<SysUser> queryWrapper = Wrappers.lambdaQuery();
        //检索条件
        queryWrapper.eq(StringUtils.isNotEmpty(sysUserMultiBo.getDeptId()), SysUser::getDeptId, sysUserMultiBo.getDeptId());
        queryWrapper.eq(SysUser::getStatus, UserStatus.OK.getCode());
        if (ObjectUtil.isNotEmpty(multiInstance)) {
            List<Long> assigneeList = (List) runtimeService.getVariable(task.getExecutionId(), multiInstance.getAssigneeList());
            queryWrapper.notIn(CollectionUtil.isNotEmpty(assigneeList), SysUser::getUserId, assigneeList);
        }
        queryWrapper.like(StringUtils.isNotEmpty(sysUserMultiBo.getUserName()), SysUser::getUserName, sysUserMultiBo.getUserName());
        queryWrapper.like(StringUtils.isNotEmpty(sysUserMultiBo.getPhonenumber()), SysUser::getPhonenumber, sysUserMultiBo.getPhonenumber());
        Page<SysUser> page = new Page<>(sysUserMultiBo.getPageNum(), sysUserMultiBo.getPageSize());
        Page<SysUser> userPage = userMapper.selectPage(page, queryWrapper);
        if (CollectionUtil.isNotEmpty(sysUserMultiBo.getIds())) {
            List<SysUser> list = userMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getUserId, sysUserMultiBo.getIds()));
            map.put("list", list);
        }
        map.put("page", TableDataInfo.build(recordPage(userPage)));
        return map;
    }

    /**
     * @description: 查询审批人
     * @param: params 参数 用户id，角色id，部门id等
     * @param: chooseWay 选择方式
     * @param: nodeName 节点名称
     * @return: java.util.List<java.lang.Long>
     * @author: gssong
     * @date: 2022/4/11 13:36
     */
    @Override
    public List<Long> getAssigneeIdList(String params, String chooseWay, String nodeName) {
        List<Long> paramList = Arrays.stream(params.split(",")).map(Long::valueOf).collect(Collectors.toList());
        List<SysUser> list = null;
        LambdaQueryWrapper<SysUser> queryWrapper = Wrappers.lambdaQuery();
        // 按用户id查询
        if (WORKFLOW_PERSON.equals(chooseWay)) {
            queryWrapper.in(SysUser::getUserId, paramList);
            list = userMapper.selectList(queryWrapper);
            //按角色id查询用户
        } else if (WORKFLOW_ROLE.equals(chooseWay)) {
            List<SysRole> sysRoles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>().in(SysRole::getRoleId, paramList));
            if (CollectionUtil.isNotEmpty(sysRoles)) {
                List<Long> collectRoleId = sysRoles.stream().map(SysRole::getRoleId).collect(Collectors.toList());
                List<SysUserRole> sysUserRoles = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getRoleId, collectRoleId));
                queryWrapper.in(SysUser::getUserId, sysUserRoles.stream().map(SysUserRole::getUserId).collect(Collectors.toList()));
                list = userMapper.selectList(queryWrapper);
            }
            //按部门id查询用户
        } else if (WORKFLOW_DEPT.equals(chooseWay)) {
            queryWrapper.in(SysUser::getDeptId, paramList);
            list = userMapper.selectList(queryWrapper);
        }
        if (CollectionUtil.isEmpty(list)) {
            throw new ServiceException(nodeName + "任务环节未配置审批人");
        }
        return list.stream().map(SysUser::getUserId).collect(Collectors.toList());
    }

    /**
     * @description: 分页查询用户
     * @param: sysUserBo
     * @return: java.util.Map<java.lang.String, java.lang.Object>
     * @author: gssong
     * @date: 2022/10/12 12:40
     */
    @Override
    public Map<String, Object> getUserListByPage(SysUserBo sysUserBo) {
        Map<String, Object> map = new HashMap<>(16);
        LambdaQueryWrapper<SysUser> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(StringUtils.isNotEmpty(sysUserBo.getDeptId()), SysUser::getDeptId, sysUserBo.getDeptId());
        queryWrapper.eq(SysUser::getStatus, UserStatus.OK.getCode());
        queryWrapper.like(StringUtils.isNotEmpty(sysUserBo.getUserName()), SysUser::getUserName, sysUserBo.getUserName());
        queryWrapper.like(StringUtils.isNotEmpty(sysUserBo.getPhonenumber()), SysUser::getPhonenumber, sysUserBo.getPhonenumber());
        Page<SysUser> page = new Page<>(sysUserBo.getPageNum(), sysUserBo.getPageSize());
        Page<SysUser> userPage = userMapper.selectPage(page, queryWrapper);
        if (CollectionUtil.isNotEmpty(sysUserBo.getIds())) {
            List<SysUser> list = userMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getUserId, sysUserBo.getIds()));
            map.put("list", list);
        }
        map.put("page", TableDataInfo.build(recordPage(userPage)));
        return map;
    }

    /**
     * @description: 分页查询角色
     * @param: sysUserBo
     * @return: java.util.Map<java.lang.String, java.lang.Object>
     * @author: gssong
     * @date: 2022/10/12 12:40
     */
    @Override
    public Map<String, Object> getRoleListByPage(SysUserBo sysUserBo) {
        Map<String, Object> map = new HashMap<>(16);
        LambdaQueryWrapper<SysRole> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(SysRole::getStatus, UserStatus.OK.getCode());
        queryWrapper.like(StringUtils.isNotEmpty(sysUserBo.getRoleName()), SysRole::getRoleName, sysUserBo.getRoleName());
        queryWrapper.like(StringUtils.isNotEmpty(sysUserBo.getRoleKey()), SysRole::getRoleKey, sysUserBo.getRoleKey());
        Page<SysRole> page = new Page<>(sysUserBo.getPageNum(), sysUserBo.getPageSize());
        Page<SysRole> rolePage = roleMapper.selectPage(page, queryWrapper);
        if (CollectionUtil.isNotEmpty(sysUserBo.getIds())) {
            List<SysRole> list = roleMapper.selectList(new LambdaQueryWrapper<SysRole>().in(SysRole::getRoleId, sysUserBo.getIds()));
            map.put("list", list);
        }
        map.put("page", TableDataInfo.build(rolePage));
        return map;
    }

    /**
     * @description: 查询部门
     * @return: java.util.List<com.ruoyi.common.core.domain.entity.SysDept>
     * @author: gssong
     * @date: 2022/10/12 12:40
     */
    @Override
    public List<SysDept> getDeptList() {
        LambdaQueryWrapper<SysDept> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(SysDept::getStatus, UserStatus.OK.getCode());
        return deptMapper.selectDeptList(queryWrapper);
    }

    /**
     * @description: 按照部门id查人员
     * @param: deptIds
     * @return: java.util.List<com.ruoyi.common.core.domain.entity.SysUser>
     * @author: gssong
     * @date: 2022/12/24 13:32
     */
    @Override
    public List<SysUser> getUserListByDeptIds(List<Long> deptIds) {
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        wrapper.in(SysUser::getDeptId, deptIds);
        return userMapper.selectList(wrapper);
    }

    /**
     * @description: 按照角色id查人员
     * @param: roleIds
     * @return: java.util.List<com.ruoyi.common.core.domain.entity.SysUser>
     * @author: gssong
     * @date: 2022/12/24 13:35
     */
    @Override
    public List<SysUser> getUserListByRoleIds(List<Long> roleIds) {
        LambdaQueryWrapper<SysUserRole> userRoleLambdaQueryWrapper = Wrappers.lambdaQuery();
        userRoleLambdaQueryWrapper.in(SysUserRole::getRoleId, roleIds);
        List<SysUserRole> sysUserRoles = userRoleMapper.selectList(userRoleLambdaQueryWrapper);
        if (CollUtil.isNotEmpty(sysUserRoles)) {
            List<Long> userIds = sysUserRoles.stream().map(SysUserRole::getUserId).collect(Collectors.toList());
            LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
            wrapper.in(SysUser::getUserId, userIds);
            return userMapper.selectList(wrapper);
        }
        return Collections.emptyList();
    }
}
