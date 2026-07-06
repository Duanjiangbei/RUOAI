package org.ruoyi.config;

import org.ruoyi.common.chat.service.workFlow.IWorkFlowStarterService;
import org.ruoyi.service.chat.impl.DisabledWorkFlowStarterService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Supplies a workflow service fallback for builds that exclude ruoyi-aiflow.
 */
@Configuration
public class WorkflowFallbackConfig {

    @Bean
    @ConditionalOnMissingBean(IWorkFlowStarterService.class)
    public IWorkFlowStarterService disabledWorkFlowStarterService() {
        return new DisabledWorkFlowStarterService();
    }
}
