package com.ruoyi.workflow.listener;

import com.ruoyi.common.utils.spring.SpringUtils;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description: 测试
 * @author: gssong
 * @date: 2022-06-26
 */
@Component
public class TestAfterBean implements FlowAfterHandler{
    private static final Logger logger = LoggerFactory.getLogger(TestAfterBean.class);


    @Override
    public void handleProcess(String processInstanceId) {
        TaskService taskService = SpringUtils.getBean(TaskService.class);
        List<Task> list = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
        for (Task task : list) {
            logger.info("taskName:"+task.getName());
        }
    }
}
