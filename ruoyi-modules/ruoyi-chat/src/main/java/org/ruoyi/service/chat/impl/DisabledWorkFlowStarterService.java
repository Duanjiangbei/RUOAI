package org.ruoyi.service.chat.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ruoyi.common.chat.entity.User;
import org.ruoyi.common.chat.service.workFlow.IWorkFlowStarterService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Fallback workflow starter used when the workflow module is not packaged.
 */
public class DisabledWorkFlowStarterService implements IWorkFlowStarterService {

    private static final String DISABLED_MESSAGE =
        "Workflow module is disabled in this demo build; normal chat and knowledge-base QA are available.";

    @Override
    public SseEmitter streaming(User user, String workflowUuid, List<ObjectNode> userInputs, Long sessionId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitter.completeWithError(new UnsupportedOperationException(DISABLED_MESSAGE));
        return emitter;
    }

    @Override
    public void resumeFlow(String runtimeUuid, String userInput, SseEmitter sseEmitter) {
        if (sseEmitter != null) {
            sseEmitter.completeWithError(new UnsupportedOperationException(DISABLED_MESSAGE));
        }
    }
}
