package com.ruoyi.workflow.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workflow.domain.ActTaskNode;
import com.ruoyi.workflow.mapper.ActTaskNodeMapper;
import com.ruoyi.workflow.service.IActTaskNodeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @description: 节点名称业务层
 * @author: gssong
 * @date: 2021/11/06 16:29
 */
@Service
public class ActTaskNodeServiceImpl extends ServiceImpl<ActTaskNodeMapper, ActTaskNode> implements IActTaskNodeService {
    /**
     * @description: 按照流程实例id查询驳回节点
     * @param: processInstanceId
     * @return: java.util.List<com.ruoyi.workflow.domain.ActTaskNode>
     * @author: gssong
     * @date: 2021/10/21
     */
    @Override
    public List<ActTaskNode> getListByInstanceId(String processInstanceId) {
        LambdaQueryWrapper<ActTaskNode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ActTaskNode::getInstanceId, processInstanceId);
        queryWrapper.orderByDesc(ActTaskNode::getOrderNo);
        return this.baseMapper.selectList(queryWrapper);
    }

    /**
     * @description: 按照流程实例id与节点id查询驳回节点
     * @param: processInstanceId
     * @param: nodeId
     * @return: com.ruoyi.workflow.domain.ActTaskNode
     * @author: gssong
     * @date: 2021/10/21
     */
    @Override
    public ActTaskNode getListByInstanceIdAndNodeId(String processInstanceId, String nodeId) {
        LambdaQueryWrapper<ActTaskNode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ActTaskNode::getInstanceId, processInstanceId);
        queryWrapper.eq(ActTaskNode::getNodeId, nodeId);
        return this.baseMapper.selectOne(queryWrapper);
    }

    /**
     * @description: 删除驳回后的节点
     * @param: processInstanceId
     * @param: targetActivityId
     * @return: java.lang.Boolean
     * @author: gssong
     * @date: 2021/10/21
     */
    @Override
    public Boolean deleteBackTaskNode(String processInstanceId, String targetActivityId) {
        try {
            LambdaQueryWrapper<ActTaskNode> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ActTaskNode::getInstanceId, processInstanceId);
            queryWrapper.eq(ActTaskNode::getNodeId, targetActivityId);
            ActTaskNode actTaskNode = this.baseMapper.selectOne(queryWrapper);
            if (ObjectUtil.isNotNull(actTaskNode)) {
                Integer orderNo = actTaskNode.getOrderNo();
                List<ActTaskNode> taskNodeList = getListByInstanceId(processInstanceId);

                List<String> ids = new ArrayList<>();
                if (CollectionUtil.isNotEmpty(taskNodeList)) {
                    for (ActTaskNode taskNode : taskNodeList) {
                        if (taskNode.getOrderNo() >= orderNo) {
                            ids.add(taskNode.getId());
                        }
                    }
                }
                if (CollectionUtil.isNotEmpty(ids)) {
                    this.baseMapper.deleteBatchIds(ids);
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException("删除失败");
        }
    }

    /**
     * @description: 按照流程实例id删除
     * @param: processInstanceId
     * @return: java.lang.Boolean
     * @author: gssong
     * @date: 2021/10/21
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByInstanceId(String processInstanceId) {
        LambdaQueryWrapper<ActTaskNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActTaskNode::getInstanceId, processInstanceId);
        List<ActTaskNode> list = baseMapper.selectList(wrapper);
        int delete = baseMapper.delete(wrapper);
        if (list.size() != delete) {
            throw new ServiceException("删除失败");
        }
        return true;
    }

    /**
     * @description: 保存已审批节点
     * @param: actTaskNode
     * @return: void
     * @author: gssong
     * @date: 2021/10/21
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveTaskNode(ActTaskNode actTaskNode) {
        try {
            List<ActTaskNode> list = getListByInstanceId(actTaskNode.getInstanceId());
            if (!list.isEmpty()) {
                ActTaskNode taskNode = list.stream().filter(e -> e.getNodeId().equals(actTaskNode.getNodeId()) && e.getOrderNo() == 0).findFirst().orElse(null);
                if (ObjectUtil.isEmpty(taskNode)) {
                    LambdaQueryWrapper<ActTaskNode> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(ActTaskNode::getInstanceId, actTaskNode.getInstanceId());
                    queryWrapper.eq(ActTaskNode::getNodeId, actTaskNode.getNodeId());
                    List<ActTaskNode> nodeList = getListByInstanceId(actTaskNode.getInstanceId());
                    actTaskNode.setOrderNo(nodeList.get(0).getOrderNo()+1);
                    ActTaskNode node = getListByInstanceIdAndNodeId(actTaskNode.getInstanceId(), actTaskNode.getNodeId());
                    if(ObjectUtil.isNotEmpty(node)){
                        node.setAssignee(node.getAssignee()+","+actTaskNode.getAssignee());
                        updateById(node);
                    }else{
                        save(actTaskNode);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }
}
