package com.ruoyi.workflow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.workflow.domain.ActCategory;
import com.ruoyi.workflow.mapper.ActCategoryMapper;
import com.ruoyi.workflow.service.IActCategoryService;
import org.springframework.stereotype.Service;

/**
 *  流程分类对象 Service业务层处理
 *
 * @author gssong
 * @date 2021-10-10
 */
@Service
public class ActCategoryServiceImpl extends ServiceImpl<ActCategoryMapper, ActCategory> implements IActCategoryService {
}
