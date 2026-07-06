package org.ruoyi.controller.course;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.domain.bo.course.CourseAskBo;
import org.ruoyi.domain.bo.course.CourseFeedbackHandleBo;
import org.ruoyi.domain.bo.course.CourseQaFeedbackBo;
import org.ruoyi.domain.bo.course.CourseQaLogBo;
import org.ruoyi.domain.vo.course.CourseAskVo;
import org.ruoyi.domain.vo.course.CourseQaFeedbackVo;
import org.ruoyi.domain.vo.course.CourseQaLogVo;
import org.ruoyi.service.course.ICourseQaService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Course material QA.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/course/qa")
public class CourseQaController extends BaseController {

    private final ICourseQaService courseQaService;

    /**
     * Ask a question against approved course material.
     */
    @RepeatSubmit()
    @PostMapping("/ask")
    public R<CourseAskVo> ask(@Validated @RequestBody CourseAskBo bo) {
        return R.ok(courseQaService.ask(bo));
    }

    /**
     * QA history.
     */
    @GetMapping("/history/list")
    public TableDataInfo<CourseQaLogVo> historyList(CourseQaLogBo bo, PageQuery pageQuery) {
        return courseQaService.queryLogPageList(bo, pageQuery);
    }

    @GetMapping("/history/{id}")
    public R<CourseQaLogVo> historyInfo(@PathVariable Long id) {
        return R.ok(courseQaService.queryLogById(id));
    }

    @Log(title = "课程问答历史", businessType = BusinessType.EXPORT)
    @PostMapping("/history/export")
    public void historyExport(CourseQaLogBo bo, HttpServletResponse response) {
        List<CourseQaLogVo> list = courseQaService.queryLogList(bo);
        ExcelUtil.exportExcel(list, "课程问答历史", CourseQaLogVo.class, response);
    }

    @Log(title = "课程问答历史", businessType = BusinessType.DELETE)
    @DeleteMapping("/history/{ids}")
    public R<Void> historyRemove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(courseQaService.deleteLogByIds(List.of(ids)));
    }

    /**
     * Submit answer feedback.
     */
    @RepeatSubmit()
    @PostMapping("/feedback")
    public R<Void> submitFeedback(@Validated @RequestBody CourseQaFeedbackBo bo) {
        return toAjax(courseQaService.submitFeedback(bo));
    }

    /**
     * Feedback records.
     */
    @GetMapping("/feedback/list")
    public TableDataInfo<CourseQaFeedbackVo> feedbackList(CourseQaFeedbackBo bo, PageQuery pageQuery) {
        return courseQaService.queryFeedbackPageList(bo, pageQuery);
    }

    @GetMapping("/feedback/{id}")
    public R<CourseQaFeedbackVo> feedbackInfo(@PathVariable Long id) {
        return R.ok(courseQaService.queryFeedbackById(id));
    }

    @Log(title = "课程问答反馈", businessType = BusinessType.UPDATE)
    @PutMapping("/feedback/handle/{id}")
    public R<Void> handleFeedback(@PathVariable Long id, @Validated @RequestBody CourseFeedbackHandleBo bo) {
        return toAjax(courseQaService.handleFeedback(id, bo));
    }
}
