package com.ruoyi.workflow.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @description: 字段属性
 * @author: gssong
 * @date: 2022-11-25
 */
@Data
public class FieldList implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 属性
     */
    private String field;

    /**
     * 是否编辑
     */
    private Boolean edit;

    /**
     * 是否隐藏
     */
    private Boolean hidden;

    /**
     * 是否必填
     */
    private Boolean required;

    /**
     * 提示信息
     */
    private String message;

}
