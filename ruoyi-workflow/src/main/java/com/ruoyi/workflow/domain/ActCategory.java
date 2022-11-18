package com.ruoyi.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程分类对象 act_category
 *
 * @author gssong
 * @date 2022-11-01
 */

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("act_category")
public class ActCategory extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 父级id
     */
    private Long parentId;

    /**
     * 排序
     */
    private Long orderNum;



}
