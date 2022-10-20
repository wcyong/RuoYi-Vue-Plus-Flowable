package com.ruoyi.workflow.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @description: 流程变量
 * @author: gssong
 * @date: 2022/07/23 14:54
 */
@Data
public class VariableVo implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 变量key
     */
    private String key;

    /**
     * 变量值
     */
    private String value;
}
