package com.ruoyi.workflow.domain.bo;

import lombok.Data;

import java.io.Serializable;

/**
 * @description: 流程实例作废请求对象
 * @author: gssong
 * @date: 2022/10/26 19:54
 */
@Data
public class ProcessInstBo implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 流程实例id
     */
    private String processInstId;

    /**
     * 作废原因
     */
    private String deleteReason;
}
