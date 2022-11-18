package com.ruoyi.workflow.domain.bo;


import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @description: 启动流程
 * @author: gssong
 * @date: 2021/10/10 18:43
 */
@Data
public class StartProcessBo implements Serializable {
    private static final long serialVersionUID=1L;

    /**
     * 业务唯一值id
     */
    private String businessKey;

    /**
     * 流程执行key
     */
    private String processKey;

    /**
     * 节点任务办理人一位或多位
     */
    private List<String> assignees;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 流程变量，前端会提交一个元素{'entity': {业务详情数据对象}}
     */
    private Map<String, Object> variables;

    public Map<String, Object> getVariables() {
        if(variables == null){
            return new HashMap<>(16);
        }
        variables.entrySet().removeIf(entry -> Objects.isNull(entry.getValue()));
        return variables;
    }
}
