package org.ruoyi.domain.bo.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Course QA request.
 */
@Data
public class CourseAskBo {

    @NotNull(message = "知识库ID不能为空")
    private Long knowledgeId;

    private Long sessionId;

    @NotBlank(message = "模型不能为空")
    private String model;

    @NotBlank(message = "问题不能为空")
    private String question;

    private Integer maxResults;

    private Double minScore;
}
