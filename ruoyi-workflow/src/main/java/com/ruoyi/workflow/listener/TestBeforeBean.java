package com.ruoyi.workflow.listener;

import com.ruoyi.common.utils.spring.SpringUtils;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @description: 测试
 * @author: gssong
 * @date: 2022-06-26
 */
@Component
public class TestBeforeBean implements FlowBeforeHandler{
    private static final Logger logger = LoggerFactory.getLogger(TestBeforeBean.class);

    @Override
    public void handleProcess(String processInstanceId, String taskId) {
        TaskService taskService = SpringUtils.getBean(TaskService.class);
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        logger.info("taskName:"+task.getName());
    }
}
