package com.ruoyi.workflow.service;


import cn.hutool.core.lang.tree.Tree;
import com.ruoyi.workflow.domain.ActCategory;

import java.util.List;

/**
 * 流程分类对象 Service接口
 *
 * @author gssong
 * @date 2022-11-01
 */
public interface IActCategoryService {

    /**
     * 查询流程分类构建树形结构
     * @param entity
     * @return
     */
    List<Tree<Long>> queryTreeList(ActCategory entity);

    /**
     * 查询流程分类
     * @param actCategory
     * @return
     */
    List<ActCategory> queryList(ActCategory actCategory);

    /**
     * 新增
     * @param actCategory
     * @return
     */
    Boolean add(ActCategory actCategory);

    /**
     * 新增
     * @param actCategory
     * @return
     */
    Boolean update(ActCategory actCategory);

    /**
     * 删除
     * @param id
     * @return
     */
    Boolean deleteById(Long id);

    /**
     * 查询
     * @param category
     * @return
     */
    ActCategory queryByCategory(String category);
}
