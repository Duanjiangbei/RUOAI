package org.ruoyi.domain.bo.course;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.domain.entity.course.CourseQaFeedback;

/**
 * Course QA feedback object.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = CourseQaFeedback.class, reverseConvertGenerate = false)
public class CourseQaFeedbackBo extends BaseEntity {

    private Long id;

    @NotNull(message = "问答记录ID不能为空")
    private Long qaLogId;

    private Long userId;

    @NotNull(message = "反馈类型不能为空")
    private Integer feedbackType;

    private String content;

    private Integer handleStatus;

    private String handleRemark;
}
