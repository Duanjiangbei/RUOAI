package org.ruoyi.domain.entity.course;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * Course QA log.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course_qa_log")
public class CourseQaLog extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long knowledgeId;

    private Long sessionId;

    private Long userId;

    private String modelName;

    private String question;

    private String answer;

    private Integer refused;

    private String refuseReason;

    private String citationJson;

    private Double topScore;

    private String remark;
}
