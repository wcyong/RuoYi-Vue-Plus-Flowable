package com.ruoyi.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.workflow.common.constant.ActConstant;
import com.ruoyi.workflow.common.enums.BusinessStatusEnum;
import com.ruoyi.workflow.domain.*;
import com.ruoyi.workflow.domain.bo.*;
import com.ruoyi.workflow.domain.vo.*;
import com.ruoyi.workflow.flowable.cmd.AddSequenceMultiInstanceCmd;
import com.ruoyi.workflow.flowable.cmd.AttachmentCmd;
import com.ruoyi.workflow.utils.CompleteTaskUtils;
import com.ruoyi.workflow.flowable.cmd.DeleteSequenceMultiInstanceCmd;
import com.ruoyi.workflow.flowable.factory.WorkflowService;
import com.ruoyi.workflow.mapper.TaskMapper;
import com.ruoyi.workflow.service.*;
import com.ruoyi.workflow.utils.WorkFlowUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.flowable.bpmn.model.*;
import org.flowable.engine.ManagementService;
import org.flowable.engine.impl.bpmn.behavior.ParallelMultiInstanceBehavior;
import org.flowable.engine.impl.bpmn.behavior.SequentialMultiInstanceBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Attachment;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.ruoyi.common.helper.LoginHelper.getUserId;

/**
 * @description: 任务业务层
 * @author: gssong
 * @date: 2021/10/17 14:57
 */
@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends WorkflowService implements ITaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);

    private final IUserService iUserService;

    private final IActBusinessStatusService iActBusinessStatusService;

    private final IActTaskNodeService iActTaskNodeService;

    private final IActNodeAssigneeService iActNodeAssigneeService;

    private final IActBusinessRuleService iActBusinessRuleService;

    private final IActHiTaskInstService iActHiTaskInstService;

    private final ManagementService managementService;

    private final TaskMapper taskMapper;

    private final IActProcessDefSetting iActProcessDefSetting;

    private final IProcessInstanceService iProcessInstanceService;


    /**
     * @description: 查询当前用户的待办任务
     * @param: req
     * @return: com.ruoyi.common.core.page.TableDataInfo<com.ruoyi.workflow.domain.vo.TaskWaitingVo>
     * @author: gssong
     * @date: 2021/10/17
     */
    @Override
    public TableDataInfo<TaskWaitingVo> getTaskWaitByPage(TaskBo req) {
        //当前登录人
        String currentUserId = LoginHelper.getLoginUser().getUserId().toString();
        TaskQuery query = taskService.createTaskQuery()
            //候选人或者办理人
            .taskCandidateOrAssigned(currentUserId)
            .orderByTaskCreateTime().asc();
        if (StringUtils.isNotEmpty(req.getTaskName())) {
            query.taskNameLikeIgnoreCase("%" + req.getTaskName() + "%");
        }
        if (StringUtils.isNotEmpty(req.getProcessDefinitionName())) {
            query.processDefinitionNameLike("%" + req.getProcessDefinitionName() + "%");
        }
        List<Task> taskList = query.listPage(req.getPageNum(), req.getPageSize());
        if (CollectionUtil.isEmpty(taskList)) {
            return new TableDataInfo<>();
        }
        long total = query.count();
        List<TaskWaitingVo> list = new ArrayList<>();
        //流程实例id
        Set<String> processInstanceIds = taskList.stream().map(Task::getProcessInstanceId).collect(Collectors.toSet());
        //流程定义id
        List<String> processDefinitionIds = taskList.stream().map(Task::getProcessDefinitionId).collect(Collectors.toList());
        //查询流程实例
        List<ProcessInstance> processInstanceList = runtimeService.createProcessInstanceQuery().processInstanceIds(processInstanceIds).list();
        //查询流程定义设置
        List<ActProcessDefSettingVo> processDefSettingLists = iActProcessDefSetting.getProcessDefSettingByDefIds(processDefinitionIds);
        //办理人
        List<Long> assignees = taskList.stream().filter(e -> StringUtils.isNotBlank(e.getAssignee())).map(e -> Long.valueOf(e.getAssignee())).collect(Collectors.toList());
        //流程发起人
        List<Long> userIds = processInstanceList.stream().map(e -> Long.valueOf(e.getStartUserId())).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(assignees)) {
            userIds.addAll(assignees);
        }
        List<SysUser> userList = iUserService.selectListUserByIds(userIds);

        for (Task task : taskList) {
            TaskWaitingVo taskWaitingVo = new TaskWaitingVo();
            BeanUtils.copyProperties(task, taskWaitingVo);
            taskWaitingVo.setAssigneeId(StringUtils.isNotBlank(task.getAssignee()) ? Long.valueOf(task.getAssignee()) : null);
            taskWaitingVo.setSuspensionState(task.isSuspended());
            taskWaitingVo.setProcessStatus(!task.isSuspended() ? "激活" : "挂起");
            processInstanceList.stream().filter(e -> e.getProcessInstanceId().equals(task.getProcessInstanceId())).findFirst()
                .ifPresent(e -> {
                    //流程发起人
                    String startUserId = e.getStartUserId();
                    taskWaitingVo.setStartUserId(startUserId);
                    if (StringUtils.isNotBlank(startUserId)) {
                        userList.stream().filter(u -> u.getUserId().toString().equals(startUserId)).findFirst().ifPresent(u -> {
                            taskWaitingVo.setStartUserNickName(u.getNickName());
                        });
                    }
                    taskWaitingVo.setProcessDefinitionVersion(e.getProcessDefinitionVersion());
                    taskWaitingVo.setProcessDefinitionName(e.getProcessDefinitionName());
                    taskWaitingVo.setBusinessKey(e.getBusinessKey());
                });
            // 查询流程定义设置
            processDefSettingLists.stream().filter(e -> e.getProcessDefinitionId().equals(task.getProcessDefinitionId())).findFirst()
                .ifPresent(taskWaitingVo::setActProcessDefSetting);
            list.add(taskWaitingVo);
        }
        if (CollectionUtil.isNotEmpty(list)) {
            //认领与归还标识
            list.forEach(e -> {
                List<IdentityLink> identityLinkList = WorkFlowUtils.getCandidateUser(e.getId());
                if (CollectionUtil.isNotEmpty(identityLinkList)) {
                    List<String> collectType = identityLinkList.stream().map(IdentityLink::getType).collect(Collectors.toList());
                    if (StringUtils.isBlank(e.getAssignee()) && collectType.size() > 1 && collectType.contains(ActConstant.CANDIDATE)) {
                        e.setIsClaim(false);
                    } else if (StringUtils.isNotBlank(e.getAssignee()) && collectType.size() > 1 && collectType.contains(ActConstant.CANDIDATE)) {
                        e.setIsClaim(true);
                    }
                }
            });
            //办理人集合
            if (CollectionUtil.isNotEmpty(userList)) {
                list.forEach(e -> userList.stream().filter(t -> StringUtils.isNotBlank(e.getAssignee()) && t.getUserId().toString().equals(e.getAssigneeId().toString()))
                    .findFirst().ifPresent(t -> {
                        e.setAssignee(t.getNickName());
                        e.setAssigneeId(t.getUserId());
                    }));
            }
            //业务id集合
            List<String> businessKeyList = list.stream().map(TaskWaitingVo::getBusinessKey).collect(Collectors.toList());
            List<ActBusinessStatus> infoList = iActBusinessStatusService.getListInfoByBusinessKey(businessKeyList);
            if (CollectionUtil.isNotEmpty(infoList)) {
                list.forEach(e -> infoList.stream().filter(t -> t.getBusinessKey().equals(e.getBusinessKey()))
                    .findFirst().ifPresent(e::setActBusinessStatus));
            }
        }
        return new TableDataInfo<>(list, total);
    }

    /**
     * @description: 自定义sql查询当前用户的待办任务
     * @param: req
     * @param: pageQuery
     * @return: com.ruoyi.common.core.page.TableDataInfo<com.ruoyi.workflow.domain.vo.TaskWaitingVo>
     * @author: gssong
     * @date: 2022/11/7
     */
    @Override
    public TableDataInfo<TaskWaitingVo> getCustomTaskWaitByPage(TaskBo req, PageQuery pageQuery) {
        String assignee = LoginHelper.getUserId().toString();
        if (StringUtils.isBlank(assignee)) {
            throw new ServiceException("当前审批人id为空");
        }
        QueryWrapper<TaskWaitingVo> wrapper = Wrappers.query();
        Page<TaskWaitingVo> page = taskMapper.getCustomTaskWaitByPage(pageQuery.build(), wrapper, assignee);
        if (CollectionUtil.isEmpty(page.getRecords())) {
            return new TableDataInfo<>();
        }
        List<TaskWaitingVo> taskList = page.getRecords();
        //流程实例id
        Set<String> processInstanceIds = taskList.stream().map(TaskWaitingVo::getProcessInstanceId).collect(Collectors.toSet());
        //流程定义id
        List<String> processDefinitionIds = taskList.stream().map(TaskWaitingVo::getProcessDefinitionId).collect(Collectors.toList());
        //查询流程实例
        List<ProcessInstance> processInstanceList = runtimeService.createProcessInstanceQuery().processInstanceIds(processInstanceIds).list();
        //查询流程定义设置
        List<ActProcessDefSettingVo> processDefSettingLists = iActProcessDefSetting.getProcessDefSettingByDefIds(processDefinitionIds);
        //办理人
        List<Long> assignees = taskList.stream().filter(e -> StringUtils.isNotBlank(e.getAssignee())).map(e -> Long.valueOf(e.getAssignee())).collect(Collectors.toList());
        //流程发起人
        List<Long> userIds = processInstanceList.stream().map(e -> Long.valueOf(e.getStartUserId())).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(assignees)) {
            userIds.addAll(assignees);
        }
        List<SysUser> userList = iUserService.selectListUserByIds(userIds);
        for (TaskWaitingVo task : taskList) {
            task.setAssigneeId(StringUtils.isNotBlank(task.getAssignee()) ? Long.valueOf(task.getAssignee()) : null);
            task.setProcessStatus(task.getSuspensionState() ? "激活" : "挂起");
            // 查询流程实例
            processInstanceList.stream().filter(e -> e.getProcessInstanceId().equals(task.getProcessInstanceId())).findFirst()
                .ifPresent(e -> {
                    //流程发起人
                    String startUserId = e.getStartUserId();
                    task.setStartUserId(startUserId);
                    if (StringUtils.isNotBlank(startUserId)) {
                        userList.stream().filter(u -> u.getUserId().toString().equals(startUserId)).findFirst().ifPresent(u -> {
                            task.setStartUserNickName(u.getNickName());
                        });
                    }
                    task.setProcessDefinitionVersion(e.getProcessDefinitionVersion());
                    task.setProcessDefinitionName(e.getProcessDefinitionName());
                    task.setBusinessKey(e.getBusinessKey());
                });
            // 查询流程定义设置
            processDefSettingLists.stream().filter(e -> e.getProcessDefinitionId().equals(task.getProcessDefinitionId())).findFirst()
                .ifPresent(task::setActProcessDefSetting);
        }
        //认领与归还标识
        taskList.forEach(e -> {
            List<IdentityLink> identityLinkList = WorkFlowUtils.getCandidateUser(e.getId());
            if (CollectionUtil.isNotEmpty(identityLinkList)) {
                List<String> collectType = identityLinkList.stream().map(IdentityLink::getType).collect(Collectors.toList());
                if (StringUtils.isBlank(e.getAssignee()) && collectType.size() > 1 && collectType.contains(ActConstant.CANDIDATE)) {
                    e.setIsClaim(false);
                } else if (StringUtils.isNotBlank(e.getAssignee()) && collectType.size() > 1 && collectType.contains(ActConstant.CANDIDATE)) {
                    e.setIsClaim(true);
                }
            }
        });
        //办理人集合
        if (CollectionUtil.isNotEmpty(userList)) {
            taskList.forEach(e -> userList.stream().filter(t -> StringUtils.isNotBlank(e.getAssignee()) && t.getUserId().toString().equals(e.getAssigneeId().toString()))
                .findFirst().ifPresent(t -> {
                    e.setAssignee(t.getNickName());
                    e.setAssigneeId(t.getUserId());
                }));
        }
        //业务id集合
        List<String> businessKeyList = taskList.stream().map(TaskWaitingVo::getBusinessKey).collect(Collectors.toList());
        List<ActBusinessStatus> infoList = iActBusinessStatusService.getListInfoByBusinessKey(businessKeyList);
        if (CollectionUtil.isNotEmpty(infoList)) {
            taskList.forEach(e -> infoList.stream().filter(t -> t.getBusinessKey().equals(e.getBusinessKey()))
                .findFirst().ifPresent(e::setActBusinessStatus));
        }
        return TableDataInfo.build(page);
    }

    /**
     * @description: 办理任务
     * @param: req
     * @return: java.lang.Boolean
     * @author: gssong
     * @date: 2021/10/21
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean completeTask(TaskCompleteBo req) {
        // 1.查询任务
        Task task = taskService.createTaskQuery().taskId(req.getTaskId()).taskAssignee(getUserId().toString()).singleResult();

        if (ObjectUtil.isNull(task)) {
            throw new ServiceException(ActConstant.MESSAGE_CURRENT_TASK_IS_NULL);
        }

        if (task.isSuspended()) {
            throw new ServiceException(ActConstant.MESSAGE_SUSPENDED);
        }
        try {
            //办理委托任务
            if (ObjectUtil.isNotEmpty(task.getDelegationState()) && ActConstant.PENDING.equals(task.getDelegationState().name())) {
                taskService.resolveTask(req.getTaskId());
                ActHiTaskInst hiTaskInst = iActHiTaskInstService.getById(task.getId());
                TaskEntity newTask = WorkFlowUtils.createNewTask(task, hiTaskInst.getStartTime());
                taskService.addComment(newTask.getId(), task.getProcessInstanceId(), req.getMessage());
                taskService.complete(newTask.getId());
                ActHiTaskInst actHiTaskInst = new ActHiTaskInst();
                actHiTaskInst.setId(task.getId());
                actHiTaskInst.setStartTime(new Date());
                iActHiTaskInstService.updateById(actHiTaskInst);
                return true;
            }
            //流程定义设置
            ActProcessDefSettingVo setting = iActProcessDefSetting.getProcessDefSettingByDefId(task.getProcessDefinitionId());
            if (setting != null && !setting.getDefaultProcess()) {
                return CompleteTaskUtils.execute(req);
            }

            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
            // 2. 判断下一节点是否是会签 如果是会签 将选择的人员放到会签变量
            List<ActNodeAssignee> actNodeAssignees = iActNodeAssigneeService.getInfoByProcessDefinitionId(task.getProcessDefinitionId());
            for (ActNodeAssignee actNodeAssignee : actNodeAssignees) {
                String column = actNodeAssignee.getMultipleColumn();
                String assigneeId = actNodeAssignee.getAssigneeId();
                if (actNodeAssignee.getMultiple() && actNodeAssignee.getIsShow()) {
                    List<Long> userIdList = req.getAssignees(actNodeAssignee.getMultipleColumn());
                    if (CollectionUtil.isNotEmpty(userIdList)) {
                        taskService.setVariable(task.getId(), column, userIdList);
                    }
                }
                //判断是否有会签并且不需要弹窗选人的节点
                if (actNodeAssignee.getMultiple() && !actNodeAssignee.getIsShow() && (StringUtils.isBlank(column) || StringUtils.isBlank(assigneeId))) {
                    throw new ServiceException("请检查【" + processInstance.getProcessDefinitionKey() + "】配置 ");
                }
                if (actNodeAssignee.getMultiple() && !actNodeAssignee.getIsShow()) {
                    WorkFlowUtils.settingAssignee(task, actNodeAssignee, actNodeAssignee.getMultiple());
                }
            }
            // 3. 指定任务审批意见
            taskService.addComment(req.getTaskId(), task.getProcessInstanceId(), req.getMessage());
            // 设置变量
            if (CollectionUtil.isNotEmpty(req.getVariables())) {
                taskService.setVariables(req.getTaskId(), req.getVariables());
            }
            // 任务前执行集合
            List<TaskListenerVo> handleBeforeList = null;
            // 任务后执行集合
            List<TaskListenerVo> handleAfterList = null;
            ActNodeAssignee nodeEvent = actNodeAssignees.stream().filter(e -> task.getTaskDefinitionKey().equals(e.getNodeId())).findFirst().orElse(null);
            if (ObjectUtil.isNotEmpty(nodeEvent) && StringUtils.isNotBlank(nodeEvent.getTaskListener())) {
                List<TaskListenerVo> taskListenerVos = JsonUtils.parseArray(nodeEvent.getTaskListener(), TaskListenerVo.class);
                handleBeforeList = taskListenerVos.stream().filter(e -> ActConstant.HANDLE_BEFORE.equals(e.getEventType())).collect(Collectors.toList());
                handleAfterList = taskListenerVos.stream().filter(e -> ActConstant.HANDLE_AFTER.equals(e.getEventType())).collect(Collectors.toList());
            }
            // 任务前执行
            if (CollectionUtil.isNotEmpty(handleBeforeList)) {
                for (TaskListenerVo taskListenerVo : handleBeforeList) {
                    WorkFlowUtils.springInvokeMethod(taskListenerVo.getBeanName(), ActConstant.HANDLE_PROCESS
                        , task.getProcessInstanceId(), task.getId());
                }
            }
            // 4. 完成任务
            taskService.complete(req.getTaskId());
            // 5. 记录执行过的流程任务节点
            WorkFlowUtils.recordExecuteNode(task, actNodeAssignees);
            // 更新业务状态为：办理中
            iActBusinessStatusService.updateState(processInstance.getBusinessKey(), BusinessStatusEnum.WAITING, task.getProcessInstanceId());
            // 6. 查询下一个任务
            List<Task> taskList = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).list();
            // 7. 如果为空 办结任务
            boolean end = false;
            if (CollectionUtil.isEmpty(taskList)) {
                // 更新业务状态已完成 办结流程
                end = iActBusinessStatusService.updateState(processInstance.getBusinessKey(), BusinessStatusEnum.FINISH, processInstance.getProcessInstanceId());
            }
            // 任务后执行
            if (CollectionUtil.isNotEmpty(handleAfterList)) {
                for (TaskListenerVo taskListenerVo : handleAfterList) {
                    WorkFlowUtils.springInvokeMethod(taskListenerVo.getBeanName(), ActConstant.HANDLE_PROCESS
                        , task.getProcessInstanceId());
                }
            }
            if (CollectionUtil.isEmpty(taskList) && end) {
                return true;
            }
            // 抄送
            if (req.getIsCopy()) {
                if (StringUtils.isBlank(req.getAssigneeIds())) {
                    throw new ServiceException("抄送人不能为空 ");
                }
                TaskEntity newTask = WorkFlowUtils.createNewTask(task, new Date());
                taskService.addComment(newTask.getId(), task.getProcessInstanceId(),
                    LoginHelper.getUsername() + "【抄送】给" + req.getAssigneeNames());
                taskService.complete(newTask.getId());
                WorkFlowUtils.createSubTask(taskList, req.getAssigneeIds());
            }
            // 自动办理
            Boolean autoComplete = WorkFlowUtils.autoComplete(processInstance.getProcessInstanceId(), processInstance.getBusinessKey(), actNodeAssignees, req);
            if (autoComplete) {
                List<Task> nextTaskList = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).list();
                if (!CollectionUtil.isEmpty(nextTaskList)) {
                    for (Task t : nextTaskList) {
                        ActNodeAssignee nodeAssignee = actNodeAssignees.stream().filter(e -> t.getTaskDefinitionKey().equals(e.getNodeId())).findFirst().orElse(null);
                        if (ObjectUtil.isNull(nodeAssignee)) {
                            throw new ServiceException("请检查【" + t.getName() + "】节点配置");
                        }
                        WorkFlowUtils.settingAssignee(t, nodeAssignee, nodeAssignee.getMultiple());
                    }
                } else {
                    // 更新业务状态已完成 办结流程
                    return iActBusinessStatusService.updateState(processInstance.getBusinessKey(), BusinessStatusEnum.FINISH, processInstance.getProcessInstanceId());
                }
                // 发送站内信
                WorkFlowUtils.sendMessage(req.getSendMessage(), processInstance.getProcessInstanceId());
                return true;
            }
            // 8. 如果不为空 指定办理人
            List<Task> nextTaskList = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).list();
            if (CollectionUtil.isEmpty(nextTaskList)) {
                // 更新业务状态已完成 办结流程
                return iActBusinessStatusService.updateState(processInstance.getBusinessKey(), BusinessStatusEnum.FINISH, processInstance.getProcessInstanceId());
            }
            for (Task t : nextTaskList) {
                ActNodeAssignee nodeAssignee = actNodeAssignees.stream().filter(e -> t.getTaskDefinitionKey().equals(e.getNodeId())).findFirst().orElse(null);
                if (ObjectUtil.isNull(nodeAssignee)) {
                    throw new ServiceException("请检查【" + t.getName() + "】节点配置");
                }
                // 不需要弹窗选人
                if (!nodeAssignee.getIsShow() && StringUtils.isBlank(t.getAssignee()) && !nodeAssignee.getMultiple()) {
                    // 设置人员
                    WorkFlowUtils.settingAssignee(t, nodeAssignee, false);
                } else if (nodeAssignee.getIsShow() && StringUtils.isBlank(t.getAssignee()) && !nodeAssignee.getMultiple()) {
                    // 弹窗选人 根据当前任务节点id获取办理人
                    List<Long> assignees = req.getAssignees(t.getTaskDefinitionKey());
                    if (CollectionUtil.isEmpty(assignees)) {
                        throw new ServiceException("【" + t.getName() + "】任务环节未配置审批人");
                    }
                    // 设置选人
                    WorkFlowUtils.setAssignee(t, assignees);
                }
            }
            // 发送站内信
            WorkFlowUtils.sendMessage(req.getSendMessage(), processInstance.getProcessInstanceId());

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("办理失败:" + e.getMessage());
            iActBusinessStatusService.deleteCache(task.getProcessInstanceId());
            throw new ServiceException("办理失败:" + e.getMessage());
        }
    }

    /**
     * @description: 文件上传
     * @param: fileList
     * @param: taskId
     * @param: processInstanceId
     * @return: java.lang.Boolean
     * @author: gssong
     * @date: 2022/9/25 11:39
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean attachmentUpload(MultipartFile[] fileList, String taskId, String processInstanceId) {
        List<Attachment> taskAttachments = taskService.getTaskAttachments(taskId);
        if (CollectionUtil.isNotEmpty(taskAttachments)) {
            for (Attachment taskAttachment : taskAttachments) {
                taskService.deleteAttachment(taskAttachment.getId());
            }
        }
        AttachmentCmd attachmentCmd = new AttachmentCmd(fileList, taskId, processInstanceId);
        return managementService.executeCommand(attachmentCmd);
    }

    /**
     * @description: 附件下载
     * @param: attachmentId
     * @param: response
     * @return: void
     * @author: gssong
     * @date: 2022/9/25 15:26
     */
    @Override
    public void downloadAttachment(String attachmentId, HttpServletResponse response) {
        Attachment attachment = taskService.getAttachment(attachmentId);
        InputStream inputStream = taskService.getAttachmentContent(attachmentId);
        if (inputStream != null && attachment != null) {
            ServletOutputStream outputStream;
            try {
                outputStream = response.getOutputStream();
                byte[] bytes = IOUtils.toByteArray(inputStream);
                outputStream.write(bytes);
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @description: 查询当前用户的已办任务
     * @param: req
     * @return: com.ruoyi.common.core.page.TableDataInfo<com.ruoyi.workflow.domain.vo.TaskFinishVo>
     * @author: gssong
     * @date: 2021/10/23
     */
    @Override
    public TableDataInfo<TaskFinishVo> getTaskFinishByPage(TaskBo req) {
        //当前登录人
        String username = LoginHelper.getUserId().toString();
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery()
            .taskAssignee(username).finished().orderByHistoricTaskInstanceStartTime().asc();
        if (StringUtils.isNotBlank(req.getTaskName())) {
            query.taskNameLike(req.getTaskName());
        }
        List<HistoricTaskInstance> list = query.listPage(req.getPageNum(), req.getPageSize());
        long total = query.count();
        List<TaskFinishVo> taskFinishVoList = new ArrayList<>();
        for (HistoricTaskInstance hti : list) {
            TaskFinishVo taskFinishVo = new TaskFinishVo();
            BeanUtils.copyProperties(hti, taskFinishVo);
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(hti.getProcessDefinitionId()).singleResult();
            taskFinishVo.setProcessDefinitionName(processDefinition.getName());
            taskFinishVo.setProcessDefinitionKey(processDefinition.getKey());
            taskFinishVo.setVersion(processDefinition.getVersion());
            taskFinishVo.setAssigneeId(StringUtils.isNotBlank(hti.getAssignee()) ? Long.valueOf(hti.getAssignee()) : null);
            taskFinishVoList.add(taskFinishVo);
        }
        if (CollectionUtil.isNotEmpty(list)) {
            //办理人集合
            List<Long> assigneeList = taskFinishVoList.stream().map(TaskFinishVo::getAssigneeId).collect(Collectors.toList());
            if (CollectionUtil.isNotEmpty(assigneeList)) {
                List<SysUser> userList = iUserService.selectListUserByIds(assigneeList);
                if (CollectionUtil.isNotEmpty(userList)) {
                    taskFinishVoList.forEach(e -> userList.stream().filter(t -> t.getUserId().toString().equals(e.getAssigneeId().toString())).findFirst().ifPresent(t -> e.setAssignee(t.getNickName())));
                }
            }
        }
        return new TableDataInfo<>(taskFinishVoList, total);
    }


    /**
     * @description: 获取目标节点（下一个节点）
     * @param: req
     * @return: java.util.Map<java.lang.String, java.lang.Object>
     * @author: gssong
     * @date: 2021/10/23
     */
    @Override
    public Map<String, Object> getNextNodeInfo(NextNodeBo req) {
        Map<String, Object> map = new HashMap<>(16);
        TaskEntity task = (TaskEntity) taskService.createTaskQuery().taskId(req.getTaskId()).singleResult();
        if (task.isSuspended()) {
            throw new ServiceException(ActConstant.MESSAGE_SUSPENDED);
        }
        ActNodeAssignee nodeAssignee = iActNodeAssigneeService.getInfo(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
        //可驳回的节点
        List<ActTaskNode> taskNodeList = iActTaskNodeService.getListByInstanceId(task.getProcessInstanceId()).stream().filter(ActTaskNode::getIsBack).collect(Collectors.toList());
        map.put("backNodeList", taskNodeList);
        //当前流程实例状态
        ActBusinessStatus actBusinessStatus = iActBusinessStatusService.getInfoByProcessInstId(task.getProcessInstanceId());
        if (!ObjectUtil.isEmpty(actBusinessStatus)) {
            map.put("businessStatus", actBusinessStatus);
        }
        //委托流程
        if (ObjectUtil.isNotEmpty(task.getDelegationState()) && ActConstant.PENDING.equals(task.getDelegationState().name())) {
            ActNodeAssignee actNodeAssignee = new ActNodeAssignee();
            actNodeAssignee.setIsDelegate(false);
            actNodeAssignee.setIsTransmit(false);
            actNodeAssignee.setIsCopy(false);
            actNodeAssignee.setAddMultiInstance(false);
            actNodeAssignee.setDeleteMultiInstance(false);
            map.put("setting", actNodeAssignee);
            map.put("list", new ArrayList<>());
            map.put("isMultiInstance", false);
            return map;
        }
        //流程定义设置
        if (ObjectUtil.isNotEmpty(nodeAssignee)) {
            map.put("setting", nodeAssignee);
        } else {
            ActNodeAssignee actNodeAssignee = new ActNodeAssignee();
            actNodeAssignee.setIsDelegate(false);
            actNodeAssignee.setIsTransmit(false);
            actNodeAssignee.setIsCopy(false);
            actNodeAssignee.setAddMultiInstance(false);
            actNodeAssignee.setDeleteMultiInstance(false);
            map.put("setting", actNodeAssignee);
        }

        //判断当前是否为会签
        MultiVo isMultiInstance = WorkFlowUtils.isMultiInstance(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
        map.put("isMultiInstance", ObjectUtil.isNotEmpty(isMultiInstance));
        //查询任务
        List<Task> taskList = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).list();
        //可以减签的人员
        if (ObjectUtil.isNotEmpty(isMultiInstance)) {
            if (isMultiInstance.getType() instanceof ParallelMultiInstanceBehavior) {
                map.put("multiList", multiList(task, taskList, isMultiInstance.getType(), null));
            } else if (isMultiInstance.getType() instanceof SequentialMultiInstanceBehavior) {
                List<Long> assigneeList = (List<Long>) runtimeService.getVariable(task.getExecutionId(), isMultiInstance.getAssigneeList());
                map.put("multiList", multiList(task, taskList, isMultiInstance.getType(), assigneeList));
            }
        } else {
            map.put("multiList", new ArrayList<>());
        }
        //如果是会签最后一个人员审批选人
        if (CollectionUtil.isNotEmpty(taskList) && taskList.size() > 1) {
            //return null;
        }

        if (CollectionUtil.isNotEmpty(req.getVariables())) {
            taskService.setVariables(task.getId(), req.getVariables());
        }
        //流程定义
        String processDefinitionId = task.getProcessDefinitionId();
        //查询bpmn信息
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        //通过任务节点id，来获取当前节点信息
        FlowElement flowElement = bpmnModel.getFlowElement(task.getTaskDefinitionKey());
        //全部节点
        Collection<FlowElement> flowElements = bpmnModel.getProcesses().get(0).getFlowElements();
        //封装下一个用户任务节点信息
        List<ProcessNode> nextNodeList = new ArrayList<>();
        //保存没有表达式的节点
        List<ProcessNode> tempNodeList = new ArrayList<>();
        ExecutionEntityImpl executionEntity = (ExecutionEntityImpl) runtimeService.createExecutionQuery()
            .executionId(task.getExecutionId()).singleResult();
        WorkFlowUtils.getNextNodeList(flowElements, flowElement, executionEntity, nextNodeList, tempNodeList, task.getId(), null);
        if (CollectionUtil.isNotEmpty(nextNodeList)) {
            nextNodeList.removeIf(node -> !node.getExpression());
        }
        if (CollectionUtil.isNotEmpty(nextNodeList) && CollectionUtil.isNotEmpty(nextNodeList.stream().filter(e -> e.getExpression() != null && e.getExpression()).collect(Collectors.toList()))) {
            List<ProcessNode> nodeList = nextNodeList.stream().filter(e -> e.getExpression() != null && e.getExpression()).collect(Collectors.toList());
            List<ProcessNode> processNodeList = getProcessNodeAssigneeList(nodeList, task.getProcessDefinitionId());
            map.put("list", processNodeList);
        } else if (CollectionUtil.isNotEmpty(tempNodeList)) {
            List<ProcessNode> processNodeList = getProcessNodeAssigneeList(tempNodeList, task.getProcessDefinitionId());
            map.put("list", processNodeList);
        } else {
            map.put("list", nextNodeList);
        }
        map.put("processInstanceId", task.getProcessInstanceId());
        //流程定义设置
        ActProcessDefSettingVo setting = iActProcessDefSetting.getProcessDefSettingByDefId(task.getProcessDefinitionId());
        if (setting != null && !setting.getDefaultProcess()) {
            Map<String, Object> executableNode = iProcessInstanceService.getExecutableNode(task.getProcessInstanceId());
            map.putAll(executableNode);
            map.put("defaultProcess", true);
            map.put("list", Collections.emptyList());
            if (BusinessStatusEnum.WAITING.getStatus().equals(actBusinessStatus.getStatus())) {
                map.put("processNodeList", Collections.emptyList());
            }
        } else {
            map.put("defaultProcess", false);
            map.put("processNodeList", Collections.emptyList());
        }
        return map;
    }


    /**
     * @description: 可减签人员集合
     * @param: task  当前任务
     * @param: taskList  当前实例所有任务
     * @param: type  会签类型
     * @param: assigneeList 串行会签人员
     * @return: java.util.List<com.ruoyi.workflow.domain.vo.TaskVo>
     * @author: gssong
     * @date: 2022/4/24 11:17
     */
    private List<TaskVo> multiList(TaskEntity task, List<Task> taskList, Object type, List<Long> assigneeList) {
        List<TaskVo> taskListVo = new ArrayList<>();
        if (type instanceof SequentialMultiInstanceBehavior) {
            List<Long> userIds = assigneeList.stream().filter(userId -> !userId.toString().equals(task.getAssignee())).collect(Collectors.toList());
            List<SysUser> sysUsers = null;
            if (CollectionUtil.isNotEmpty(userIds)) {
                sysUsers = iUserService.selectListUserByIds(userIds);
            }
            for (Long userId : userIds) {
                TaskVo taskVo = new TaskVo();
                taskVo.setId("串行会签");
                taskVo.setExecutionId("串行会签");
                taskVo.setProcessInstanceId(task.getProcessInstanceId());
                taskVo.setName(task.getName());
                taskVo.setAssigneeId(String.valueOf(userId));
                if (CollectionUtil.isNotEmpty(sysUsers) && sysUsers != null) {
                    sysUsers.stream().filter(u -> u.getUserId().toString().equals(userId.toString())).findFirst().ifPresent(user -> taskVo.setAssignee(user.getNickName()));
                }
                taskListVo.add(taskVo);
            }
            return taskListVo;
        } else if (type instanceof ParallelMultiInstanceBehavior) {
            List<Task> tasks = taskList.stream().filter(e -> StringUtils.isBlank(e.getParentTaskId()) && !e.getExecutionId().equals(task.getExecutionId())
                && e.getTaskDefinitionKey().equals(task.getTaskDefinitionKey())).collect(Collectors.toList());
            if (CollectionUtil.isNotEmpty(tasks)) {
                List<Long> userIds = tasks.stream().map(e -> Long.valueOf(e.getAssignee())).collect(Collectors.toList());
                List<SysUser> sysUsers = null;
                if (CollectionUtil.isNotEmpty(userIds)) {
                    sysUsers = iUserService.selectListUserByIds(userIds);
                }
                for (Task t : tasks) {
                    TaskVo taskVo = new TaskVo();
                    taskVo.setId(t.getId());
                    taskVo.setExecutionId(t.getExecutionId());
                    taskVo.setProcessInstanceId(t.getProcessInstanceId());
                    taskVo.setName(t.getName());
                    taskVo.setAssigneeId(t.getAssignee());
                    if (CollectionUtil.isNotEmpty(sysUsers)) {
                        SysUser sysUser = sysUsers.stream().filter(u -> u.getUserId().toString().equals(t.getAssignee())).findFirst().orElse(null);
                        if (ObjectUtil.isNotEmpty(sysUser)) {
                            taskVo.setAssignee(sysUser.getNickName());
                        }
                    }
                    taskListVo.add(taskVo);
                }
                return taskListVo;
            }
        }
        return Collections.emptyList();
    }

    /**
     * @description: 设置节点审批人员
     * @param: nodeList节点列表
     * @param: definitionId 流程定义id
     * @return: java.util.List<com.ruoyi.workflow.domain.vo.ProcessNode>
     * @author: gssong
     * @date: 2021/10/23
     */
    private List<ProcessNode> getProcessNodeAssigneeList(List<ProcessNode> nodeList, String definitionId) {
        List<ActNodeAssignee> actNodeAssignees = iActNodeAssigneeService.getInfoByProcessDefinitionId(definitionId);
        if (CollUtil.isEmpty(actNodeAssignees)) {
            throw new ServiceException("当前流程定义未配置审批人，请联系管理员！");
        }
        for (ProcessNode processNode : nodeList) {
            if (CollectionUtil.isEmpty(actNodeAssignees)) {
                throw new ServiceException("该流程定义未配置，请联系管理员！");
            }
            ActNodeAssignee nodeAssignee = actNodeAssignees.stream().filter(e -> e.getNodeId().equals(processNode.getNodeId())).findFirst().orElse(null);

            //按角色 部门 人员id 等设置查询人员信息
            if (ObjectUtil.isNotNull(nodeAssignee) && StringUtils.isNotBlank(nodeAssignee.getAssigneeId())
                && nodeAssignee.getBusinessRuleId() == null && StringUtils.isNotBlank(nodeAssignee.getAssignee())) {
                processNode.setChooseWay(nodeAssignee.getChooseWay());
                processNode.setAssignee(nodeAssignee.getAssignee());
                processNode.setAssigneeId(nodeAssignee.getAssigneeId());
                processNode.setIsShow(nodeAssignee.getIsShow());
                if (nodeAssignee.getMultiple()) {
                    processNode.setNodeId(nodeAssignee.getMultipleColumn());
                }
                processNode.setMultiple(nodeAssignee.getMultiple());
                processNode.setMultipleColumn(nodeAssignee.getMultipleColumn());
                //按照业务规则设置查询人员信息
            } else if (ObjectUtil.isNotNull(nodeAssignee) && nodeAssignee.getBusinessRuleId() != null) {
                ActBusinessRuleVo actBusinessRuleVo = iActBusinessRuleService.queryById(nodeAssignee.getBusinessRuleId());
                List<String> ruleAssignList = WorkFlowUtils.ruleAssignList(actBusinessRuleVo, processNode.getTaskId(), processNode.getNodeName());
                processNode.setChooseWay(nodeAssignee.getChooseWay());
                processNode.setAssignee("");
                processNode.setAssigneeId(String.join(",", ruleAssignList));
                processNode.setIsShow(nodeAssignee.getIsShow());
                processNode.setBusinessRuleId(nodeAssignee.getBusinessRuleId());
                if (nodeAssignee.getMultiple()) {
                    processNode.setNodeId(nodeAssignee.getMultipleColumn());
                }
                processNode.setMultiple(nodeAssignee.getMultiple());
                processNode.setMultipleColumn(nodeAssignee.getMultipleColumn());
            } else {
                throw new ServiceException(processNode.getNodeName() + "未配置审批人，请联系管理员！");
            }
        }
        if (CollectionUtil.isNotEmpty(nodeList)) {
            // 去除不需要弹窗选人的节点
            nodeList.removeIf(node -> !node.getIsShow());
        }
        return nodeList;
    }

    /**
     * @description: 查询所有用户的已办任务
     * @param: req
     * @return: com.ruoyi.common.core.page.TableDataInfo<com.ruoyi.workflow.domain.vo.TaskFinishVo>
     * @author: gssong
     * @date: 2021/10/23
     */
    @Override
    public TableDataInfo<TaskFinishVo> getAllTaskFinishByPage(TaskBo req) {
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery()
            .finished().orderByHistoricTaskInstanceStartTime().asc();
        if (StringUtils.isNotBlank(req.getTaskName())) {
            query.taskNameLike(req.getTaskName());
        }
        List<HistoricTaskInstance> list = query.listPage(req.getPageNum(), req.getPageSize());
        long total = query.count();
        List<TaskFinishVo> taskFinishVoList = new ArrayList<>();
        for (HistoricTaskInstance hti : list) {
            TaskFinishVo taskFinishVo = new TaskFinishVo();
            BeanUtils.copyProperties(hti, taskFinishVo);
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(hti.getProcessDefinitionId()).singleResult();
            taskFinishVo.setProcessDefinitionName(processDefinition.getName());
            taskFinishVo.setProcessDefinitionKey(processDefinition.getKey());
            taskFinishVo.setVersion(processDefinition.getVersion());
            taskFinishVo.setAssigneeId(StringUtils.isNotBlank(hti.getAssignee()) ? Long.valueOf(hti.getAssignee()) : null);
            taskFinishVoList.add(taskFinishVo);
        }
        if (CollectionUtil.isNotEmpty(list)) {
            //办理人集合
            List<Long> assigneeList = taskFinishVoList.stream().map(TaskFinishVo::getAssigneeId).collect(Collectors.toList());
            if (CollectionUtil.isNotEmpty(assigneeList)) {
                List<SysUser> userList = iUserService.selectListUserByIds(assigneeList);
                if (CollectionUtil.isNotEmpty(userList)) {
                    taskFinishVoList.forEach(e -> userList.stream().filter(t -> t.getUserId().compareTo(e.getAssigneeId()) == 0).findFirst().ifPresent(u -> {
                        e.setAssignee(u.getNickName());
                        e.setAssigneeId(u.getUserId());
                    }));
                }
            }
        }
        return new TableDataInfo<>(taskFinishVoList, total);
    }

    /**
     * @description: 查询所有用户的待办任务
     * @param: req
     * @return: com.ruoyi.common.core.page.TableDataInfo<com.ruoyi.workflow.domain.vo.TaskWaitingVo>
     * @author: gssong
     * @date: 2021/10/17
     */
    @Override
    public TableDataInfo<TaskWaitingVo> getAllTaskWaitByPage(TaskBo req) {
        TaskQuery query = taskService.createTaskQuery()
            .orderByTaskCreateTime().asc();
        if (StringUtils.isNotEmpty(req.getTaskName())) {
            query.taskNameLikeIgnoreCase("%" + req.getTaskName() + "%");
        }
        if (StringUtils.isNotEmpty(req.getProcessDefinitionName())) {
            query.processDefinitionNameLike("%" + req.getProcessDefinitionName() + "%");
        }
        List<Task> taskList = query.listPage(req.getPageNum(), req.getPageSize());
        if (CollectionUtil.isEmpty(taskList)) {
            return new TableDataInfo<>();
        }
        long total = query.count();
        List<TaskWaitingVo> list = new ArrayList<>();
        //流程实例id
        Set<String> processInstanceIds = taskList.stream().map(Task::getProcessInstanceId).collect(Collectors.toSet());
        //流程定义id
        List<String> processDefinitionIds = taskList.stream().map(Task::getProcessDefinitionId).collect(Collectors.toList());
        //查询流程实例
        List<ProcessInstance> processInstanceList = runtimeService.createProcessInstanceQuery().processInstanceIds(processInstanceIds).list();
        //查询流程定义设置
        List<ActProcessDefSettingVo> processDefSettingLists = iActProcessDefSetting.getProcessDefSettingByDefIds(processDefinitionIds);
        //办理人
        List<Long> assignees = taskList.stream().filter(e -> StringUtils.isNotBlank(e.getAssignee())).map(e -> Long.valueOf(e.getAssignee())).filter(ObjectUtil::isNotEmpty).collect(Collectors.toList());
        //流程发起人
        List<Long> userIds = processInstanceList.stream().map(e -> Long.valueOf(e.getStartUserId())).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(assignees)) {
            userIds.addAll(assignees);
        }
        List<SysUser> userList = iUserService.selectListUserByIds(userIds);
        //查询任务
        List<Task> taskCollect = taskService.createTaskQuery().processInstanceIdIn(processInstanceIds).list();
        for (Task task : taskList) {
            TaskWaitingVo taskWaitingVo = new TaskWaitingVo();
            BeanUtils.copyProperties(task, taskWaitingVo);
            taskWaitingVo.setAssigneeId(StringUtils.isNotBlank(task.getAssignee()) ? Long.valueOf(task.getAssignee()) : null);
            taskWaitingVo.setSuspensionState(task.isSuspended());
            taskWaitingVo.setProcessStatus(!task.isSuspended() ? "激活" : "挂起");
            processInstanceList.stream().filter(e -> e.getProcessInstanceId().equals(task.getProcessInstanceId())).findFirst()
                .ifPresent(e -> {
                    //流程发起人
                    String startUserId = e.getStartUserId();
                    taskWaitingVo.setStartUserId(startUserId);
                    if (StringUtils.isNotBlank(startUserId)) {
                        userList.stream().filter(u -> u.getUserId().toString().equals(startUserId)).findFirst().ifPresent(u -> {
                            taskWaitingVo.setStartUserNickName(u.getNickName());
                        });
                    }
                    taskWaitingVo.setProcessDefinitionVersion(e.getProcessDefinitionVersion());
                    taskWaitingVo.setProcessDefinitionName(e.getProcessDefinitionName());
                    taskWaitingVo.setBusinessKey(e.getBusinessKey());
                });
            // 查询流程定义设置
            processDefSettingLists.stream().filter(e -> e.getProcessDefinitionId().equals(task.getProcessDefinitionId())).findFirst()
                .ifPresent(taskWaitingVo::setActProcessDefSetting);
            //是否会签
            MultiVo multiInstance = WorkFlowUtils.isMultiInstance(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
            taskWaitingVo.setMultiInstance(ObjectUtil.isNotEmpty(multiInstance));
            //查询任务
            List<Task> tasks = taskCollect.stream().filter(e -> e.getProcessInstanceId().equals(task.getProcessInstanceId())).collect(Collectors.toList());
            //可以减签的人员
            if (ObjectUtil.isNotEmpty(multiInstance)) {
                if (multiInstance.getType() instanceof ParallelMultiInstanceBehavior) {
                    taskWaitingVo.setTaskVoList(multiList((TaskEntity) task, tasks, multiInstance.getType(), null));
                } else if (multiInstance.getType() instanceof SequentialMultiInstanceBehavior && StringUtils.isNotBlank(task.getExecutionId())) {
                    List<Long> assigneeList = (List<Long>) runtimeService.getVariable(task.getExecutionId(), multiInstance.getAssigneeList());
                    taskWaitingVo.setTaskVoList(multiList((TaskEntity) task, tasks, multiInstance.getType(), assigneeList));
                }
            }
            list.add(taskWaitingVo);
        }
        if (CollectionUtil.isNotEmpty(list)) {
            //认领与归还标识
            list.forEach(e -> {
                List<IdentityLink> identityLinkList = WorkFlowUtils.getCandidateUser(e.getId());
                if (CollectionUtil.isNotEmpty(identityLinkList)) {
                    List<String> collectType = identityLinkList.stream().map(IdentityLink::getType).collect(Collectors.toList());
                    if (StringUtils.isBlank(e.getAssignee()) && collectType.size() > 1 && collectType.contains(ActConstant.CANDIDATE)) {
                        e.setIsClaim(false);
                    } else if (StringUtils.isNotBlank(e.getAssignee()) && collectType.size() > 1 && collectType.contains(ActConstant.CANDIDATE)) {
                        e.setIsClaim(true);
                    }
                }
            });
            //办理人集合
            if (CollectionUtil.isNotEmpty(userList)) {
                list.forEach(e -> userList.stream().filter(t -> StringUtils.isNotBlank(e.getAssignee()) && t.getUserId().toString().equals(e.getAssigneeId().toString()))
                    .findFirst().ifPresent(t -> {
                        e.setAssignee(t.getNickName());
                        e.setAssigneeId(t.getUserId());
                    }));
            }
            //业务id集合
            List<String> businessKeyList = list.stream().map(TaskWaitingVo::getBusinessKey).collect(Collectors.toList());
            List<ActBusinessStatus> infoList = iActBusinessStatusService.getListInfoByBusinessKey(businessKeyList);
            if (CollectionUtil.isNotEmpty(infoList)) {
                list.forEach(e -> infoList.stream().filter(t -> t.getBusinessKey().equals(e.getBusinessKey())).findFirst().ifPresent(e::setActBusinessStatus));
            }
        }
        return new TableDataInfo<>(list, total);
    }

    /**
     * @description: 驳回审批
     * @param: backProcessBo
     * @return: java.lang.String
     * @author: gssong
     * @date: 2021/11/6
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String backProcess(BackProcessBo backProcessBo) {

        Task task = taskService.createTaskQuery().taskId(backProcessBo.getTaskId()).taskAssignee(getUserId().toString()).singleResult();
        String processInstanceId = task.getProcessInstanceId();
        if (task.isSuspended()) {
            throw new ServiceException(ActConstant.MESSAGE_SUSPENDED);
        }
        if (ObjectUtil.isNull(task)) {
            throw new ServiceException(ActConstant.MESSAGE_CURRENT_TASK_IS_NULL);
        }
        try {
            //判断是否有多个任务
            List<Task> taskList = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
            List<String> gatewayNode = WorkFlowUtils.getGatewayNode(task, backProcessBo.getTargetActivityId());
            //当前单个任务驳回到并行(包含)网关
            taskService.addComment(task.getId(), processInstanceId, StringUtils.isNotBlank(backProcessBo.getComment()) ? backProcessBo.getComment() : "驳回");
            if (CollectionUtil.isNotEmpty(gatewayNode) && taskList.size() == 1) {
                runtimeService.createChangeActivityStateBuilder().processInstanceId(processInstanceId)
                    .moveSingleActivityIdToActivityIds(taskList.get(0).getTaskDefinitionKey(), gatewayNode)
                    .changeState();
                //当前多个任务驳回到单个节点
            } else if (taskList.size() > 1 && CollectionUtil.isEmpty(gatewayNode)) {
                runtimeService.createChangeActivityStateBuilder().processInstanceId(processInstanceId)
                    .moveActivityIdsToSingleActivityId(taskList.stream().map(Task::getTaskDefinitionKey).distinct().collect(Collectors.toList()), backProcessBo.getTargetActivityId())
                    .changeState();
                //当前单个节点驳回单个节点
            } else if (taskList.size() == 1 && CollectionUtil.isEmpty(gatewayNode)) {
                runtimeService.createChangeActivityStateBuilder().processInstanceId(processInstanceId)
                    .moveActivityIdTo(taskList.get(0).getTaskDefinitionKey(), backProcessBo.getTargetActivityId())
                    .changeState();
                //当前多个节点驳回到并行(包含)网关
            } else if (taskList.size() > 1 && CollectionUtil.isNotEmpty(gatewayNode)) {
                taskList.forEach(e -> {
                    if (e.getId().equals(backProcessBo.getTaskId())) {
                        runtimeService.createChangeActivityStateBuilder().processInstanceId(processInstanceId)
                            .moveSingleActivityIdToActivityIds(e.getTaskDefinitionKey(), gatewayNode)
                            .changeState();
                    } else {
                        WorkFlowUtils.deleteRuntimeTask(e);
                    }
                });
            } else {
                throw new ServiceException("驳回失败");
            }
            List<Task> otherTasks = null;
            if (taskList.size() > 1) {
                otherTasks = taskList.stream().filter(e -> !e.getId().equals(backProcessBo.getTaskId())).collect(Collectors.toList());
            }
            if (CollectionUtil.isNotEmpty(otherTasks)) {
                otherTasks.forEach(e -> historyService.deleteHistoricTaskInstance(e.getId()));
            }

            List<Task> newTaskList = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
            //处理并行会签环节重复节点
            if (CollectionUtil.isNotEmpty(newTaskList)) {
                List<Task> taskCollect = newTaskList.stream().filter(e -> e.getTaskDefinitionKey().equals(backProcessBo.getTargetActivityId())).collect(Collectors.toList());
                if (taskCollect.size() > 1) {
                    taskCollect.remove(0);
                    taskCollect.forEach(WorkFlowUtils::deleteRuntimeTask);
                }
            }
            ActTaskNode actTaskNode = iActTaskNodeService.getListByInstanceIdAndNodeId(task.getProcessInstanceId(), backProcessBo.getTargetActivityId());

            if (ObjectUtil.isNotEmpty(actTaskNode) && ActConstant.USER_TASK.equals(actTaskNode.getTaskType())) {
                List<Task> runTaskList = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
                for (Task runTask : runTaskList) {
                    //取之前的历史办理人
                    List<HistoricTaskInstance> oldTargetTaskList = historyService.createHistoricTaskInstanceQuery()
                        // 节点id
                        .taskDefinitionKey(runTask.getTaskDefinitionKey())
                        .processInstanceId(processInstanceId)
                        //已经完成才是历史
                        .finished()
                        //最新办理的在最前面
                        .orderByTaskCreateTime().desc()
                        .list();
                    if (CollectionUtil.isNotEmpty(oldTargetTaskList)) {
                        HistoricTaskInstance oldTargetTask = oldTargetTaskList.get(0);
                        taskService.setAssignee(runTask.getId(), oldTargetTask.getAssignee());
                    }

                }
            }

            //删除驳回后的流程节点
            if (ObjectUtil.isNotNull(actTaskNode) && actTaskNode.getOrderNo() == 0) {
                ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
                iActBusinessStatusService.updateState(processInstance.getBusinessKey(), BusinessStatusEnum.BACK, processInstanceId);
            }
            iActTaskNodeService.deleteBackTaskNode(processInstanceId, backProcessBo.getTargetActivityId());
            //发送站内信
            WorkFlowUtils.sendMessage(backProcessBo.getSendMessage(), processInstanceId);
            return processInstanceId;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException("驳回失败:" + e.getMessage());
        }
    }

    /**
     * @description: 获取历史任务节点，用于驳回功能
     * @param: processInstId
     * @return: java.util.List<com.ruoyi.workflow.domain.ActTaskNode>
     * @author: gssong
     * @date: 2021/11/6
     */
    @Override
    public List<ActTaskNode> getBackNodes(String processInstId) {
        return iActTaskNodeService.getListByInstanceId(processInstId).stream().filter(ActTaskNode::getIsBack).collect(Collectors.toList());
    }

    /**
     * @description: 委派任务
     * @param: taskREQ
     * @return: java.lang.Boolean
     * @author: gssong
     * @date: 2022/3/4
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delegateTask(DelegateBo delegateBo) {
        if (StringUtils.isBlank(delegateBo.getDelegateUserId())) {
            throw new ServiceException("请选择委托人");
        }
        TaskEntity task = (TaskEntity) taskService.createTaskQuery().taskId(delegateBo.getTaskId())
            .taskCandidateOrAssigned(LoginHelper.getUserId().toString()).singleResult();
        if (ObjectUtil.isEmpty(task)) {
            throw new ServiceException(ActConstant.MESSAGE_CURRENT_TASK_IS_NULL);
        }
        try {
            TaskEntity newTask = WorkFlowUtils.createNewTask(task, new Date());
            taskService.addComment(newTask.getId(), task.getProcessInstanceId(), "【" + LoginHelper.getUsername() + "】委派给【" + delegateBo.getDelegateUserName() + "】");
            //委托任务
            taskService.delegateTask(delegateBo.getTaskId(), delegateBo.getDelegateUserId());
            //办理生成的任务记录
            taskService.complete(newTask.getId());
            ActHiTaskInst actHiTaskInst = new ActHiTaskInst();
            actHiTaskInst.setId(task.getId());
            actHiTaskInst.setStartTime(new Date());
            iActHiTaskInstService.updateById(actHiTaskInst);
            //发送站内信
            WorkFlowUtils.sendMessage(delegateBo.getSendMessage(), task.getProcessInstanceId());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * @description: 转办任务
     * @param: transmitBo
     * @return: java.lang.Boolean
     * @author: gssong
     * @date: 2022/3/13 13:18
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean transmitTask(TransmitBo transmitBo) {
        Task task = taskService.createTaskQuery().taskId(transmitBo.getTaskId())
            .taskCandidateOrAssigned(LoginHelper.getUserId().toString()).singleResult();
        if (ObjectUtil.isEmpty(task)) {
            throw new ServiceException(ActConstant.MESSAGE_CURRENT_TASK_IS_NULL);
        }
        try {
            TaskEntity newTask = WorkFlowUtils.createNewTask(task, new Date());
            taskService.addComment(newTask.getId(), task.getProcessInstanceId(),
                StringUtils.isNotBlank(transmitBo.getComment()) ? transmitBo.getComment() : LoginHelper.getUsername() + "转办了任务");
            taskService.complete(newTask.getId());
            taskService.setAssignee(task.getId(), transmitBo.getTransmitUserId());
            //发送站内信
            WorkFlowUtils.sendMessage(transmitBo.getSendMessage(), task.getProcessInstanceId());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * @description: 会签任务加签
     * @param: addMultiBo
     * @return: java.lang.Boolean
     * @author: gssong
     * @date: 2022/4/15 13:06
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean addMultiInstanceExecution(AddMultiBo addMultiBo) {
        Task task;
        if (LoginHelper.isAdmin()) {
            task = taskService.createTaskQuery().taskId(addMultiBo.getTaskId()).singleResult();
        } else {
            task = taskService.createTaskQuery().taskId(addMultiBo.getTaskId())
                .taskCandidateOrAssigned(LoginHelper.getUserId().toString()).singleResult();
        }
        if (ObjectUtil.isEmpty(task) && !LoginHelper.isAdmin()) {
            throw new ServiceException(ActConstant.MESSAGE_CURRENT_TASK_IS_NULL);
        }
        if (task.isSuspended()) {
            throw new ServiceException(ActConstant.MESSAGE_SUSPENDED);
        }
        String taskDefinitionKey = task.getTaskDefinitionKey();
        String processInstanceId = task.getProcessInstanceId();
        String processDefinitionId = task.getProcessDefinitionId();
        MultiVo multiVo = WorkFlowUtils.isMultiInstance(processDefinitionId, taskDefinitionKey);
        if (ObjectUtil.isEmpty(multiVo)) {
            throw new ServiceException("当前环节不是会签节点");
        }
        try {
            if (multiVo.getType() instanceof ParallelMultiInstanceBehavior) {
                for (Long assignee : addMultiBo.getAssignees()) {
                    runtimeService.addMultiInstanceExecution(taskDefinitionKey, processInstanceId, Collections.singletonMap(multiVo.getAssignee(), assignee));
                }
            } else if (multiVo.getType() instanceof SequentialMultiInstanceBehavior) {
                AddSequenceMultiInstanceCmd addSequenceMultiInstanceCmd = new AddSequenceMultiInstanceCmd(task.getExecutionId(), multiVo.getAssigneeList(), addMultiBo.getAssignees());
                managementService.executeCommand(addSequenceMultiInstanceCmd);
            }
            List<String> assigneeNames = addMultiBo.getAssigneeNames();
            String username = LoginHelper.getUsername();
            TaskEntity newTask = WorkFlowUtils.createNewTask(task, new Date());
            taskService.addComment(newTask.getId(), processInstanceId, username + "加签【" + String.join(",", assigneeNames) + "】");
            taskService.complete(newTask.getId());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * @description: 会签任务减签
     * @param: deleteMultiBo
     * @return: java.lang.Boolean
     * @author: gssong
     * @date: 2022/4/16 10:59
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteMultiInstanceExecution(DeleteMultiBo deleteMultiBo) {
        Task task;
        if (LoginHelper.isAdmin()) {
            task = taskService.createTaskQuery().taskId(deleteMultiBo.getTaskId()).singleResult();
        } else {
            task = taskService.createTaskQuery().taskId(deleteMultiBo.getTaskId())
                .taskCandidateOrAssigned(LoginHelper.getUserId().toString()).singleResult();
        }
        if (ObjectUtil.isEmpty(task) && !LoginHelper.isAdmin()) {
            throw new ServiceException(ActConstant.MESSAGE_CURRENT_TASK_IS_NULL);
        }
        if (task.isSuspended()) {
            throw new ServiceException(ActConstant.MESSAGE_SUSPENDED);
        }
        String taskDefinitionKey = task.getTaskDefinitionKey();
        String processInstanceId = task.getProcessInstanceId();
        String processDefinitionId = task.getProcessDefinitionId();
        MultiVo multiVo = WorkFlowUtils.isMultiInstance(processDefinitionId, taskDefinitionKey);
        if (ObjectUtil.isEmpty(multiVo)) {
            throw new ServiceException("当前环节不是会签节点");
        }
        try {
            if (multiVo.getType() instanceof ParallelMultiInstanceBehavior) {
                for (String executionId : deleteMultiBo.getExecutionIds()) {
                    runtimeService.deleteMultiInstanceExecution(executionId, false);
                }
                for (String taskId : deleteMultiBo.getTaskIds()) {
                    historyService.deleteHistoricTaskInstance(taskId);
                }
            } else if (multiVo.getType() instanceof SequentialMultiInstanceBehavior) {
                DeleteSequenceMultiInstanceCmd deleteSequenceMultiInstanceCmd = new DeleteSequenceMultiInstanceCmd(task.getAssignee(), task.getExecutionId(), multiVo.getAssigneeList(), deleteMultiBo.getAssigneeIds());
                managementService.executeCommand(deleteSequenceMultiInstanceCmd);
            }
            List<String> assigneeNames = deleteMultiBo.getAssigneeNames();
            String username = LoginHelper.getUsername();
            TaskEntity newTask = WorkFlowUtils.createNewTask(task, new Date());
            taskService.addComment(newTask.getId(), processInstanceId, username + "减签【" + String.join(",", assigneeNames) + "】");
            taskService.complete(newTask.getId());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * @description: 修改办理人
     * @param: updateAssigneeBo
     * @return: java.lang.Boolean
     * @author: gssong
     * @date: 2022/7/17 13:35
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateAssignee(UpdateAssigneeBo updateAssigneeBo) {
        List<Task> list = taskService.createNativeTaskQuery().sql("select * from act_ru_task where id_ in " + getInParam(updateAssigneeBo.getTaskIdList())).list();
        if (CollectionUtil.isEmpty(list)) {
            throw new ServiceException("办理失败，任务不存在");
        }
        try {
            for (Task task : list) {
                taskService.setAssignee(task.getId(), updateAssigneeBo.getUserId());
            }
            return true;
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * @description: 拼接单引号, 到数据库后台用in查询.
     * @param: param
     * @return: java.lang.String
     * @author: gssong
     * @date: 2022/7/22 12:17
     */
    private String getInParam(List<String> param) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < param.size(); i++) {
            sb.append("'").append(param.get(i)).append("'");
            if (i != param.size() - 1) {
                sb.append(",");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * @description: 查询流程变量
     * @param: taskId
     * @return: java.util.List<com.ruoyi.workflow.domain.vo.VariableVo>
     * @author: gssong
     * @date: 2022/7/23 14:33
     */
    @Override
    public List<VariableVo> getProcessInstVariable(String taskId) {
        List<VariableVo> variableVoList = new ArrayList<>();
        Map<String, VariableInstance> variableInstances = taskService.getVariableInstances(taskId);
        if (CollectionUtil.isNotEmpty(variableInstances)) {
            for (Map.Entry<String, VariableInstance> entry : variableInstances.entrySet()) {
                VariableVo variableVo = new VariableVo();
                variableVo.setKey(entry.getKey());
                variableVo.setValue(entry.getValue().getValue().toString());
                variableVoList.add(variableVo);
            }
        }
        return variableVoList;
    }

    /**
     * @description: 修改审批意见
     * @param: commentId
     * @param: comment
     * @return: java.lang.Boolean
     * @author: gssong
     * @date: 2022/7/24 13:28
     */
    @Override
    public Boolean editComment(String commentId, String comment) {
        return taskMapper.editComment(commentId, comment) > 0;
    }

    /**
     * @description: 修改附件
     * @param: fileList
     * @param: taskId
     * @param: processInstanceId
     * @return: java.lang.Boolean
     * @author: gssong
     * @date: 2022/9/26 13:01
     */
    @Override
    public Boolean editAttachment(MultipartFile[] fileList, String taskId, String processInstanceId) {
        AttachmentCmd attachmentCmd = new AttachmentCmd(fileList, taskId, processInstanceId);
        return managementService.executeCommand(attachmentCmd);
    }

    /**
     * @description: 删除附件
     * @param: attachmentId
     * @return: java.lang.Boolean
     * @author: gssong
     * @date: 2022/9/26 13:11
     */
    @Override
    public Boolean deleteAttachment(String attachmentId) {
        try {
            taskService.deleteAttachment(attachmentId);
            return true;
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * @description: 终止任务
     * @param: taskBo
     * @return: java.lang.Boolean
     * @author: gssong
     * @date: 2022/10/27 20:32
     */
    @Override
    public Boolean terminationTask(TaskBo taskBo) {
        try {
            Task task = taskService.createTaskQuery().taskId(taskBo.getTaskId()).singleResult();
            if (ObjectUtil.isEmpty(task)) {
                throw new ServiceException("当前任务不存在");
            }
            ActBusinessStatus actBusinessStatus = iActBusinessStatusService.getInfoByProcessInstId(task.getProcessInstanceId());
            if (actBusinessStatus == null) {
                throw new ServiceException("当前流程异常，未生成act_business_status对象");
            }
            if (StringUtils.isBlank(taskBo.getComment())) {
                taskBo.setComment(LoginHelper.getUsername() + "终止了申请");
            } else {
                taskBo.setComment(LoginHelper.getUsername() + "终止了申请：" + taskBo.getComment());
            }
            taskService.addComment(task.getId(), task.getProcessInstanceId(), taskBo.getComment());
            List<Task> list = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).list();
            if (CollectionUtil.isNotEmpty(list)) {
                List<Task> subTasks = list.stream().filter(e -> StringUtils.isNotBlank(e.getParentTaskId())).collect(Collectors.toList());
                if (CollectionUtil.isNotEmpty(subTasks)) {
                    subTasks.forEach(e -> taskService.deleteTask(e.getId()));
                }
                runtimeService.deleteProcessInstance(task.getProcessInstanceId(), "");
            }
            return iActBusinessStatusService.updateState(actBusinessStatus.getBusinessKey(), BusinessStatusEnum.TERMINATION, task.getProcessInstanceId());
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }
}
