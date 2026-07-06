-- Course material QA assignment extension.
-- Run this after the base ruoyi-ai-v3_mysql8.sql schema.

-- Compatibility fix for older demo databases that were created before model_dimension was added.
SET @has_model_dimension := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'chat_model'
    AND column_name = 'model_dimension'
);
SET @add_model_dimension := IF(
  @has_model_dimension = 0,
  'ALTER TABLE `chat_model` ADD COLUMN `model_dimension` int NULL DEFAULT NULL COMMENT ''model dimension'' AFTER `model_show`',
  'SELECT 1'
);
PREPARE stmt_add_model_dimension FROM @add_model_dimension;
EXECUTE stmt_add_model_dimension;
DEALLOCATE PREPARE stmt_add_model_dimension;

UPDATE `chat_model`
SET `model_dimension` = 1024
WHERE `category` = 'vector'
  AND `model_name` = 'text-embedding-v4'
  AND `model_dimension` IS NULL;

ALTER TABLE `knowledge_attach`
  ADD COLUMN `status` tinyint DEFAULT 0 COMMENT '解析状态: 0待解析, 1解析中, 2已解析, 3解析失败',
  ADD COLUMN `course_name` varchar(100) DEFAULT NULL COMMENT '课程名称',
  ADD COLUMN `course_code` varchar(50) DEFAULT NULL COMMENT '课程编号',
  ADD COLUMN `material_type` varchar(50) DEFAULT NULL COMMENT '资料类型',
  ADD COLUMN `term` varchar(50) DEFAULT NULL COMMENT '学期',
  ADD COLUMN `tags` varchar(255) DEFAULT NULL COMMENT '关键词标签',
  ADD COLUMN `audit_status` tinyint DEFAULT 0 COMMENT '审核状态: 0待审核 1通过 2驳回',
  ADD COLUMN `audit_by` bigint DEFAULT NULL COMMENT '审核人',
  ADD COLUMN `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  ADD COLUMN `reject_reason` varchar(255) DEFAULT NULL COMMENT '驳回原因';

ALTER TABLE `knowledge_info`
  ADD COLUMN `similarity_threshold` double DEFAULT 0 COMMENT '相似度阈值',
  ADD COLUMN `enable_hybrid` tinyint DEFAULT 0 COMMENT '是否启用混合检索: 0否 1是',
  ADD COLUMN `hybrid_alpha` double DEFAULT 0.5 COMMENT '混合检索权重比例',
  ADD COLUMN `enable_rerank` tinyint DEFAULT 0 COMMENT '是否启用重排序: 0否 1是',
  ADD COLUMN `rerank_model` varchar(100) DEFAULT NULL COMMENT '重排序模型名称',
  ADD COLUMN `rerank_top_n` int DEFAULT NULL COMMENT '重排序后返回的文档数量',
  ADD COLUMN `rerank_score_threshold` double DEFAULT 0 COMMENT '重排序相关性分数阈值';

ALTER TABLE `knowledge_fragment`
  ADD COLUMN `knowledge_id` bigint DEFAULT NULL COMMENT '知识库ID';

DROP TABLE IF EXISTS `course_qa_log`;
CREATE TABLE `course_qa_log` (
  `id` bigint NOT NULL COMMENT '主键',
  `knowledge_id` bigint NOT NULL COMMENT '知识库ID',
  `session_id` bigint DEFAULT NULL COMMENT '会话ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `model_name` varchar(100) DEFAULT NULL COMMENT '问答模型',
  `question` text NOT NULL COMMENT '用户问题',
  `answer` longtext COMMENT '系统答案',
  `refused` tinyint DEFAULT 0 COMMENT '是否拒答: 0否 1是',
  `refuse_reason` varchar(255) DEFAULT NULL COMMENT '拒答原因',
  `citation_json` longtext COMMENT '引用片段JSON',
  `top_score` double DEFAULT NULL COMMENT '最高检索分数',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_course_qa_log_knowledge` (`knowledge_id`),
  KEY `idx_course_qa_log_user` (`user_id`),
  KEY `idx_course_qa_log_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程资料问答日志';

DROP TABLE IF EXISTS `course_qa_feedback`;
CREATE TABLE `course_qa_feedback` (
  `id` bigint NOT NULL COMMENT '主键',
  `qa_log_id` bigint NOT NULL COMMENT '问答记录ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `feedback_type` tinyint NOT NULL COMMENT '反馈类型: 1有帮助 2答案错误 3引用错误 4无关',
  `content` varchar(500) DEFAULT NULL COMMENT '反馈内容',
  `handle_status` tinyint DEFAULT 0 COMMENT '处理状态: 0未处理 1已处理',
  `handle_by` bigint DEFAULT NULL COMMENT '处理人',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `handle_remark` varchar(500) DEFAULT NULL COMMENT '处理备注',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_course_qa_feedback_log` (`qa_log_id`),
  KEY `idx_course_qa_feedback_status` (`handle_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程资料问答反馈';
