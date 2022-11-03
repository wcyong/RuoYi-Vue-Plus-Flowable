package com.ruoyi.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.utils.TreeBuildUtils;
import com.ruoyi.workflow.domain.ActCategory;
import com.ruoyi.workflow.mapper.ActCategoryMapper;
import com.ruoyi.workflow.service.IActCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *  流程分类对象 Service业务层处理
 *
 * @author gssong
 * @date 2021-10-10
 */
@Service
@RequiredArgsConstructor
public class ActCategoryServiceImpl extends ServiceImpl<ActCategoryMapper, ActCategory> implements IActCategoryService {

    private final ActCategoryMapper actCategoryMapper;

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
    public Boolean add(ActCategory actCategory) {
        return actCategoryMapper.insert(actCategory)>0;
    }

    @Override
    public Boolean update(ActCategory actCategory) {
        return actCategoryMapper.updateById(actCategory)>0;
    }

    @Override
    public Boolean deleteById(Long id) {
        return actCategoryMapper.deleteById(id)>0;
    }

    @Override
    public ActCategory queryById(Long id) {
        return actCategoryMapper.selectById(id);
    }
}
