package com.ruoyi.workflow.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.workflow.common.constant.ActConstant;
import com.ruoyi.workflow.domain.ActNodeAssignee;
import com.ruoyi.workflow.domain.vo.ActProcessNodeVo;
import com.ruoyi.workflow.domain.vo.FieldList;
import com.ruoyi.workflow.domain.vo.MultiVo;
import com.ruoyi.workflow.domain.vo.TaskListenerVo;
import com.ruoyi.workflow.mapper.ActNodeAssigneeMapper;
import com.ruoyi.workflow.service.IActNodeAssigneeService;
import com.ruoyi.workflow.utils.WorkFlowUtils;
import lombok.RequiredArgsConstructor;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * @description: 流程节点人员设置业务层
 * @author: gssong
 * @date: 2021/11/21
 */
@Service
@RequiredArgsConstructor
public class ActNodeAssigneeServiceImpl extends ServiceImpl<ActNodeAssigneeMapper, ActNodeAssignee> implements IActNodeAssigneeService {

    private final RepositoryService repositoryService;

    /**
     * @description: 保存流程节点人员设置
     * @param: actNodeAssignee
     * @return: com.ruoyi.workflow.domain.ActNodeAssignee
     * @author: gssong
     * @date: 2021/11/21
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActNodeAssignee add(ActNodeAssignee actNodeAssignee) {
        RedisUtils.deleteObject(ActConstant.CACHE_ACT_NODE_ASSIGNEE_KEY + actNodeAssignee.getProcessDefinitionId() + "-" + actNodeAssignee.getNodeId());

        if (actNodeAssignee.getIndex() == 1 && StringUtils.isBlank(actNodeAssignee.getChooseWay())) {
            throw new ServiceException("请选择选人方式");
        }

        if (!ActConstant.WORKFLOW_RULE.equals(actNodeAssignee.getChooseWay())) {
            actNodeAssignee.setBusinessRuleId(null);
        }

        if (StringUtils.isNotBlank(actNodeAssignee.getId())) {
            del(actNodeAssignee.getId());
        }

        //判断是否会签
        if (!actNodeAssignee.getMultiple()) {
            actNodeAssignee.setAddMultiInstance(false);
            actNodeAssignee.setDeleteMultiInstance(false);
            actNodeAssignee.setMultipleColumn("");
        }
        if (CollectionUtil.isNotEmpty(actNodeAssignee.getTaskListenerList())) {
            List<TaskListenerVo> taskListenerList = new ArrayList<>();
            actNodeAssignee.getTaskListenerList().forEach(e -> {
                if (StringUtils.isNotBlank(e.getEventType()) && StringUtils.isNotBlank(e.getBeanName())) {
                    taskListenerList.add(e);
                }
            });
            if (CollectionUtil.isNotEmpty(taskListenerList)) {
                String jsonString = JsonUtils.toJsonString(taskListenerList);
                actNodeAssignee.setTaskListener(jsonString);
            }
        }
        actNodeAssignee.setFieldListJson("");
        if (CollectionUtil.isNotEmpty(actNodeAssignee.getFieldList())) {
            List<FieldList> fieldListVoList = new ArrayList<>();
            actNodeAssignee.getFieldList().forEach(e -> {
                if (StringUtils.isNotBlank(e.getField()) && e.getEdit() != null && e.getRequired() != null) {
                    if (!e.getRequired()) {
                        e.setMessage("");
                    }
                    fieldListVoList.add(e);
                }
            });
            if (CollectionUtil.isNotEmpty(fieldListVoList)) {
                String jsonString = JsonUtils.toJsonString(fieldListVoList);
                actNodeAssignee.setFieldListJson(jsonString);
            }
        }
        baseMapper.insert(actNodeAssignee);
        return actNodeAssignee;
    }

    /**
     * @description: 按照流程定义id和流程节点id查询流程节点人员设置
     * @param: actNodeAssignee
     * @return: com.ruoyi.workflow.domain.ActNodeAssignee
     * @author: gssong
     * @date: 2021/11/21
     */
    @Override
    public ActNodeAssignee getInfo(String processDefinitionId, String nodeId) {
        LambdaQueryWrapper<ActNodeAssignee> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ActNodeAssignee::getProcessDefinitionId, processDefinitionId);
        wrapper.eq(ActNodeAssignee::getNodeId, nodeId);
        return baseMapper.selectOne(wrapper);
    }

    /**
     * @description: 按照流程定义id和流程节点id查询流程节点人员设置
     * @param: actNodeAssignee
     * @return: com.ruoyi.workflow.domain.ActNodeAssignee
     * @author: gssong
     * @date: 2022/6/10 23:22
     */
    @Override
    public ActNodeAssignee getInfoSetting(String processDefinitionId, String nodeId) {
        ActNodeAssignee cacheActNodeAssignee = RedisUtils.getCacheObject(ActConstant.CACHE_ACT_NODE_ASSIGNEE_KEY + processDefinitionId + "-" + nodeId);
        if (cacheActNodeAssignee != null) {
            return cacheActNodeAssignee;
        }
        LambdaQueryWrapper<ActNodeAssignee> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ActNodeAssignee::getProcessDefinitionId, processDefinitionId);
        wrapper.eq(ActNodeAssignee::getNodeId, nodeId);
        ActNodeAssignee nodeAssignee = baseMapper.selectOne(wrapper);

        if (ObjectUtil.isEmpty(nodeAssignee)) {
            nodeAssignee = new ActNodeAssignee();
            nodeAssignee.setProcessDefinitionId(processDefinitionId);
            nodeAssignee.setNodeId(nodeId);
            nodeAssignee.setChooseWay(ActConstant.WORKFLOW_PERSON);
            nodeAssignee.setIsBack(false);
            nodeAssignee.setIsShow(false);
            nodeAssignee.setIsTransmit(false);
            nodeAssignee.setIsCopy(false);
            nodeAssignee.setIsDelegate(false);
            nodeAssignee.setMultiple(false);
            nodeAssignee.setAddMultiInstance(false);
            nodeAssignee.setDeleteMultiInstance(false);
        }
        MultiVo multiInstance = WorkFlowUtils.isMultiInstance(processDefinitionId, nodeId);
        if (ObjectUtil.isNotEmpty(multiInstance)) {
            nodeAssignee.setMultiple(true);
            if (StringUtils.isBlank(multiInstance.getAssigneeList())) {
                nodeAssignee.setMultipleColumn("获取会签集合变量为空，请检查流程");
            } else {
                nodeAssignee.setMultipleColumn(multiInstance.getAssigneeList());
            }
        } else {
            nodeAssignee.setMultiple(false);
            nodeAssignee.setMultipleColumn("");
            nodeAssignee.setAddMultiInstance(false);
            nodeAssignee.setDeleteMultiInstance(false);
        }
        if (ObjectUtil.isNotEmpty(nodeAssignee) && StringUtils.isNotBlank(nodeAssignee.getTaskListener())) {
            nodeAssignee.setTaskListenerList(JsonUtils.parseArray(nodeAssignee.getTaskListener(), TaskListenerVo.class));
        } else {
            nodeAssignee.setTaskListenerList(new ArrayList<>());
        }
        if (ObjectUtil.isNotEmpty(nodeAssignee) && StringUtils.isNotBlank(nodeAssignee.getFieldListJson())) {
            nodeAssignee.setFieldList(JsonUtils.parseArray(nodeAssignee.getFieldListJson(), FieldList.class));
        } else {
            nodeAssignee.setFieldList(new ArrayList<>());
        }
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        List<Process> processes = bpmnModel.getProcesses();
        Collection<FlowElement> elements = processes.get(0).getFlowElements();

        for (FlowElement element : elements) {
            if (element instanceof StartEvent) {
                List<SequenceFlow> outgoingFlows = ((StartEvent) element).getOutgoingFlows();
                for (SequenceFlow outgoingFlow : outgoingFlows) {
                    FlowElement flowElement = outgoingFlow.getTargetFlowElement();
                    if (flowElement instanceof UserTask && flowElement.getId().equals(nodeId)) {
                        nodeAssignee.setIndex(0);
                    }
                }
            }
            if (element instanceof UserTask && nodeId.equals(element.getId())) {
                List<SequenceFlow> outgoingFlows = ((FlowNode) element).getOutgoingFlows();
                for (SequenceFlow outgoingFlow : outgoingFlows) {
                    FlowElement flowElement = outgoingFlow.getTargetFlowElement();
                    if (flowElement instanceof EndEvent) {
                        nodeAssignee.setEnd(true);
                    }
                }
            }
            if (element instanceof SubProcess) {
                List<SequenceFlow> outgoingFlows = ((FlowNode) element).getOutgoingFlows();
                for (SequenceFlow outgoingFlow : outgoingFlows) {
                    FlowElement flowElement = outgoingFlow.getTargetFlowElement();
                    if (flowElement instanceof EndEvent) {
                        nodeAssignee.setEnd(true);
                    }
                }
            }
        }
        setCache(processDefinitionId, nodeId, nodeAssignee);
        return nodeAssignee;
    }

    /**
     * 添加缓存
     * @param processDefinitionId
     * @param nodeId
     * @param nodeAssignee
     */
    private void setCache(String processDefinitionId, String nodeId, ActNodeAssignee nodeAssignee) {
        RedisUtils.setCacheObject(ActConstant.CACHE_ACT_NODE_ASSIGNEE_KEY + processDefinitionId + "-" + nodeId,
            nodeAssignee, Duration.ofMinutes(ActConstant.CACHE_EXPIRATION));
    }

    /**
     * @description: 按照流程定义id查询
     * @param: processDefinitionId
     * @return: java.util.List<com.ruoyi.workflow.domain.ActNodeAssignee>
     * @author: gssong
     * @date: 2021/11/21
     */
    @Override
    public List<ActNodeAssignee> getInfoByProcessDefinitionId(String processDefinitionId) {
        LambdaQueryWrapper<ActNodeAssignee> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ActNodeAssignee::getProcessDefinitionId, processDefinitionId);
        return this.baseMapper.selectList(wrapper);
    }

    /**
     * @description: 删除流程节点人员设置
     * @param: id
     * @return: java.lang.Boolean
     * @author: gssong
     * @date: 2021/11/21
     */
    @Override
    public Boolean del(String id) {
        int i = baseMapper.deleteById(id);
        return i == 1;
    }

    /**
     * @description: 按照流程定义id删除
     * @param: definitionId
     * @return: java.lang.Boolean
     * @author: gssong
     * @date: 2021/11/21
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delByDefinitionId(String definitionId) {
        LambdaQueryWrapper<ActNodeAssignee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ActNodeAssignee::getProcessDefinitionId, definitionId);
        List<ActNodeAssignee> list = baseMapper.selectList(queryWrapper);
        int i = baseMapper.delete(queryWrapper);
        if (list.size() != i) {
            throw new ServiceException("删除失败");
        }
        return true;
    }

    /**
     * @description: 按照流程定义id与节点id删除
     * @param: definitionId
     * @param: nodeId
     * @return: java.lang.Boolean
     * @author: gssong
     * @date: 2021/11/21
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delByDefinitionIdAndNodeId(String definitionId, String nodeId) {
        LambdaQueryWrapper<ActNodeAssignee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ActNodeAssignee::getProcessDefinitionId, definitionId);
        queryWrapper.eq(ActNodeAssignee::getNodeId, nodeId);
        List<ActNodeAssignee> list = baseMapper.selectList(queryWrapper);
        int i = baseMapper.delete(queryWrapper);
        if (list.size() != i) {
            throw new ServiceException("删除失败");
        }
        return true;
    }

    /**
     * @description: 复制给最新流程节点人员设置
     * @param: id
     * @param: key 流程定义key
     * @return: java.lang.Boolean
     * @author: gssong
     * @date: 2021/11/21
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean copy(String id, String key) {
        LambdaQueryWrapper<ActNodeAssignee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActNodeAssignee::getProcessDefinitionId, id);
        List<ActNodeAssignee> oldNodeAssigneeList = baseMapper.selectList(wrapper);

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey(key).latestVersion().singleResult();
        if (ObjectUtil.isEmpty(processDefinition)) {
            throw new ServiceException("流程定义不存在");
        }
        List<ActProcessNodeVo> processNodeVoList = new ArrayList<>();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        List<Process> processes = bpmnModel.getProcesses();
        Collection<FlowElement> elements = processes.get(0).getFlowElements();
        for (FlowElement element : elements) {
            if (element instanceof UserTask) {
                ActProcessNodeVo actProcessNodeVo = new ActProcessNodeVo();
                actProcessNodeVo.setNodeId(element.getId());
                actProcessNodeVo.setNodeName(element.getName());
                actProcessNodeVo.setProcessDefinitionId(processDefinition.getId());
                processNodeVoList.add(actProcessNodeVo);
            } else if (element instanceof SubProcess) {
                Collection<FlowElement> flowElements = ((SubProcess) element).getFlowElements();
                for (FlowElement flowElement : flowElements) {
                    if (flowElement instanceof UserTask) {
                        ActProcessNodeVo actProcessNodeVo = new ActProcessNodeVo();
                        actProcessNodeVo.setNodeId(flowElement.getId());
                        actProcessNodeVo.setNodeName(flowElement.getName());
                        actProcessNodeVo.setProcessDefinitionId(processDefinition.getId());
                        processNodeVoList.add(actProcessNodeVo);
                    }
                }
            }
        }
        delByDefinitionId(processDefinition.getId());
        List<ActNodeAssignee> actNodeAssigneeList = new ArrayList<>();
        for (ActNodeAssignee oldNodeAssignee : oldNodeAssigneeList) {
            ActProcessNodeVo actProcessNodeVo = processNodeVoList.stream().filter(e -> e.getNodeId().equals(oldNodeAssignee.getNodeId())).findFirst().orElse(null);
            if (ObjectUtil.isNotEmpty(actProcessNodeVo)) {

                ActNodeAssignee actNodeAssignee = new ActNodeAssignee();
                BeanUtils.copyProperties(oldNodeAssignee, actNodeAssignee);
                actNodeAssignee.setId("");
                actNodeAssignee.setProcessDefinitionId(processDefinition.getId());
                if (!actNodeAssignee.getMultiple()) {
                    actNodeAssignee.setMultipleColumn("");
                }
                actNodeAssigneeList.add(actNodeAssignee);
            }
        }
        if (CollectionUtil.isNotEmpty(actNodeAssigneeList)) {
            actNodeAssigneeList.forEach(e->{
                RedisUtils.deleteObject(ActConstant.CACHE_ACT_NODE_ASSIGNEE_KEY + e.getProcessDefinitionId() + "-" + e.getNodeId());
            });
            return saveBatch(actNodeAssigneeList);
        }
        return false;
    }
}
