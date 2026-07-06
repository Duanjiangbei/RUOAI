package org.ruoyi.domain.vo.course;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.entity.course.CourseQaFeedback;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Course QA feedback view object.
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = CourseQaFeedback.class)
public class CourseQaFeedbackVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "主键")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ExcelProperty(value = "问答记录ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long qaLogId;

    @ExcelProperty(value = "用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @ExcelProperty(value = "反馈类型")
    private Integer feedbackType;

    @ExcelProperty(value = "反馈内容")
    private String content;

    @ExcelProperty(value = "处理状态")
    private Integer handleStatus;

    private Long handleBy;

    private Date handleTime;

    private String handleRemark;

    private Date createTime;

    private String remark;
}
