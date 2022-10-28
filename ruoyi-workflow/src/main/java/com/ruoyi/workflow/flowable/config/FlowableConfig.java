
package com.ruoyi.workflow.flowable.config;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
/**
 * @description: 配置
 * @author: gssong
 * @date: 2021/10/03 19:31
 */
@Configuration
public class FlowableConfig implements EngineConfigurationConfigurer<SpringProcessEngineConfiguration> {

    @Value("${flowable.activity-font-name}")
    private String  activityFontName;

    @Value("${flowable.label-font-name}")
    private String  labelFontName;

    @Value("${flowable.annotation-font-name}")
    private String  annotationFontName;

    /**
     * 解決工作流生成图片乱码问题
     * @param processEngineConfiguration
     */

    @Override
    public void configure(SpringProcessEngineConfiguration processEngineConfiguration) {
        processEngineConfiguration.setActivityFontName(activityFontName);
        processEngineConfiguration.setAnnotationFontName(annotationFontName);
        processEngineConfiguration.setLabelFontName(labelFontName);
        processEngineConfiguration.setProcessDiagramGenerator(new CustomDefaultProcessDiagramGenerator());

        /**
         * 自定义id
         */
        processEngineConfiguration.setIdGenerator(() -> IdWorker.getIdStr());
    }
}

