package com.ruoyi.workflow.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 方法参数对象
 *
 * @author gssong
 * @date 2021-12-17
 */
@Data
public class ActBusinessRuleParam implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 参数类型
     */
    private String paramType;

    /**
     * 参数
     */
    private String param;

    /**
     * 备注
     */
    private String remark;

    /**
     * 排序
     */
    private Integer orderNo;

}
