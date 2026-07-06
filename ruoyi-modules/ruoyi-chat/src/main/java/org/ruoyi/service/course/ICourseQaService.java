package org.ruoyi.service.course;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.domain.bo.course.CourseAskBo;
import org.ruoyi.domain.bo.course.CourseFeedbackHandleBo;
import org.ruoyi.domain.bo.course.CourseQaFeedbackBo;
import org.ruoyi.domain.bo.course.CourseQaLogBo;
import org.ruoyi.domain.vo.course.CourseAskVo;
import org.ruoyi.domain.vo.course.CourseQaFeedbackVo;
import org.ruoyi.domain.vo.course.CourseQaLogVo;

import java.util.Collection;
import java.util.List;

/**
 * Course material QA service.
 */
public interface ICourseQaService {

    CourseAskVo ask(CourseAskBo bo);

    CourseQaLogVo queryLogById(Long id);

    TableDataInfo<CourseQaLogVo> queryLogPageList(CourseQaLogBo bo, PageQuery pageQuery);

    List<CourseQaLogVo> queryLogList(CourseQaLogBo bo);

    Boolean deleteLogByIds(Collection<Long> ids);

    Boolean submitFeedback(CourseQaFeedbackBo bo);

    Boolean handleFeedback(Long id, CourseFeedbackHandleBo bo);

    CourseQaFeedbackVo queryFeedbackById(Long id);

    TableDataInfo<CourseQaFeedbackVo> queryFeedbackPageList(CourseQaFeedbackBo bo, PageQuery pageQuery);

    List<CourseQaFeedbackVo> queryFeedbackList(CourseQaFeedbackBo bo);
}
