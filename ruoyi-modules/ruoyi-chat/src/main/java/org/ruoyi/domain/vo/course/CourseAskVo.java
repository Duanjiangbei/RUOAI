package org.ruoyi.domain.vo.course;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Course QA response.
 */
@Data
public class CourseAskVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long qaLogId;

    private Boolean refused;

    private String refuseReason;

    private String answer;

    private List<CourseCitationVo> citations;
}
