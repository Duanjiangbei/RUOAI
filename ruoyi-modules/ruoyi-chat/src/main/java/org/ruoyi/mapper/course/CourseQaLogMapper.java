package org.ruoyi.mapper.course;

import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.domain.entity.course.CourseQaLog;
import org.ruoyi.domain.vo.course.CourseQaLogVo;

/**
 * Course QA log mapper.
 */
@Mapper
public interface CourseQaLogMapper extends BaseMapperPlus<CourseQaLog, CourseQaLogVo> {
}
