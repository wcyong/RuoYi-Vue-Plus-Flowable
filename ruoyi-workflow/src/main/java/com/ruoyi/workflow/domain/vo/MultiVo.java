package com.ruoyi.workflow.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @description: MultiVo
 * @author: gssong
 * @date: 2022/04/20 21:22
 */
@Data
public class MultiVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会签类型（串行，并行）
     */
    private Object type;

    /**
     * 会签人员KEY
     */
    private String assignee;

    /**
     * 会签人员集合KEY
     */
    private String assigneeList;
}
