package org.ruoyi.domain.bo.course;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.domain.entity.course.CourseQaLog;

/**
 * Course QA log query object.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = CourseQaLog.class, reverseConvertGenerate = false)
public class CourseQaLogBo extends BaseEntity {

    private Long id;

    private Long knowledgeId;

    private Long sessionId;

    private Long userId;

    private String modelName;

    private String question;

    private Integer refused;
}
