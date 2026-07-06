package org.ruoyi.domain.bo.knowledge;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Material audit request.
 */
@Data
public class KnowledgeAttachAuditBo {

    /**
     * Audit status: 1 approved, 2 rejected.
     */
    @NotNull(message = "审核状态不能为空")
    private Integer auditStatus;

    /**
     * Reject reason.
     */
    private String rejectReason;
}
