package com.ruoyi.workflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.lang.tree.Tree;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.workflow.domain.ActCategory;
import com.ruoyi.workflow.service.IActCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程分类
 *
 * @author gssong
 * @date 2022-11-03
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/category")
public class ActCategoryController extends BaseController {

    private final IActCategoryService iActCategoryService;

    /**
     * 查询流程分类构建树形结构
     */
    @GetMapping("/queryTreeList")
    public R<List<Tree<Long>>> queryTreeList(ActCategory entity) {
        return R.ok(iActCategoryService.queryTreeList(entity));
    }

    /**
     * 查询流程分类
     */
    @SaCheckPermission("workflow:category:list")
    @GetMapping("/list")
    public R<List<ActCategory>> queryList(ActCategory entity) {
        return R.ok(iActCategoryService.queryList(entity));
    }

    /**
     * 新增流程分类
     */
    @SaCheckPermission("workflow:category:add")
    @Log(title = "流程分类", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@RequestBody ActCategory entity) {
        return toAjax(iActCategoryService.add(entity));
    }

    /**
     * 删除流程分类
     */
    @SaCheckPermission("workflow:category:remove")
    @Log(title = "流程分类", businessType = BusinessType.DELETE)
    @RepeatSubmit()
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        return toAjax(iActCategoryService.deleteById(id));
    }
}
