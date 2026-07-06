package org.ruoyi.domain.entity.course;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Date;

/**
 * Course QA feedback.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course_qa_feedback")
public class CourseQaFeedback extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long qaLogId;

    private Long userId;

    private Integer feedbackType;

    private String content;

    private Integer handleStatus;

    private Long handleBy;

    private Date handleTime;

    private String handleRemark;

    private String remark;
}
