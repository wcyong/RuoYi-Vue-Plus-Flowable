package com.ruoyi.workflow.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.workflow.common.constant.ActConstant;
import com.ruoyi.workflow.common.enums.BusinessStatusEnum;
import com.ruoyi.workflow.domain.ActNodeAssignee;
import com.ruoyi.workflow.domain.ActProcessNodeAssignee;
import com.ruoyi.workflow.domain.ActTaskNode;
import com.ruoyi.workflow.domain.bo.TaskCompleteBo;
import com.ruoyi.workflow.domain.vo.ProcessNodePath;
import com.ruoyi.workflow.domain.vo.TaskListenerVo;
import com.ruoyi.workflow.service.IActBusinessStatusService;
import com.ruoyi.workflow.service.IActNodeAssigneeService;
import com.ruoyi.workflow.service.IActProcessNodeAssigneeService;
import com.ruoyi.workflow.service.IActTaskNodeService;
import com.ruoyi.workflow.service.impl.ActBusinessStatusServiceImpl;
import com.ruoyi.workflow.service.impl.ActNodeAssigneeServiceImpl;
import com.ruoyi.workflow.service.impl.ActProcessNodeAssigneeServiceImpl;
import com.ruoyi.workflow.service.impl.ActTaskNodeServiceImpl;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.ruoyi.common.helper.LoginHelper.getUserId;

/**
 * @description: 办理任务(申请人选择全部办理人后续审批人不再选择审批人)
 * @author: gssong
 * @date: 2022/12/20
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CompleteTaskUtils {

    private static final TaskService taskService = SpringUtils.getBean(TaskService.class);
    private static final RuntimeService runtimeService = SpringUtils.getBean(RuntimeService.class);
    private static final IActProcessNodeAssigneeService iActProcessNodeAssigneeService = SpringUtils.getBean(ActProcessNodeAssigneeServiceImpl.class);
    private static final IActTaskNodeService iActTaskNodeService = SpringUtils.getBean(ActTaskNodeServiceImpl.class);
    private static final IActNodeAssigneeService iActNodeAssigneeService = SpringUtils.getBean(ActNodeAssigneeServiceImpl.class);
    private static final IActBusinessStatusService iActBusinessStatusService = SpringUtils.getBean(ActBusinessStatusServiceImpl.class);

    /**
     * @description: 办理任务
     * @param: taskCompleteBo
     * @return: java.lang.Boolean
     * @author: gssong
     * @date: 2022/12/20
     */
    public static Boolean execute(TaskCompleteBo taskCompleteBo) {

        // 1.查询任务
        Task task = taskService.createTaskQuery().taskId(taskCompleteBo.getTaskId()).taskAssignee(getUserId().toString()).singleResult();
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();

        if (CollUtil.isNotEmpty(taskCompleteBo.getProcessNodeList()) && taskCompleteBo.getDefaultProcess()) {
            iActProcessNodeAssigneeService.deleteByProcessInstanceId(task.getProcessInstanceId());
            List<ActProcessNodeAssignee> processNodeAssigneeList = new ArrayList<>();
            for (ProcessNodePath processNodePath : taskCompleteBo.getProcessNodeList()) {
                ActProcessNodeAssignee actProcessNodeAssignee = new ActProcessNodeAssignee();
                actProcessNodeAssignee.setProcessDefinitionId(task.getProcessDefinitionId());
                actProcessNodeAssignee.setProcessInstanceId(task.getProcessInstanceId());
                actProcessNodeAssignee.setNodeId(processNodePath.getNodeId());
                actProcessNodeAssignee.setAssigneeId(processNodePath.getTransactorId());
                actProcessNodeAssignee.setAssignee(processNodePath.getTransactor());
                actProcessNodeAssignee.setNodeName(processNodePath.getNodeName());
                actProcessNodeAssignee.setMultiple(processNodePath.getMultiple());
                if (processNodePath.getMultiple()) {
                    actProcessNodeAssignee.setMultipleColumn(processNodePath.getMultipleColumn());
                }
                processNodeAssigneeList.add(actProcessNodeAssignee);
            }
            String nodeNames = processNodeAssigneeList.stream().filter(e -> StringUtils.isBlank(e.getAssigneeId())).map(ActProcessNodeAssignee::getNodeName).collect(Collectors.joining(","));
            if (StringUtils.isNotBlank(nodeNames)) {
                throw new ServiceException(nodeNames+"审批环节未设置审批人");
            }
            iActProcessNodeAssigneeService.insertBatch(processNodeAssigneeList);
        }
        List<ActTaskNode> taskNodes = iActTaskNodeService.getListByInstanceId(task.getProcessInstanceId());
        if (CollUtil.isNotEmpty(taskCompleteBo.getProcessNodeList()) && CollUtil.isEmpty(taskNodes)) {
            List<ActProcessNodeAssignee> processNodeAssignees = iActProcessNodeAssigneeService.queryByProcessInstanceId(task.getProcessInstanceId());
            List<ActProcessNodeAssignee> assigneeList = processNodeAssignees.stream().filter(ActProcessNodeAssignee::getMultiple).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(assigneeList)) {
                assigneeList.forEach(e -> {
                    taskService.setVariable(task.getId(), e.getMultipleColumn(), Arrays.asList(e.getAssigneeId().split(",")));
                });
            }
            taskService.setAssignee(task.getId(), LoginHelper.getUserId().toString());
            taskService.addComment(taskCompleteBo.getTaskId(), task.getProcessInstanceId(), taskCompleteBo.getMessage());
            taskService.complete(task.getId());
            List<ActNodeAssignee> actNodeAssignees = iActNodeAssigneeService.getInfoByProcessDefinitionId(task.getProcessDefinitionId());
            WorkFlowUtils.recordExecuteNode(task, actNodeAssignees);
            iActBusinessStatusService.updateState(processInstance.getBusinessKey(), BusinessStatusEnum.WAITING, task.getProcessInstanceId());

            List<Task> taskList = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).list();
            if (CollUtil.isNotEmpty(taskList)) {
                setAssignee(processNodeAssignees, taskList);
            }
            WorkFlowUtils.autoComplete(task.getProcessInstanceId(), processInstance.getBusinessKey(), processNodeAssignees, actNodeAssignees);
        } else {
            List<ActNodeAssignee> actNodeAssignees = iActNodeAssigneeService.getInfoByProcessDefinitionId(task.getProcessDefinitionId());
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
            taskService.complete(task.getId());
            WorkFlowUtils.recordExecuteNode(task, actNodeAssignees);
            List<ActProcessNodeAssignee> processNodeAssignees = iActProcessNodeAssigneeService.queryByProcessInstanceId(task.getProcessInstanceId());
            List<Task> taskList = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).list();
            if (CollUtil.isNotEmpty(taskList)) {
                setAssignee(processNodeAssignees, taskList);
                WorkFlowUtils.autoComplete(task.getProcessInstanceId(), processInstance.getBusinessKey(), processNodeAssignees, actNodeAssignees);
            } else {
                iActBusinessStatusService.updateState(processInstance.getBusinessKey(), BusinessStatusEnum.FINISH, task.getProcessInstanceId());
            }
            // 任务后执行
            if (CollectionUtil.isNotEmpty(handleAfterList)) {
                for (TaskListenerVo taskListenerVo : handleAfterList) {
                    WorkFlowUtils.springInvokeMethod(taskListenerVo.getBeanName(), ActConstant.HANDLE_PROCESS
                        , task.getProcessInstanceId());
                }
            }
        }
        return true;
    }

    /**
     * @description: 设置人员
     * @param: processNodeAssignees
     * @param: taskList
     * @return: void
     * @author: gssong
     * @date: 2022/12/20
     */
    private static void setAssignee(List<ActProcessNodeAssignee> processNodeAssignees, List<Task> taskList) {
        for (Task t : taskList) {
            processNodeAssignees.stream().filter(e -> e.getNodeId().equals(t.getTaskDefinitionKey()) && !e.getMultiple()).findFirst()
                .ifPresent(e -> {
                    String[] userIds = e.getAssigneeId().split(",");
                    if (userIds.length == 1) {
                        taskService.setAssignee(t.getId(), userIds[0]);
                    } else {
                        for (String userId : userIds) {
                            taskService.addCandidateUser(t.getId(), userId);
                        }
                    }
                });
        }
    }
}
