package org.ruoyi.domain.bo.course;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Feedback handling request.
 */
@Data
public class CourseFeedbackHandleBo {

    @NotNull(message = "处理状态不能为空")
    private Integer handleStatus;

    private String handleRemark;
}
