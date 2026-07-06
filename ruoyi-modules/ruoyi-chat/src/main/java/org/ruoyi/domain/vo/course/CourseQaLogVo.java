package org.ruoyi.domain.vo.course;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.entity.course.CourseQaLog;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Course QA log view object.
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = CourseQaLog.class)
public class CourseQaLogVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "主键")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ExcelProperty(value = "知识库ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long knowledgeId;

    @ExcelProperty(value = "会话ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sessionId;

    @ExcelProperty(value = "用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @ExcelProperty(value = "模型")
    private String modelName;

    @ExcelProperty(value = "问题")
    private String question;

    @ExcelProperty(value = "答案")
    private String answer;

    @ExcelProperty(value = "是否拒答")
    private Integer refused;

    @ExcelProperty(value = "拒答原因")
    private String refuseReason;

    private String citationJson;

    @ExcelProperty(value = "最高分")
    private Double topScore;

    @ExcelProperty(value = "创建时间")
    private Date createTime;

    private String remark;
}
