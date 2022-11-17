package com.ruoyi.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workflow.common.constant.ActConstant;
import com.ruoyi.workflow.domain.ActCategory;
import com.ruoyi.workflow.domain.ActNodeAssignee;
import com.ruoyi.workflow.domain.bo.DefinitionBo;
import com.ruoyi.workflow.domain.vo.ActProcessDefSettingVo;
import com.ruoyi.workflow.domain.vo.ActProcessNodeVo;
import com.ruoyi.workflow.domain.vo.ProcessDefinitionVo;
import com.ruoyi.workflow.flowable.factory.WorkflowService;
import com.ruoyi.workflow.mapper.ProcessDefinitionMapper;
import com.ruoyi.workflow.service.IActCategoryService;
import com.ruoyi.workflow.service.IActNodeAssigneeService;
import com.ruoyi.workflow.service.IActProcessDefSetting;
import com.ruoyi.workflow.service.IProcessDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.ProcessMigrationService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

/**
 * @description: 流程定义服务层
 * @author: gssong
 * @date: 2021/10/07 11:14
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessDefinitionServiceImpl extends WorkflowService implements IProcessDefinitionService {

    private final IActNodeAssigneeService iActNodeAssigneeService;

    private final ProcessDefinitionMapper processDefinitionMapper;

    private final IActProcessDefSetting iActProcessDefSetting;

    private final IActCategoryService iActCategoryService;

    /**
     * @description: 查询流程定义列表
     * @param: definitionBo
     * @return: com.ruoyi.common.core.page.TableDataInfo<com.ruoyi.workflow.domain.vo.ProcessDefinitionVo>
     * @author: gssong
     * @date: 2021/10/7
     */
    @Override
    public TableDataInfo<ProcessDefinitionVo> getByPage(DefinitionBo definitionBo) {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
        if (StringUtils.isNotEmpty(definitionBo.getKey())) {
            query.processDefinitionKeyLike("%" + definitionBo.getKey() + "%");
        }
        if (StringUtils.isNotEmpty(definitionBo.getName())) {
            query.processDefinitionNameLike("%" + definitionBo.getName() + "%");
        }
        if (StringUtils.isNotEmpty(definitionBo.getCategory())) {
            query.processDefinitionCategory(definitionBo.getCategory());
        }
        // 分页查询
        List<ProcessDefinitionVo> processDefinitionVoList = new ArrayList<>();
        List<ProcessDefinition> definitionList = query.latestVersion().listPage(definitionBo.getPageNum(), definitionBo.getPageSize());
        List<String> deploymentIds = definitionList.stream().map(ProcessDefinition::getDeploymentId).collect(Collectors.toList());
        List<Deployment> deploymentList = repositoryService.createDeploymentQuery()
            .deploymentIds(deploymentIds).list();
        List<ActProcessDefSettingVo> processDefSettingList = null;
        if (CollectionUtil.isNotEmpty(definitionList)) {
            List<String> defIds = definitionList.stream().map(ProcessDefinition::getId).collect(Collectors.toList());
            processDefSettingList = iActProcessDefSetting.getProcessDefSettingByDefIds(defIds);
        }
        for (ProcessDefinition processDefinition : definitionList) {
            // 部署时间
            Deployment deployment = deploymentList.stream().filter(e -> e.getId().equals(processDefinition.getDeploymentId())).findFirst().orElse(null);
            ProcessDefinitionVo processDefinitionVo = BeanUtil.toBean(processDefinition, ProcessDefinitionVo.class);
            if (ObjectUtil.isNotEmpty(deployment) && deployment.getDeploymentTime() != null) {
                processDefinitionVo.setDeploymentTime(deployment.getDeploymentTime());
            }
            // 流程定义设置
            if (CollectionUtil.isNotEmpty(processDefSettingList)) {
                processDefSettingList.stream().filter(e -> processDefinition.getId().equals(e.getProcessDefinitionId())).findFirst().ifPresent(processDefinitionVo::setActProcessDefSettingVo);
            }
            processDefinitionVoList.add(processDefinitionVo);
        }
        // 总记录数
        long total = query.count();

        return new TableDataInfo<>(processDefinitionVoList, total);
    }

    /**
     * @description: 查询历史流程定义列表
     * @param: definitionBo
     * @return: java.util.List<com.ruoyi.workflow.domain.vo.ProcessDefinitionVo>
     * @author: gssong
     * @date: 2021/10/7
     */
    @Override
    public List<ProcessDefinitionVo> getHistByPage(DefinitionBo definitionBo) {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
        if (StringUtils.isNotBlank(definitionBo.getKey())) {
            query.processDefinitionKey(definitionBo.getKey());
        }

        // 分页查询
        List<ProcessDefinitionVo> processDefinitionVoList = new ArrayList<>();
        List<ProcessDefinition> definitionList = query.list();
        for (ProcessDefinition processDefinition : definitionList) {
            if (!processDefinition.getId().equals(definitionBo.getId())) {
                // 部署时间
                Deployment deployment = repositoryService.createDeploymentQuery()
                    .deploymentId(processDefinition.getDeploymentId()).singleResult();
                ProcessDefinitionVo processDefinitionVo = BeanUtil.toBean(processDefinition, ProcessDefinitionVo.class);
                processDefinitionVo.setDeploymentTime(deployment.getDeploymentTime());
                processDefinitionVoList.add(processDefinitionVo);
            }
        }
        return CollectionUtil.reverse(processDefinitionVoList);
    }

    /**
     * @description: 删除流程定义
     * @param: deploymentId
     * @param: definitionId
     * @return: java.lang.Boolean
     * @author: gssong
     * @date: 2021/10/7
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteDeployment(String deploymentId, String definitionId) {
        try {
            List<HistoricTaskInstance> taskInstanceList = historyService.createHistoricTaskInstanceQuery().processDefinitionId(definitionId).list();
            if (CollectionUtil.isNotEmpty(taskInstanceList)) {
                throw new ServiceException("当前流程定义已被使用不可删除！");
            }
            //删除流程定义
            repositoryService.deleteDeployment(deploymentId);
            //删除流程节点设置人员
            iActNodeAssigneeService.delByDefinitionId(definitionId);
            //删除流程定义设置
            iActProcessDefSetting.deleteByDefinitionIds(Collections.singletonList(definitionId));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * @description: 通过zip或xml部署流程定义
     * @param: file
     * @return: java.lang.Boolean
     * @author: gssong
     * @date: 2022/4/12 13:32
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deployByFile(MultipartFile file) {
        try {
            // 文件名 = 流程名称-流程key-流程分类
            String filename = file.getOriginalFilename();
            assert filename != null;
            String[] splitFilename = filename.substring(0, filename.lastIndexOf(".")).split("-");
            if (splitFilename.length < 3) {
                throw new ServiceException("流程分类不能为空(文件名 = 流程名称-流程key-流程分类)");
            }
            //流程名称
            String processName = splitFilename[0];
            //流程key
            String processKey = splitFilename[1];
            //流程分类
            String category = splitFilename[2];

            ActCategory actCategory = iActCategoryService.queryById(Long.valueOf(category));
            if (actCategory == null) {
                throw new ServiceException("流程分类不存在");
            }
            // 文件后缀名
            String suffix = filename.substring(filename.lastIndexOf(".") + 1).toUpperCase();
            InputStream inputStream = file.getInputStream();
            Deployment deployment;
            if (ActConstant.ZIP.equals(suffix)) {
                // zip
                deployment = repositoryService.createDeployment()
                    .addZipInputStream(new ZipInputStream(inputStream)).name(processName).key(processKey).category(category).deploy();
            } else {
                // xml 或 bpmn
                deployment = repositoryService.createDeployment()
                    .addInputStream(filename, inputStream).name(processName).key(processKey).category(category).deploy();
            }
            // 更新分类
            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
            repositoryService.setProcessDefinitionCategory(definition.getId(), category);

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            throw new ServiceException("部署失败" + e.getMessage());
        }
    }

    /**
     * @description: 导出流程定义文件（xml,png)
     * @param: type 类型 xml 或 png
     * @param: definitionId 流程定义id
     * @param: response
     * @return: void
     * @author: gssong
     * @date: 2021/10/7
     */
    @Override
    public void exportFile(String type, String definitionId, HttpServletResponse response) {
        try {
            ProcessDefinition processDefinition = repositoryService.getProcessDefinition(definitionId);
            String resourceName = "文件不存在";

            if (ActConstant.XML.equals(type)) {
                //xml名称
                resourceName = processDefinition.getResourceName();
            } else if (ActConstant.PNG.equals(type)) {
                // 获取 png 图片资源名
                resourceName = processDefinition.getDiagramResourceName();
            }
            InputStream inputStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(), resourceName);
            // 创建输出流
            response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode(resourceName, ActConstant.UTF_8));
            // 流的拷贝放到设置请求头下面，不然文件大于10k可能无法导出
            IOUtils.copy(inputStream, response.getOutputStream());

            response.flushBuffer();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("导出文件失败：{}", e.getMessage());
        }
    }

    /**
     * @description: 查看xml文件
     * @param: definitionId
     * @return: java.lang.String
     * @author: gssong
     * @date: 2022/5/3 19:34
     */
    @Override
    public String getXml(String definitionId) {
        StringBuilder xml = new StringBuilder();
        ProcessDefinition processDefinition = repositoryService.getProcessDefinition(definitionId);
        InputStream inputStream;
        try {
            inputStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(), processDefinition.getResourceName());
            xml.append(IOUtils.toString(inputStream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return xml.toString();
    }

    /**
     * @description: 激活或者挂起流程定义
     * @param: data 参数
     * @return: com.ruoyi.common.core.domain.R<java.lang.Boolean>
     * @author: gssong
     * @date: 2021/10/10
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateProcDefState(Map<String, Object> data) {
        try {
            String definitionId = data.get("definitionId").toString();
            String description = data.get("description").toString();
            //更新原因
            processDefinitionMapper.updateDescriptionById(definitionId, description);
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(definitionId).singleResult();
            //将当前为挂起状态更新为激活状态
            //参数说明：参数1：流程定义id,参数2：是否激活（true是否级联对应流程实例，激活了则对应流程实例都可以审批），
            //参数3：什么时候激活，如果为null则立即激活，如果为具体时间则到达此时间后激活
            if (processDefinition.isSuspended()) {
                repositoryService.activateProcessDefinitionById(definitionId, true, null);
            } else {
                repositoryService.suspendProcessDefinitionById(definitionId, true, null);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("操作失败：{}", e.getMessage());
            throw new ServiceException("操作失败");
        }
    }

    /**
     * @description: 查询流程环节
     * @param: processDefinitionId
     * @return: com.ruoyi.common.core.domain.R<java.util.List < com.ruoyi.workflow.domain.vo.ActProcessNodeVo>>
     * @author: gssong
     * @date: 2021/11/19
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ActProcessNodeVo> setting(String processDefinitionId) {
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        List<Process> processes = bpmnModel.getProcesses();
        List<ActProcessNodeVo> processNodeVoList = new ArrayList<>();
        Collection<FlowElement> elements = processes.get(0).getFlowElements();
        //获取开始节点后第一个节点
        ActProcessNodeVo firstNode = new ActProcessNodeVo();
        for (FlowElement element : elements) {
            if (element instanceof StartEvent) {
                List<SequenceFlow> outgoingFlows = ((StartEvent) element).getOutgoingFlows();
                for (SequenceFlow outgoingFlow : outgoingFlows) {
                    FlowElement flowElement = outgoingFlow.getTargetFlowElement();
                    if (flowElement instanceof UserTask) {
                        firstNode.setNodeId(flowElement.getId());
                        firstNode.setNodeName(flowElement.getName());
                        firstNode.setProcessDefinitionId(processDefinitionId);
                        firstNode.setNodeType(ActConstant.USER_TASK);
                        firstNode.setIndex(0);
                    }
                }
            }
        }
        processNodeVoList.add(firstNode);
        for (FlowElement element : elements) {
            ActProcessNodeVo actProcessNodeVo = new ActProcessNodeVo();
            if (element instanceof UserTask && !firstNode.getNodeId().equals(element.getId())) {
                actProcessNodeVo.setNodeId(element.getId());
                actProcessNodeVo.setNodeName(element.getName());
                actProcessNodeVo.setProcessDefinitionId(processDefinitionId);
                actProcessNodeVo.setNodeType(ActConstant.USER_TASK);
                actProcessNodeVo.setIndex(1);
                processNodeVoList.add(actProcessNodeVo);
            } else if (element instanceof SubProcess) {
                Collection<FlowElement> flowElements = ((SubProcess) element).getFlowElements();
                for (FlowElement flowElement : flowElements) {
                    ActProcessNodeVo actProcessNode = new ActProcessNodeVo();
                    if (flowElement instanceof UserTask && !firstNode.getNodeId().equals(flowElement.getId())) {
                        actProcessNode.setNodeId(flowElement.getId());
                        actProcessNode.setNodeName(flowElement.getName());
                        actProcessNode.setProcessDefinitionId(processDefinitionId);
                        actProcessNode.setNodeType(ActConstant.USER_TASK);
                        actProcessNode.setIndex(1);
                        processNodeVoList.add(actProcessNode);
                    }
                }
            } else if (element instanceof EndEvent) {
                actProcessNodeVo.setNodeId(element.getId());
                actProcessNodeVo.setNodeName(element.getName());
                actProcessNodeVo.setProcessDefinitionId(processDefinitionId);
                actProcessNodeVo.setNodeType(ActConstant.END_EVENT);
                actProcessNodeVo.setIndex(1);
                processNodeVoList.add(actProcessNodeVo);
            }
        }
        ActProcessNodeVo actProcessNodeVo = processNodeVoList.get(0);
        ActNodeAssignee actNodeAssignee = new ActNodeAssignee();
        actNodeAssignee.setProcessDefinitionId(processDefinitionId);
        actNodeAssignee.setNodeId(actProcessNodeVo.getNodeId());
        actNodeAssignee.setNodeName(actProcessNodeVo.getNodeName());
        actNodeAssignee.setIsShow(false);
        actNodeAssignee.setIsBack(true);
        actNodeAssignee.setMultiple(false);
        actNodeAssignee.setIndex(0);
        ActNodeAssignee info = iActNodeAssigneeService.getInfo(actProcessNodeVo.getProcessDefinitionId(), actProcessNodeVo.getNodeId());
        if (ObjectUtil.isEmpty(info)) {
            iActNodeAssigneeService.add(actNodeAssignee);
        }
        //按照画图x坐标排序
        for (ActProcessNodeVo processNodeVo : processNodeVoList) {
            GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(processNodeVo.getNodeId());
            processNodeVo.setX(graphicInfo.getX());
        }
        for (ActProcessNodeVo node : processNodeVoList) {
            if (ActConstant.END_EVENT.equals(node.getNodeType())) {
                FlowElement flowElement = bpmnModel.getFlowElement(node.getNodeId());

                List<SequenceFlow> incomingFlows = ((FlowNode) flowElement).getIncomingFlows();
                for (SequenceFlow incomingFlow : incomingFlows) {
                    FlowElement sourceFlowElement = incomingFlow.getSourceFlowElement();
                    if (sourceFlowElement instanceof ParallelGateway) {
                        List<SequenceFlow> parallelGatewayOutgoingFlow = ((ParallelGateway) sourceFlowElement).getOutgoingFlows();
                        for (SequenceFlow sequenceFlow : parallelGatewayOutgoingFlow) {
                            FlowElement element = sequenceFlow.getTargetFlowElement();
                            if (element instanceof UserTask) {
                                node.setEnd(true);
                            }
                        }
                    } else if (sourceFlowElement instanceof InclusiveGateway) {
                        List<SequenceFlow> inclusiveGatewayOutgoingFlow = ((InclusiveGateway) sourceFlowElement).getOutgoingFlows();
                        for (SequenceFlow sequenceFlow : inclusiveGatewayOutgoingFlow) {
                            FlowElement element = sequenceFlow.getTargetFlowElement();
                            if (element instanceof UserTask) {
                                node.setEnd(true);
                            }
                        }
                    } else if (sourceFlowElement instanceof ExclusiveGateway) {
                        List<SequenceFlow> exclusiveGatewayOutgoingFlow = ((ExclusiveGateway) sourceFlowElement).getOutgoingFlows();
                        for (SequenceFlow sequenceFlow : exclusiveGatewayOutgoingFlow) {
                            FlowElement element = sequenceFlow.getTargetFlowElement();
                            if (element instanceof UserTask) {
                                node.setEnd(true);
                            }
                        }
                    } else if (sourceFlowElement instanceof UserTask) {
                        processNodeVoList.stream().filter(e -> e.getNodeId().equals(sourceFlowElement.getId())).findFirst().ifPresent(e -> e.setEnd(true));
                    }
                }
            }
        }

        processNodeVoList.removeIf(e -> ActConstant.END_EVENT.equals(e.getNodeType()));
        return processNodeVoList.stream().sorted(Comparator.comparing(ActProcessNodeVo::getX)).collect(Collectors.toList());
    }

    /**
     * @description: 迁移流程定义
     * @param: currentProcessDefinitionId 当前流程定义id
     * @param: fromProcessDefinitionId 需要迁移到的流程定义id
     * @return: java.lang.Boolean
     * @author: gssong
     * @date: 2022/8/22 13:11
     */
    @Override
    public Boolean migrationProcessDefinition(String currentProcessDefinitionId, String fromProcessDefinitionId) {
        try {
            ProcessMigrationService processMigrationService = processEngine.getProcessMigrationService();
            // 迁移验证
            boolean migrationValid = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(currentProcessDefinitionId)
                .validateMigrationOfProcessInstances(fromProcessDefinitionId)
                .isMigrationValid();
            if (!migrationValid) {
                throw new ServiceException("流程定义差异过大无法迁移，请修改流程图");
            }
            // 已结束的流程实例不会迁移
            processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(currentProcessDefinitionId)
                .migrateProcessInstances(fromProcessDefinitionId);
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
        return true;
    }
}
