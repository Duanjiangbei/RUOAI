package org.ruoyi.service.knowledge.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.domain.entity.knowledge.KnowledgeAttach;
import org.ruoyi.enums.KnowledgeAttachStatus;
import org.ruoyi.mapper.knowledge.KnowledgeAttachMapper;
import org.ruoyi.service.knowledge.IKnowledgeAttachService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class FailedDocAttachRetryRunner implements ApplicationRunner {

    private final KnowledgeAttachMapper knowledgeAttachMapper;

    @Override
    public void run(ApplicationArguments args) {
        List<KnowledgeAttach> failedDocAttachments = knowledgeAttachMapper.selectList(
            Wrappers.<KnowledgeAttach>lambdaQuery()
                .eq(KnowledgeAttach::getType, ".doc")
                .eq(KnowledgeAttach::getStatus, KnowledgeAttachStatus.FAILED.getCode())
                .like(KnowledgeAttach::getRemark, "OLE2")
                .last("LIMIT 20")
        );
        if (failedDocAttachments.isEmpty()) {
            return;
        }

        log.info("Retrying {} failed legacy Word attachment(s)", failedDocAttachments.size());
        IKnowledgeAttachService attachService = SpringUtils.getBean(IKnowledgeAttachService.class);
        for (KnowledgeAttach attach : failedDocAttachments) {
            attachService.parse(attach.getId());
        }
    }
}
