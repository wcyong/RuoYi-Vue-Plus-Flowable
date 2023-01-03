package com.ruoyi.workflow.flowable.cmd;

import com.ruoyi.common.utils.spring.SpringUtils;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;

import java.io.Serializable;
import java.util.Map;

/**
 * @description: 校验流程变量
 * @author: gssong
 * @date: 2022/8/22 18:26
 */
public class ExpressCheckCmd implements Command<Boolean>, Serializable {

    private final String conditionExpression;

    private final Map<String, Object> variableMap;

    public ExpressCheckCmd(String conditionExpression, Map<String, Object> variableMap) {
        this.conditionExpression = conditionExpression;
        this.variableMap = variableMap;
    }

    @Override
    public Boolean execute(CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = SpringUtils.getBean(ProcessEngineConfigurationImpl.class);
        Expression expression = processEngineConfiguration.getExpressionManager().createExpression(this.conditionExpression);
        DelegateExecution delegateExecution = new ExecutionEntityImpl();
        delegateExecution.setTransientVariables(variableMap);
        Object result = expression.getValue(delegateExecution);
        return (Boolean) result;
    }

}
