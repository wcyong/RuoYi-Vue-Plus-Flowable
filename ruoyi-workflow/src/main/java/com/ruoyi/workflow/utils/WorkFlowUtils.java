package com.ruoyi.workflow.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.common.utils.StreamUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.workflow.domain.*;
import com.ruoyi.workflow.domain.bo.SendMessage;
import com.ruoyi.workflow.domain.bo.TaskCompleteBo;
import com.ruoyi.workflow.domain.vo.FieldList;
import com.ruoyi.workflow.domain.vo.MultiVo;
import com.ruoyi.workflow.flowable.cmd.*;
import com.ruoyi.workflow.common.constant.FlowConstant;
import com.ruoyi.workflow.common.enums.BusinessStatusEnum;
import com.ruoyi.workflow.domain.vo.ActBusinessRuleVo;
import com.ruoyi.workflow.domain.vo.ProcessNode;
import com.ruoyi.workflow.service.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.*;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;
import org.flowable.engine.*;
import org.flowable.engine.impl.bpmn.behavior.ParallelMultiInstanceBehavior;
import org.flowable.engine.impl.bpmn.behavior.SequentialMultiInstanceBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.springframework.util.ReflectionUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.rmi.ServerException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ruoyi.workflow.common.constant.FlowConstant.*;

/**
 * @description: 工作流工具栏
 * @author: gssong
 * @date: 2021/10/03 19:31
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkFlowUtils {

    private static final IActBusinessStatusService iActBusinessStatusService = SpringUtils.getBean(IActBusinessStatusService.class);

    private static final IUserService iUserService = SpringUtils.getBean(IUserService.class);

    private static final ProcessEngine PROCESS_ENGINE = SpringUtils.getBean(ProcessEngine.class);

    private static final ISysMessageService iSysMessageService = SpringUtils.getBean(ISysMessageService.class);

    private static final IActBusinessRuleService iActBusinessRuleService = SpringUtils.getBean(IActBusinessRuleService.class);

    private static final IActTaskNodeService iActTaskNodeService = SpringUtils.getBean(IActTaskNodeService.class);

    private static final IActNodeAssigneeService iActNodeAssigneeService = SpringUtils.getBean(IActNodeAssigneeService.class);

    /**
     * @description: bpmnModel转为xml
     * @param: jsonBytes
     * @return: byte[]
     * @author: gssong
     * @date: 2021/11/5
     */
    public static byte[] bpmnJsonToXmlBytes(byte[] jsonBytes) throws IOException {
        if (jsonBytes == null) {
            return new byte[0];
        }
        //1. json字节码转成 BpmnModel 对象
        ObjectMapper objectMapper = JsonUtils.getObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonBytes);
        BpmnModel bpmnModel = new BpmnJsonConverter().convertToBpmnModel(jsonNode);

        if (bpmnModel.getProcesses().isEmpty()) {
            return new byte[0];
        }
        //2.将bpmnModel转为xml
        return new BpmnXMLConverter().convertToXML(bpmnModel);
    }

    /**
     * @description: xml转为bpmnModel
     * @param: xml
     * @return: org.flowable.bpmn.model.BpmnModel
     * @author: gssong
     * @date: 2022/10/30 18:25
     */
    public static BpmnModel xmlToBpmnModel(String xml) throws IOException {
        if (xml == null) {
            throw new ServerException("xml不能为空");
        }
        InputStream inputStream = new ByteArrayInputStream(StrUtil.utf8Bytes(xml));
        XMLInputFactory factory = XMLInputFactory.newInstance();
        try {
            XMLStreamReader reader = factory.createXMLStreamReader(inputStream);
            return new BpmnXMLConverter().convertToBpmnModel(reader);
        } catch (XMLStreamException e) {
            throw new ServerException(e.getMessage());
        }
    }

    /**
     * @description: 校验模型会签变量
     * @param: bpmnModel
     * @return: void
     * @author: gssong
     * @date: 2022/10/30 18:27
     */
    public static void checkBpmnModelMultiVariable(BpmnModel bpmnModel) throws ServerException {
        Collection<FlowElement> flowElements = bpmnModel.getMainProcess().getFlowElements();
        List<MultiVo> multiVoList = new ArrayList<>();
        for (FlowElement flowElement : flowElements) {
            if (flowElement instanceof UserTask && ObjectUtil.isNotEmpty(((UserTask) flowElement).getLoopCharacteristics()) && StringUtils.isNotBlank(((UserTask) flowElement).getLoopCharacteristics().getInputDataItem())) {
                MultiVo multiVo = new MultiVo();
                multiVo.setAssigneeList(((UserTask) flowElement).getLoopCharacteristics().getInputDataItem());
                multiVoList.add(multiVo);
            }
        }
        if (CollectionUtil.isNotEmpty(multiVoList) && multiVoList.size() > 1) {
            Map<String, List<MultiVo>> assigneeListGroup = StreamUtils.groupByKey(multiVoList, MultiVo::getAssigneeList);
            for (Map.Entry<String, List<MultiVo>> entry : assigneeListGroup.entrySet()) {
                List<MultiVo> value = entry.getValue();
                if (CollectionUtil.isNotEmpty(value) && value.size() > 1) {
                    String key = entry.getKey();
                    throw new ServerException("会签人员集合【" + key + "】重复,请重新设置集合KEY");
                }
            }
        }
    }

    /**
     * @description: 获取下一审批节点信息
     * @param: flowElements 全部节点
     * @param: flowElement 当前节点信息
     * @param: nextNodes 下一节点信息
     * @param: tempNodes 保存没有表达式的节点信息
     * @param: taskId 任务id
     * @param: gateway 网关
     * @return: void
     * @author: gssong
     * @date: 2022/4/11 13:37
     */
    public static void getNextNodeList(Collection<FlowElement> flowElements, FlowElement flowElement, ExecutionEntityImpl executionEntity, List<ProcessNode> nextNodes, List<ProcessNode> tempNodes, String taskId, String gateway) {
        // 获取当前节点的连线信息
        List<SequenceFlow> outgoingFlows = ((FlowNode) flowElement).getOutgoingFlows();
        // 当前节点的所有下一节点出口
        for (SequenceFlow sequenceFlow : outgoingFlows) {
            // 下一节点的目标元素
            ProcessNode processNode = new ProcessNode();
            ProcessNode tempNode = new ProcessNode();
            FlowElement outFlowElement = sequenceFlow.getTargetFlowElement();
            if (outFlowElement instanceof UserTask) {
                nextNodeBuild(executionEntity, nextNodes, tempNodes, taskId, gateway, sequenceFlow, processNode, tempNode, outFlowElement);
                // 排他网关
            } else if (outFlowElement instanceof ExclusiveGateway) {
                getNextNodeList(flowElements, outFlowElement, executionEntity, nextNodes, tempNodes, taskId, FlowConstant.EXCLUSIVE_GATEWAY);
                //并行网关
            } else if (outFlowElement instanceof ParallelGateway) {
                getNextNodeList(flowElements, outFlowElement, executionEntity, nextNodes, tempNodes, taskId, FlowConstant.PARALLEL_GATEWAY);
                //包含网关
            } else if (outFlowElement instanceof InclusiveGateway) {
                getNextNodeList(flowElements, outFlowElement, executionEntity, nextNodes, tempNodes, taskId, FlowConstant.INCLUSIVE_GATEWAY);
            } else if (outFlowElement instanceof EndEvent) {
                FlowElement subProcess = getSubProcess(flowElements, outFlowElement);
                if (subProcess == null) {
                    continue;
                }
                getNextNodeList(flowElements, subProcess, executionEntity, nextNodes, tempNodes, taskId, FlowConstant.END_EVENT);
            } else if (outFlowElement instanceof SubProcess) {
                Collection<FlowElement> subFlowElements = ((SubProcess) outFlowElement).getFlowElements();
                for (FlowElement element : subFlowElements) {
                    if (element instanceof StartEvent) {
                        List<SequenceFlow> startOutgoingFlows = ((StartEvent) element).getOutgoingFlows();
                        for (SequenceFlow outgoingFlow : startOutgoingFlows) {
                            FlowElement targetFlowElement = outgoingFlow.getTargetFlowElement();
                            if (targetFlowElement instanceof UserTask) {
                                nextNodeBuild(executionEntity, nextNodes, tempNodes, taskId, gateway, sequenceFlow, processNode, tempNode, targetFlowElement);
                                break;
                            }
                        }
                    }
                }
            } else {
                throw new ServiceException("未识别出节点类型");
            }
        }
    }

    /**
     * @description: 构建下一审批节点
     * @param: executionEntity
     * @param: nextNodes 下一节点信息
     * @param: tempNodes 保存没有表达式的节点信息(用于排他网关)
     * @param: taskId 任务id
     * @param: gateway 网关
     * @param: sequenceFlow  节点
     * @param: processNode 下一节点的目标元素
     * @param: tempNode  保存没有表达式的节点
     * @param: outFlowElement 目标节点
     * @return: void
     * @author: gssong
     * @date: 2022/4/11 13:35
     */
    private static void nextNodeBuild(ExecutionEntityImpl executionEntity, List<ProcessNode> nextNodes, List<ProcessNode> tempNodes, String taskId, String gateway, SequenceFlow sequenceFlow, ProcessNode processNode, ProcessNode tempNode, FlowElement outFlowElement) {
        // 判断是否为排它网关
        if (FlowConstant.EXCLUSIVE_GATEWAY.equals(gateway)) {
            String conditionExpression = sequenceFlow.getConditionExpression();
            //判断是否有条件
            if (StringUtils.isNotBlank(conditionExpression)) {
                ExpressCmd expressCmd = new ExpressCmd(sequenceFlow, executionEntity);
                Boolean condition = PROCESS_ENGINE.getManagementService().executeCommand(expressCmd);
                processNodeBuildList(processNode, outFlowElement, FlowConstant.EXCLUSIVE_GATEWAY, taskId, condition, nextNodes);
            } else {
                tempNodeBuildList(tempNodes, taskId, tempNode, outFlowElement);
            }
            //包含网关
        } else if (FlowConstant.INCLUSIVE_GATEWAY.equals(gateway)) {
            String conditionExpression = sequenceFlow.getConditionExpression();
            if (StringUtils.isBlank(conditionExpression)) {
                processNodeBuildList(processNode, outFlowElement, FlowConstant.INCLUSIVE_GATEWAY, taskId, true, nextNodes);
            } else {
                ExpressCmd expressCmd = new ExpressCmd(sequenceFlow, executionEntity);
                Boolean condition = PROCESS_ENGINE.getManagementService().executeCommand(expressCmd);
                processNodeBuildList(processNode, outFlowElement, FlowConstant.INCLUSIVE_GATEWAY, taskId, condition, nextNodes);
            }
        } else {
            processNodeBuildList(processNode, outFlowElement, FlowConstant.USER_TASK, taskId, true, nextNodes);
        }
    }

    /**
     * @description: 临时节点信息(排他网关)
     * @param: tempNodes 临时节点集合
     * @param: taskId 任务id
     * @param: tempNode 节点对象
     * @param: outFlowElement 节点信息
     * @return: void
     * @author: gssong
     * @date: 2022/7/16 19:17
     */
    private static void tempNodeBuildList(List<ProcessNode> tempNodes, String taskId, ProcessNode tempNode, FlowElement outFlowElement) {
        tempNode.setNodeId(outFlowElement.getId());
        tempNode.setNodeName(outFlowElement.getName());
        tempNode.setNodeType(FlowConstant.EXCLUSIVE_GATEWAY);
        tempNode.setTaskId(taskId);
        tempNode.setExpression(true);
        tempNode.setChooseWay(FlowConstant.WORKFLOW_ASSIGNEE);
        tempNode.setAssignee(((UserTask) outFlowElement).getAssignee());
        tempNode.setAssigneeId(((UserTask) outFlowElement).getAssignee());
        tempNodes.add(tempNode);
    }

    /**
     * @description: 保存节点信息
     * @param: processNode 节点对象
     * @param: outFlowElement 节点信息
     * @param: exclusiveGateway 网关
     * @param: taskId 任务id
     * @param: condition 条件
     * @param: nextNodes 节点集合
     * @return: void
     * @author: gssong
     * @date: 2022/7/16 19:17
     */
    private static void processNodeBuildList(ProcessNode processNode, FlowElement outFlowElement, String exclusiveGateway, String taskId, Boolean condition, List<ProcessNode> nextNodes) {
        processNode.setNodeId(outFlowElement.getId());
        processNode.setNodeName(outFlowElement.getName());
        processNode.setNodeType(exclusiveGateway);
        processNode.setTaskId(taskId);
        processNode.setExpression(condition);
        processNode.setChooseWay(FlowConstant.WORKFLOW_ASSIGNEE);
        processNode.setAssignee(((UserTask) outFlowElement).getAssignee());
        processNode.setAssigneeId(((UserTask) outFlowElement).getAssignee());
        nextNodes.add(processNode);
    }

    /**
     * @description: 判断是否为主流程结束节点
     * @param: flowElements全部节点
     * @param: endElement 结束节点
     * @return: org.flowable.bpmn.model.FlowElement
     * @author: gssong
     * @date: 2022/7/11 20:39
     */
    public static FlowElement getSubProcess(Collection<FlowElement> flowElements, FlowElement endElement) {
        for (FlowElement mainElement : flowElements) {
            if (mainElement instanceof SubProcess) {
                for (FlowElement subEndElement : ((SubProcess) mainElement).getFlowElements()) {
                    if (endElement.equals(subEndElement)) {
                        return mainElement;
                    }
                }
            }
        }
        return null;
    }

    /**
     * @description: 查询业务规则中的人员id
     * @param: businessRule 业务规则对象
     * @param: taskName 任务名称
     * @param: variables 业务变量
     * @return: 执行业务规则查询人员
     * @return: java.util.List<java.lang.String>
     * @author: gssong
     * @date: 2022/4/11 13:35
     */
    public static List<String> ruleAssignList(ActBusinessRuleVo businessRule, String taskName,Map<String,Object> variables) {
        try {
            //返回值
            Object obj;
            //方法名称
            String methodName = businessRule.getMethod();
            //全类名
            Object beanName = SpringUtils.getBean(businessRule.getBeanName());
            if (StringUtils.isNotBlank(businessRule.getParam())) {
                List<ActBusinessRuleParam> businessRuleParams = JsonUtils.parseArray(businessRule.getParam(), ActBusinessRuleParam.class);
                Class[] paramClass = new Class[businessRuleParams.size()];
                List<Object> params = new ArrayList<>();
                for (int i = 0; i < businessRuleParams.size(); i++) {
                    if (variables.containsKey(businessRuleParams.get(i).getParam())) {
                        String variable = (String) variables.get(businessRuleParams.get(i).getParam());
                        switch (businessRuleParams.get(i).getParamType()) {
                            case FlowConstant.PARAM_STRING:
                                paramClass[i] = String.valueOf(variable).getClass();
                                params.add(String.valueOf(variable));
                                break;
                            case FlowConstant.PARAM_SHORT:
                                paramClass[i] = Short.valueOf(variable).getClass();
                                params.add(Short.valueOf(variable));
                                break;
                            case FlowConstant.PARAM_INTEGER:
                                paramClass[i] = Integer.valueOf(variable).getClass();
                                params.add(Integer.valueOf(variable));
                                break;
                            case FlowConstant.PARAM_LONG:
                                paramClass[i] = Long.valueOf(variable).getClass();
                                params.add(Long.valueOf(variable));
                                break;
                            case FlowConstant.PARAM_FLOAT:
                                paramClass[i] = Float.valueOf(variable).getClass();
                                params.add(Float.valueOf(variable));
                                break;
                            case FlowConstant.PARAM_DOUBLE:
                                paramClass[i] = Double.valueOf(variable).getClass();
                                params.add(Double.valueOf(variable));
                                break;
                            case FlowConstant.PARAM_BOOLEAN:
                                paramClass[i] = Boolean.valueOf(variable).getClass();
                                params.add(Boolean.valueOf(variable));
                                break;
                            default:
                                break;
                        }
                    }
                }
                if (ObjectUtil.isEmpty(paramClass) && CollectionUtil.isNotEmpty(businessRuleParams)) {
                    String variableParams = businessRuleParams.stream().map(ActBusinessRuleParam::getParam).collect(Collectors.joining(","));
                    throw new ServiceException("【" + variableParams + "】流程变量不存在");
                }
                Method method = ReflectionUtils.findMethod(beanName.getClass(), methodName, paramClass);
                assert method != null;
                obj = ReflectionUtils.invokeMethod(method, beanName, params.toArray());
            } else {
                Method method = ReflectionUtils.findMethod(beanName.getClass(), methodName);
                assert method != null;
                obj = ReflectionUtils.invokeMethod(method, beanName);
            }
            if (obj == null) {
                throw new ServiceException("【" + taskName + "】任务环节未配置审批人,请确认传值是否正确,检查：【" + businessRule.getBeanName() + "】Bean容器中【" + methodName + "】方法");
            }
            return Arrays.asList(obj.toString().split(","));
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * @description: 查询业务规则中的人员id
     * @param: businessRule 业务规则对象
     * @param: taskId 任务id
     * @param: taskName 任务名称
     * @return: 执行业务规则查询人员
     * @return: java.util.List<java.lang.String>
     * @author: gssong
     * @date: 2022/4/11 13:35
     */
    public static List<String> ruleAssignList(ActBusinessRuleVo businessRule, String taskId, String taskName) {
        try {
            //返回值
            Object obj;
            //方法名称
            String methodName = businessRule.getMethod();
            //全类名
            Object beanName = SpringUtils.getBean(businessRule.getBeanName());
            if (StringUtils.isNotBlank(businessRule.getParam())) {
                List<ActBusinessRuleParam> businessRuleParams = JsonUtils.parseArray(businessRule.getParam(), ActBusinessRuleParam.class);
                Class[] paramClass = new Class[businessRuleParams.size()];
                List<Object> params = new ArrayList<>();
                for (int i = 0; i < businessRuleParams.size(); i++) {
                    Map<String, VariableInstance> variables = PROCESS_ENGINE.getTaskService().getVariableInstances(taskId);
                    if (variables.containsKey(businessRuleParams.get(i).getParam())) {
                        VariableInstance v = variables.get(businessRuleParams.get(i).getParam());
                        String variable = v.getTextValue();
                        switch (businessRuleParams.get(i).getParamType()) {
                            case FlowConstant.PARAM_STRING:
                                paramClass[i] = String.valueOf(variable).getClass();
                                params.add(String.valueOf(variable));
                                break;
                            case FlowConstant.PARAM_SHORT:
                                paramClass[i] = Short.valueOf(variable).getClass();
                                params.add(Short.valueOf(variable));
                                break;
                            case FlowConstant.PARAM_INTEGER:
                                paramClass[i] = Integer.valueOf(variable).getClass();
                                params.add(Integer.valueOf(variable));
                                break;
                            case FlowConstant.PARAM_LONG:
                                paramClass[i] = Long.valueOf(variable).getClass();
                                params.add(Long.valueOf(variable));
                                break;
                            case FlowConstant.PARAM_FLOAT:
                                paramClass[i] = Float.valueOf(variable).getClass();
                                params.add(Float.valueOf(variable));
                                break;
                            case FlowConstant.PARAM_DOUBLE:
                                paramClass[i] = Double.valueOf(variable).getClass();
                                params.add(Double.valueOf(variable));
                                break;
                            case FlowConstant.PARAM_BOOLEAN:
                                paramClass[i] = Boolean.valueOf(variable).getClass();
                                params.add(Boolean.valueOf(variable));
                                break;
                            default:
                                break;
                        }
                    }
                }
                if (ObjectUtil.isEmpty(paramClass) && CollectionUtil.isNotEmpty(businessRuleParams)) {
                    String variableParams = businessRuleParams.stream().map(ActBusinessRuleParam::getParam).collect(Collectors.joining(","));
                    throw new ServiceException("【" + variableParams + "】流程变量不存在");
                }
                Method method = ReflectionUtils.findMethod(beanName.getClass(), methodName, paramClass);
                assert method != null;
                obj = ReflectionUtils.invokeMethod(method, beanName, params.toArray());
            } else {
                Method method = ReflectionUtils.findMethod(beanName.getClass(), methodName);
                assert method != null;
                obj = ReflectionUtils.invokeMethod(method, beanName);
            }
            if (obj == null) {
                throw new ServiceException("【" + taskName + "】任务环节未配置审批人,请确认传值是否正确,检查：【" + businessRule.getBeanName() + "】Bean容器中【" + methodName + "】方法");
            }
            return Arrays.asList(obj.toString().split(","));
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * @description: 设置业务流程参数
     * @param: obj 对象
     * @param: id 主键id
     * @author: gssong
     * @date: 2022/1/16
     */
    public static void setStatusFieldValue(Object obj, String id) {
        Class<?> claszz = obj.getClass();
        ActBusinessStatus actBusinessStatus = iActBusinessStatusService.getInfoByBusinessKey(id);
        Field businessStatus;
        try {
            businessStatus = claszz.getDeclaredField(ACT_BUSINESS_STATUS);
            businessStatus.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            throw new ServiceException("未找到" + ACT_BUSINESS_STATUS + "属性：" + e.getMessage());
        }
        try {
            if (ObjectUtil.isNotEmpty(actBusinessStatus)) {
                businessStatus.set(obj, actBusinessStatus);
            } else {
                ActBusinessStatus status = new ActBusinessStatus();
                status.setStatus(BusinessStatusEnum.DRAFT.getStatus());
                businessStatus.set(obj, status);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException("设置流程状态失败：" + e.getMessage());
        }
    }

    /**
     * @description: 设置业务流程参数
     * @param: obj 对象
     * @param: idList 主键集合
     * @param: fieldName 主键属性名称
     * @return: void
     * @author: gssong
     * @date: 2022/9/6
     */
    public static void setStatusListFieldValue(Object obj, List<String> idList, String fieldName) {
        List<ActBusinessStatus> actBusinessStatusList = iActBusinessStatusService.getListInfoByBusinessKey(idList);
        if (obj instanceof Collection) {
            Collection<?> collection = (Collection<?>) obj;
            for (Object o : collection) {
                if (o != null) {
                    Class<?> claszz = o.getClass();
                    Field businessStatus;
                    Field fieldInfo;
                    String fieldValue;
                    try {
                        businessStatus = claszz.getDeclaredField(ACT_BUSINESS_STATUS);
                        businessStatus.setAccessible(true);
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                        throw new ServiceException("未找到" + ACT_BUSINESS_STATUS + "属性：" + e.getMessage());
                    }
                    try {
                        fieldInfo = claszz.getDeclaredField(fieldName);
                        fieldInfo.setAccessible(true);
                        fieldValue = fieldInfo.get(o).toString();
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                        throw new ServiceException("未找到" + fieldName + "属性：" + e.getMessage());
                    }
                    try {
                        ActBusinessStatus actBusinessStatus = actBusinessStatusList.stream().filter(e -> e.getBusinessKey().equals(fieldValue)).findFirst().orElse(null);
                        if (ObjectUtil.isNotEmpty(actBusinessStatus)) {
                            businessStatus.set(o, actBusinessStatus);
                        } else {
                            ActBusinessStatus status = new ActBusinessStatus();
                            status.setStatus(BusinessStatusEnum.DRAFT.getStatus());
                            businessStatus.set(o, status);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new ServiceException("设置流程状态失败：" + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * @description: 设置流程节点人员设置对象
     * @param: obj 对象
     * @param: taskId 任务id
     * @author: gssong
     * @date: 2022/11/27
     */
    public static void setActNodeAssignee(Object obj, String taskId) {
        Task task = PROCESS_ENGINE.getTaskService().createTaskQuery().taskId(taskId).singleResult();
        if (task != null) {
            ActNodeAssignee actNodeAssignee = iActNodeAssigneeService.getInfoSetting(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
            if (actNodeAssignee != null && StringUtils.isNotBlank(actNodeAssignee.getFieldListJson())) {
                List<FieldList> fieldLists = JsonUtils.parseArray(actNodeAssignee.getFieldListJson(), FieldList.class);
                Map<String, FieldList> collectMap = fieldLists.stream().collect(Collectors.toMap(FieldList::getField, Function.identity()));
                actNodeAssignee.setFieldMap(collectMap);
                Class<?> claszz = obj.getClass();
                Field nodeAssignee;
                try {
                    nodeAssignee = claszz.getDeclaredField(ACT_NODE_ASSIGNEE);
                    nodeAssignee.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                    throw new ServiceException("未找到" + ACT_NODE_ASSIGNEE + "属性：" + e.getMessage());
                }
                try {
                    nodeAssignee.set(obj, actNodeAssignee);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    throw new ServiceException("设置流程状态失败：" + e.getMessage());
                }
            }
        }
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
    public static List<Long> getAssigneeIdList(String params, String chooseWay, String nodeName) {
        return iUserService.getAssigneeIdList(params, chooseWay, nodeName);
    }

    /**
     * @description: 删除正在执行的任务
     * @param: task
     * @return: void
     * @author: gssong
     * @date: 2022/4/11 13:36
     */
    public static void deleteRuntimeTask(Task task) {
        DeleteTaskCmd deleteTaskCmd = new DeleteTaskCmd(task.getId());
        PROCESS_ENGINE.getManagementService().executeCommand(deleteTaskCmd);
        DeleteExecutionCmd deleteExecutionCmd = new DeleteExecutionCmd(task.getExecutionId());
        PROCESS_ENGINE.getManagementService().executeCommand(deleteExecutionCmd);
        PROCESS_ENGINE.getHistoryService().deleteHistoricTaskInstance(task.getId());
        PROCESS_ENGINE.getHistoryService().createNativeHistoricActivityInstanceQuery()
            .sql("DELETE  FROM ACT_HI_ACTINST WHERE EXECUTION_ID_ = '" + task.getExecutionId() + "'");
    }

    /**
     * @description: 判断当前节点是否为会签节点
     * @param: processDefinitionId 流程定义id
     * @param: taskDefinitionKey 当前节点id
     * @return: com.ruoyi.workflow.domain.vo.MultiVo
     * @author: gssong
     * @date: 2022/4/16 13:31
     */
    public static MultiVo isMultiInstance(String processDefinitionId, String taskDefinitionKey) {
        BpmnModel bpmnModel = PROCESS_ENGINE.getRepositoryService().getBpmnModel(processDefinitionId);
        FlowNode flowNode = (FlowNode) bpmnModel.getFlowElement(taskDefinitionKey);
        MultiVo multiVo = new MultiVo();
        //判断是否为并行会签节点
        if (flowNode.getBehavior() instanceof ParallelMultiInstanceBehavior) {
            ParallelMultiInstanceBehavior behavior = (ParallelMultiInstanceBehavior) flowNode.getBehavior();
            if (behavior != null && behavior.getCollectionExpression() != null) {
                Expression collectionExpression = behavior.getCollectionExpression();
                String assigneeList = collectionExpression.getExpressionText();
                String assignee = behavior.getCollectionElementVariable();
                multiVo.setType(behavior);
                multiVo.setAssignee(assignee);
                multiVo.setAssigneeList(assigneeList);
                return multiVo;
            }
            //判断是否为串行会签节点
        } else if (flowNode.getBehavior() instanceof SequentialMultiInstanceBehavior) {
            SequentialMultiInstanceBehavior behavior = (SequentialMultiInstanceBehavior) flowNode.getBehavior();
            if (behavior != null && behavior.getCollectionExpression() != null) {
                Expression collectionExpression = behavior.getCollectionExpression();
                String assigneeList = collectionExpression.getExpressionText();
                String assignee = behavior.getCollectionElementVariable();
                multiVo.setType(behavior);
                multiVo.setAssignee(assignee);
                multiVo.setAssigneeList(assigneeList);
                return multiVo;
            }
        }
        return null;
    }

    /**
     * @description: 创建子任务
     * @param: parentTask
     * @param: assignees
     * @return: java.util.List<org.flowable.task.api.Task>
     * @author: gssong
     * @date: 2022/5/6 19:18
     */
    public static List<Task> createSubTask(List<Task> parentTaskList, String assignees) {
        List<Task> list = new ArrayList<>();
        for (Task parentTask : parentTaskList) {
            String[] userIds = assignees.split(",");
            for (String userId : userIds) {
                TaskEntity newTask = (TaskEntity) PROCESS_ENGINE.getTaskService().newTask();
                newTask.setParentTaskId(parentTask.getId());
                newTask.setAssignee(userId);
                newTask.setName("【抄送】-" + parentTask.getName());
                newTask.setProcessDefinitionId(parentTask.getProcessDefinitionId());
                newTask.setProcessInstanceId(parentTask.getProcessInstanceId());
                newTask.setTaskDefinitionKey(parentTask.getTaskDefinitionKey());
                PROCESS_ENGINE.getTaskService().saveTask(newTask);
                list.add(newTask);
            }
        }
        if (CollectionUtil.isNotEmpty(list) && CollectionUtil.isNotEmpty(parentTaskList)) {
            String processInstanceId = parentTaskList.get(0).getProcessInstanceId();
            String processDefinitionId = parentTaskList.get(0).getProcessDefinitionId();
            List<String> taskIds = list.stream().map(Task::getId).collect(Collectors.toList());
            UpdateHiTaskInstCmd updateHiTaskInstCmd = new UpdateHiTaskInstCmd(taskIds, processDefinitionId, processInstanceId);
            PROCESS_ENGINE.getManagementService().executeCommand(updateHiTaskInstCmd);
        }
        return list;
    }

    /**
     * @description: 创建流程任务
     * @param: parentTask
     * @param: createTime
     * @return: org.flowable.task.service.impl.persistence.entity.TaskEntity
     * @author: gssong
     * @date: 2022/3/13
     */
    public static TaskEntity createNewTask(Task currentTask) {
        TaskEntity task = null;
        if (ObjectUtil.isNotEmpty(currentTask)) {
            task = (TaskEntity) PROCESS_ENGINE.getTaskService().newTask();
            task.setCategory(currentTask.getCategory());
            task.setDescription(currentTask.getDescription());
            task.setTenantId(currentTask.getTenantId());
            task.setAssignee(currentTask.getAssignee());
            task.setName(currentTask.getName());
            task.setProcessDefinitionId(currentTask.getProcessDefinitionId());
            task.setProcessInstanceId(currentTask.getProcessInstanceId());
            task.setTaskDefinitionKey(currentTask.getTaskDefinitionKey());
            task.setPriority(currentTask.getPriority());
            task.setCreateTime(new Date());
            PROCESS_ENGINE.getTaskService().saveTask(task);
        }
        if (ObjectUtil.isNotNull(task)) {
            UpdateHiTaskInstCmd updateHiTaskInstCmd = new UpdateHiTaskInstCmd(Collections.singletonList(task.getId()),task.getProcessDefinitionId(),task.getProcessInstanceId());
            PROCESS_ENGINE.getManagementService().executeCommand(updateHiTaskInstCmd);
        }
        return task;
    }

    /**
     * @description: 发送站内信
     * @param: sendMessage
     * @param: processInstanceId
     * @return: void
     * @author: gssong
     * @date: 2022/6/18 13:26
     */
    public static void sendMessage(SendMessage sendMessage, String processInstanceId) {
        List<SysMessage> messageList = new ArrayList<>();
        List<Task> taskList = PROCESS_ENGINE.getTaskService().createTaskQuery().processInstanceId(processInstanceId).list();
        for (Task taskInfo : taskList) {
            if (StringUtils.isNotBlank(taskInfo.getAssignee())) {
                SysMessage sysMessage = new SysMessage();
                sysMessage.setSendId(LoginHelper.getUserId());
                sysMessage.setRecordId(Long.valueOf(taskInfo.getAssignee()));
                sysMessage.setType(1);
                sysMessage.setTitle(sendMessage.getTitle());
                sysMessage.setMessageContent(sendMessage.getMessageContent() + ",请您注意查收");
                sysMessage.setStatus(0);
                messageList.add(sysMessage);
            } else {
                List<IdentityLink> identityLinkList = getCandidateUser(taskInfo.getId());
                if (CollectionUtil.isNotEmpty(identityLinkList)) {
                    for (IdentityLink identityLink : identityLinkList) {
                        SysMessage sysMessage = new SysMessage();
                        sysMessage.setSendId(LoginHelper.getUserId());
                        sysMessage.setRecordId(Long.valueOf(identityLink.getUserId()));
                        sysMessage.setType(1);
                        sysMessage.setTitle(sendMessage.getTitle());
                        sysMessage.setMessageContent(sendMessage.getMessageContent() + ",请您注意查收");
                        sysMessage.setStatus(0);
                        messageList.add(sysMessage);
                    }
                }
            }
        }
        if (CollectionUtil.isNotEmpty(messageList)) {
            iSysMessageService.sendBatchMessage(messageList);
            //TODO 邮件 短信 自行编写
        }
    }

    /**
     * @description: 执行bean中方法
     * @param: serviceName bean名称
     * @param: methodName 方法名称
     * @param: params 参数
     * @author: gssong
     * @date: 2022/6/26 15:37
     */
    public static void springInvokeMethod(String serviceName, String methodName, Object... params) {
        Object service = SpringUtils.getBean(serviceName);
        Class<?>[] paramClass = null;
        if (Objects.nonNull(params)) {
            int paramsLength = params.length;
            paramClass = new Class[paramsLength];
            for (int i = 0; i < paramsLength; i++) {
                paramClass[i] = params[i].getClass();
            }
        }
        // 找到方法
        Method method = ReflectionUtils.findMethod(service.getClass(), methodName, paramClass);
        // 执行方法
        assert method != null;
        ReflectionUtils.invokeMethod(method, service, params);
    }

    /**
     * @description: 获取候选人
     * @param: taskId
     * @return: java.util.List<org.flowable.identitylink.api.IdentityLink>
     * @author: gssong
     * @date: 2022/7/9 17:55
     */
    public static List<IdentityLink> getCandidateUser(String taskId) {
        return PROCESS_ENGINE.getTaskService().getIdentityLinksForTask(taskId);
    }

    /**
     * @description: 自动办理任务
     * @param: processInstanceId 流程实例id
     * @param: businessKey 业务id
     * @param: processNodeAssigneeList 流程人员
     * @param: actNodeAssignees 流程定义设置
     * @return: void
     * @author: gssong
     * @date: 2022/12/19 13:53
     */
    public static boolean autoComplete(String processInstanceId, String businessKey, List<ActProcessNodeAssignee> processNodeAssigneeList, List<ActNodeAssignee> actNodeAssignees){
        List<Task> taskList = PROCESS_ENGINE.getTaskService().createTaskQuery().processInstanceId(processInstanceId).list();
        if (CollectionUtil.isEmpty(taskList)) {
            iActBusinessStatusService.updateState(businessKey, BusinessStatusEnum.FINISH, processInstanceId);
        }
        List<Task> list = PROCESS_ENGINE.getTaskService().createTaskQuery().processInstanceId(processInstanceId)
            .taskCandidateOrAssigned(LoginHelper.getUserId().toString()).list();
        if (CollectionUtil.isEmpty(list)) {
            return false;
        }
        for (Task task : list) {
            ActNodeAssignee nodeAssignee = actNodeAssignees.stream().filter(e -> task.getTaskDefinitionKey().equals(e.getNodeId())).findFirst().orElse(null);
            if (ObjectUtil.isNull(nodeAssignee)) {
                throw new ServiceException("请检查【" + task.getName() + "】节点配置");
            }
            if (!nodeAssignee.getAutoComplete()) {
                return false;
            }
            if(StringUtils.isBlank(task.getAssignee())){
                PROCESS_ENGINE.getTaskService().claim(task.getId(),LoginHelper.getUserId().toString());
                task.setAssignee(LoginHelper.getUserId().toString());
            }
            PROCESS_ENGINE.getTaskService().addComment(task.getId(), task.getProcessInstanceId(), "流程引擎满足条件自动办理");
            PROCESS_ENGINE.getTaskService().complete(task.getId());
            recordExecuteNode(task, actNodeAssignees);
        }
        List<Task> tasks = PROCESS_ENGINE.getTaskService().createTaskQuery().processInstanceId(processInstanceId).list();
        if(CollUtil.isNotEmpty(tasks)){
            for (Task task : tasks) {
                processNodeAssigneeList.stream().filter(e -> e.getNodeId().equals(task.getTaskDefinitionKey()) && !e.getMultiple()).findFirst()
                    .ifPresent(e -> {
                        String[] userIds = e.getAssigneeId().split(",");
                        if(userIds.length == 1){
                            PROCESS_ENGINE.getTaskService().setAssignee(task.getId(), userIds[0]);
                        }else{
                            for (String userId : userIds) {
                                PROCESS_ENGINE.getTaskService().addCandidateUser(task.getId(), userId);
                            }
                        }
                    });
            }
        }
        autoComplete(processInstanceId, businessKey, processNodeAssigneeList, actNodeAssignees);
        return true;
    }

    /**
     * @description: 自动办理任务
     * @param: processInstanceId 流程实例id
     * @param: businessKey 业务id
     * @param: actNodeAssignees 流程定义设置
     * @return: java.lang.Boolean
     * @author: gssong
     * @date: 2022/7/12 21:27
     */
    public static Boolean autoComplete(String processInstanceId, String businessKey, List<ActNodeAssignee> actNodeAssignees, TaskCompleteBo req) {

        List<Task> taskList = PROCESS_ENGINE.getTaskService().createTaskQuery().processInstanceId(processInstanceId).list();
        if (CollectionUtil.isEmpty(taskList)) {
            iActBusinessStatusService.updateState(businessKey, BusinessStatusEnum.FINISH, processInstanceId);
        }
        for (Task task : taskList) {
            ActNodeAssignee nodeAssignee = actNodeAssignees.stream().filter(e -> task.getTaskDefinitionKey().equals(e.getNodeId())).findFirst().orElse(null);
            if (ObjectUtil.isNull(nodeAssignee)) {
                throw new ServiceException("请检查【" + task.getName() + "】节点配置");
            }

            if (!nodeAssignee.getAutoComplete()) {
                return false;
            }
            settingAssignee(task, nodeAssignee, nodeAssignee.getMultiple());
            List<Long> assignees = req.getAssignees(task.getTaskDefinitionKey());
            if (!nodeAssignee.getIsShow() && CollectionUtil.isNotEmpty(assignees) && assignees.contains(LoginHelper.getUserId())) {
                PROCESS_ENGINE.getTaskService().addComment(task.getId(), task.getProcessInstanceId(), "流程引擎满足条件自动办理");
                PROCESS_ENGINE.getTaskService().complete(task.getId());
                recordExecuteNode(task, actNodeAssignees);
            } else {
                settingAssignee(task, nodeAssignee, nodeAssignee.getMultiple());
            }

        }
        List<Task> list = PROCESS_ENGINE.getTaskService().createTaskQuery().processInstanceId(processInstanceId)
            .taskCandidateOrAssigned(LoginHelper.getUserId().toString()).list();
        if (CollectionUtil.isEmpty(list)) {
            return false;
        }
        for (Task task : list) {
            if(StringUtils.isBlank(task.getAssignee())){
                PROCESS_ENGINE.getTaskService().claim(task.getId(),LoginHelper.getUserId().toString());
                task.setAssignee(LoginHelper.getUserId().toString());
            }
            PROCESS_ENGINE.getTaskService().addComment(task.getId(), task.getProcessInstanceId(), "流程引擎满足条件自动办理");
            PROCESS_ENGINE.getTaskService().complete(task.getId());
            recordExecuteNode(task, actNodeAssignees);
        }
        autoComplete(processInstanceId, businessKey, actNodeAssignees, req);
        return true;
    }

    /**
     * @description: 设置任务执行人员
     * @param: task 任务信息
     * @param: actNodeAssignee 人员设置
     * @param: multiple 是否为会签节点
     * @return: void
     * @author: gssong
     * @date: 2022/7/8
     */
    public static void settingAssignee(Task task, ActNodeAssignee actNodeAssignee, Boolean multiple) {
        //按业务规则选人
        if (FlowConstant.WORKFLOW_RULE.equals(actNodeAssignee.getChooseWay())) {
            ActBusinessRuleVo actBusinessRuleVo = iActBusinessRuleService.queryById(actNodeAssignee.getBusinessRuleId());
            List<String> ruleAssignList = ruleAssignList(actBusinessRuleVo, task.getId(), task.getName());
            List<Long> userIdList = new ArrayList<>();
            for (String userId : ruleAssignList) {
                userIdList.add(Long.valueOf(userId));
            }
            if (multiple) {
                PROCESS_ENGINE.getTaskService().setVariable(task.getId(), actNodeAssignee.getMultipleColumn(), userIdList);
            } else {
                setAssignee(task, userIdList);
            }
        } else {
            if (StringUtils.isBlank(actNodeAssignee.getAssigneeId())) {
                throw new ServiceException("请检查【" + task.getName() + "】节点配置");
            }
            // 设置审批人员
            List<Long> assignees = getAssigneeIdList(actNodeAssignee.getAssigneeId(), actNodeAssignee.getChooseWay(), task.getName());
            if (multiple) {
                PROCESS_ENGINE.getTaskService().setVariable(task.getId(), actNodeAssignee.getMultipleColumn(), assignees);
            } else {
                setAssignee(task, assignees);
            }
        }
    }

    /**
     * @description: 设置任务人员
     * @param: task 任务
     * @param: assignees 办理人
     * @return: void
     * @author: gssong
     * @date: 2021/10/21
     */
    public static void setAssignee(Task task, List<Long> assignees) {
        if (assignees.size() == 1) {
            PROCESS_ENGINE.getTaskService().setAssignee(task.getId(), assignees.get(0).toString());
        } else {
            // 多个作为候选人
            for (Long assignee : assignees) {
                PROCESS_ENGINE.getTaskService().addCandidateUser(task.getId(), assignee.toString());
            }
        }
    }

    /**
     * @description: 记录审批节点
     * @param: task
     * @param: actNodeAssignees
     * @return: void
     * @author: gssong
     * @date: 2022/7/29 20:57
     */
    public static void recordExecuteNode(Task task, List<ActNodeAssignee> actNodeAssignees) {
        List<ActTaskNode> actTaskNodeList = iActTaskNodeService.getListByInstanceId(task.getProcessInstanceId());
        ActTaskNode actTaskNode = new ActTaskNode();
        actTaskNode.setNodeId(task.getTaskDefinitionKey());
        actTaskNode.setNodeName(task.getName());
        actTaskNode.setInstanceId(task.getProcessInstanceId());
        actTaskNode.setAssignee(task.getAssignee());
        MultiVo multiInstance = isMultiInstance(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
        if (ObjectUtil.isNotEmpty(multiInstance)) {
            actTaskNode.setTaskType(MULTI_INSTANCE);
        } else {
            actTaskNode.setTaskType(USER_TASK);
        }
        if (CollectionUtil.isEmpty(actTaskNodeList)) {
            actTaskNode.setOrderNo(0);
            actTaskNode.setIsBack(true);
            iActTaskNodeService.save(actTaskNode);
        } else {
            ActNodeAssignee actNodeAssignee = actNodeAssignees.stream().filter(e -> e.getNodeId().equals(task.getTaskDefinitionKey())).findFirst().orElse(null);
            //如果为设置流程定义配置默认 当前环节可以回退
            if (ObjectUtil.isEmpty(actNodeAssignee)) {
                actTaskNode.setIsBack(true);
            } else {
                actTaskNode.setIsBack(actNodeAssignee.getIsBack());
            }
            iActTaskNodeService.saveTaskNode(actTaskNode);
        }
    }

    /**
     * @description: 获取当前驳回节点的网关（并行网关，包容网关），获取网关节点走向
     * @param: task 任务
     * @param: targetActivityId 驳回的节点id
     * @return: java.util.List<java.lang.String>
     * @author: gssong
     * @date: 2022/4/10
     */
    public static List<String> getGatewayNode(Task task, String targetActivityId) {
        List<String> nodeListId = new ArrayList<>();
        Map<String, Object> variables = PROCESS_ENGINE.getRuntimeService().getVariables(task.getExecutionId());
        BpmnModel bpmnModel = PROCESS_ENGINE.getRepositoryService().getBpmnModel(task.getProcessDefinitionId());
        FlowElement flowElement = bpmnModel.getFlowElement(targetActivityId);
        List<SequenceFlow> incomingFlows = ((FlowNode) flowElement).getIncomingFlows();
        for (SequenceFlow incomingFlow : incomingFlows) {
            FlowElement sourceFlowElement = incomingFlow.getSourceFlowElement();
            //并行网关
            if (sourceFlowElement instanceof ParallelGateway) {
                List<SequenceFlow> parallelGatewayOutgoingFlow = ((ParallelGateway) sourceFlowElement).getOutgoingFlows();
                for (SequenceFlow sequenceFlow : parallelGatewayOutgoingFlow) {
                    FlowElement element = sequenceFlow.getTargetFlowElement();
                    if (element instanceof UserTask) {
                        nodeListId.add(element.getId());
                    }
                }
                //包容网关
            } else if (sourceFlowElement instanceof InclusiveGateway) {
                List<SequenceFlow> inclusiveGatewayOutgoingFlow = ((InclusiveGateway) sourceFlowElement).getOutgoingFlows();
                for (SequenceFlow sequenceFlow : inclusiveGatewayOutgoingFlow) {
                    String conditionExpression = sequenceFlow.getConditionExpression();
                    FlowElement element = sequenceFlow.getTargetFlowElement();
                    if (element instanceof UserTask) {
                        if (StringUtils.isBlank(conditionExpression)) {
                            nodeListId.add(element.getId());
                        } else {
                            ExpressCheckCmd expressCmd = new ExpressCheckCmd(conditionExpression, variables);
                            Boolean condition = PROCESS_ENGINE.getManagementService().executeCommand(expressCmd);
                            if (condition) {
                                nodeListId.add(element.getId());
                            }
                        }
                    }
                }
            }
        }
        return nodeListId;
    }

    /**
     * @description: 设置流程变量
     * @param: taskId
     * @param: variableName
     * @param: value
     * @return: void
     * @author: gssong
     * @date: 2022/10/18 12:25
     */
    public static void setVariable(String taskId, String variableName, Object value) {
        PROCESS_ENGINE.getTaskService().setVariable(taskId, variableName, value);
    }

    /**
     * @description: 设置流程变量
     * @param: taskId
     * @param: variables
     * @return: void
     * @author: gssong
     * @date: 2022/10/18 12:25
     */
    public static void setVariables(String taskId, Map<String, Object> variables) {
        PROCESS_ENGINE.getTaskService().setVariables(taskId, variables);
    }

    /**
     * @description: 获取当前任务
     * @param: taskId
     * @return: org.flowable.task.api.Task
     * @author: gssong
     * @date: 2022/10/18 12:26
     */
    public static Task getCurrentTask(String taskId) {
        return PROCESS_ENGINE.getTaskService().createTaskQuery().taskId(taskId).singleResult();
    }

    /**
     * @description: 获取当前任务
     * @param: taskId
     * @return: java.util.List<org.flowable.task.api.Task>
     * @author: gssong
     * @date: 2022/10/18 12:26
     */
    public static List<Task> getCurrentTaskList(String processInstanceId) {
        return PROCESS_ENGINE.getTaskService().createTaskQuery().processInstanceId(processInstanceId).list();
    }

    /**
     * @description: 获取当前任务审批人
     * @param: taskId
     * @return: java.util.List<org.flowable.identitylink.api.IdentityLink>
     * @author: gssong
     * @date: 2022/10/18 12:43
     */
    public static List<IdentityLink> getCurrentApprover(String taskId) {
        return PROCESS_ENGINE.getTaskService().getIdentityLinksForTask(taskId);
    }
}
