package com.ruoyi.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.TreeBuildUtils;
import com.ruoyi.workflow.domain.ActCategory;
import com.ruoyi.workflow.mapper.ActCategoryMapper;
import com.ruoyi.workflow.service.IActCategoryService;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *  流程分类对象 Service业务层处理
 *
 * @author gssong
 * @date 2022-11-01
 */
@Service
@RequiredArgsConstructor
public class ActCategoryServiceImpl extends ServiceImpl<ActCategoryMapper, ActCategory> implements IActCategoryService {

    private final ActCategoryMapper actCategoryMapper;
    private final RepositoryService repositoryService;

    @Override
    public List<Tree<Long>> queryTreeList(ActCategory entity) {
        return buildCategoryTreeSelect(actCategoryMapper.selectList(null));
    }

    public List<Tree<Long>> buildCategoryTreeSelect(List<ActCategory> categories) {
        if (CollUtil.isEmpty(categories)) {
            return CollUtil.newArrayList();
        }
        return TreeBuildUtils.build(categories, (category, tree) ->
            tree.setId(category.getId())
                .setParentId(category.getParentId())
                .setName(category.getCategoryName())
                .setWeight(category.getOrderNum()));
    }

    @Override
    public List<ActCategory> queryList(ActCategory actCategory) {
        LambdaQueryWrapper<ActCategory> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StringUtils.isNotBlank(actCategory.getCategoryName()),ActCategory::getCategoryName,actCategory.getCategoryName());
        return actCategoryMapper.selectList(wrapper);
    }

    @Override
    public Boolean add(ActCategory actCategory) {
        return actCategoryMapper.insert(actCategory)>0;
    }

    @Override
    public Boolean update(ActCategory actCategory) {
        return actCategoryMapper.updateById(actCategory)>0;
    }

    @Override
    public Boolean deleteById(Long id) {
        LambdaQueryWrapper<ActCategory> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ActCategory::getParentId,id);
        List<ActCategory> categoryList = actCategoryMapper.selectList(wrapper);
        if(CollUtil.isNotEmpty(categoryList)){
            throw new ServiceException("该分类下存在子级无法删除");
        }
        List<Model> modelList = repositoryService.createModelQuery().modelCategory(id.toString()).list();
        if(CollUtil.isNotEmpty(modelList)){
            throw new ServiceException("该分类下存在模型无法删除");
        }
        List<ProcessDefinition> definitionList = repositoryService.createProcessDefinitionQuery().processDefinitionCategory(id.toString()).list();
        if(CollUtil.isNotEmpty(definitionList)){
            throw new ServiceException("该分类下存在流程定义无法删除");
        }
        return actCategoryMapper.deleteById(id)>0;
    }

    @Override
    public ActCategory queryByCategory(String category) {
        LambdaQueryWrapper<ActCategory> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ActCategory::getCategoryName,category);
        return actCategoryMapper.selectOne(wrapper);
    }
}
