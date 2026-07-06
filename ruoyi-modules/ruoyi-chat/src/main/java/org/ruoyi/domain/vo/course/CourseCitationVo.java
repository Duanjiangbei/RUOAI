package org.ruoyi.domain.vo.course;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Citation returned by course QA.
 */
@Data
public class CourseCitationVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;

    private String docId;

    private Long knowledgeId;

    private Integer idx;

    private String content;

    private Double score;

    private String sourceName;

    private String courseName;

    private String materialType;

    private String tags;
}
