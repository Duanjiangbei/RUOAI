# RUOAI

RUOAI 是一个面向校园课程资料场景的智能检索与问答系统。项目基于 RuoYi-AI 进行二次开发，重点围绕课程资料上传、审核、文档解析、知识库检索、RAG 问答、引用展示、历史记录和用户反馈等流程进行改造。

本仓库用于课程实践与系统演示，README 内容已按本项目实际功能重写，不沿用原项目介绍。

## 项目定位

在课程学习和教学管理中，课件、实验指导书、作业要求、课程说明等资料经常分散在不同文件中，学生查找信息成本较高。RUOAI 的目标是把这些资料统一上传、解析、入库，并通过大模型和向量检索能力提供可追溯的问答服务。

系统回答问题时会优先检索课程资料片段，并在答案中返回引用来源，减少无依据回答。对于资料不足、相似度较低或疑似提示注入的问题，系统会进行拒答或限制回答范围。

## 主要功能

- 课程资料上传与元数据管理：支持课程名称、课程编号、资料类型、学期、标签等字段。
- 资料审核流程：只有审核通过的资料会进入课程资料问答依据范围。
- 文档解析与切片：支持 PDF、Word、Excel、文本等资料解析，并生成知识片段。
- 向量检索与混合检索：支持 Weaviate、Milvus、Qdrant 等向量库，并增强相似度过滤、混合检索和重排序。
- 课程资料问答：新增 `/course/qa/ask` 接口，基于已审核资料生成答案。
- 引用来源展示：问答结果返回资料片段、来源文件、课程信息和相似度分数。
- 问答历史记录：保存问题、答案、引用片段、是否拒答、拒答原因等信息。
- 用户反馈处理：支持对答案质量、引用准确性等进行反馈和后台处理。
- 部署演示适配：补充 Docker、Nginx、前端演示覆盖文件，方便本地演示。

## 核心改造点

### 后端业务模块

新增课程问答相关接口、实体、Mapper 和 Service：

```text
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/controller/course/
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/domain/bo/course/
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/domain/entity/course/
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/domain/vo/course/
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/mapper/course/
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/service/course/
```

### 知识库增强

对知识库附件、知识片段、资料审核和文档解析流程进行了扩展：

```text
KnowledgeAttachController
KnowledgeAttachServiceImpl
KnowledgeFragmentServiceImpl
WordLoader
FailedDocAttachRetryRunner
```

### RAG 检索增强

重点修改：

```text
ChatServiceFacade
KnowledgeRetrievalServiceImpl
MilvusVectorStoreStrategy
QdrantVectorStoreStrategy
WeaviateVectorStoreStrategy
```

### 数据库脚本

课程资料问答扩展脚本位于：

```text
docs/script/sql/update/course_material_qa.sql
```

脚本中新增或扩展了课程问答日志、反馈、资料审核、检索参数等相关表结构。

## 技术栈

- 后端：Spring Boot、MyBatis-Plus、Sa-Token
- AI 能力：LangChain4j、大模型接口、Embedding 模型、Rerank 模型
- 数据存储：MySQL、Redis
- 向量数据库：Weaviate、Milvus、Qdrant
- 对象存储：MinIO / OSS 适配
- 部署：Docker Compose、Nginx

## 快速启动

项目保留 Docker 部署配置，演示环境主要使用：

```text
docs/docker/ruoyi-ai/docker-compose.yaml
docs/docker/ruoyi-ai/docker-compose-all.yaml
```

常用启动方式：

```bash
cd docs/docker/ruoyi-ai
docker compose -f docker-compose-all.yaml up -d
```

如果从源码构建后端，请在仓库根目录执行对应 Docker Compose 配置中指定的构建流程。

## 演示说明

演示相关前端覆盖文件和静态资源位于：

```text
output/
```

其中包含课程资料问答入口、引用展示、知识库上传增强、Nginx 配置等演示辅助文件。

## 开源来源

本项目基于开源项目 RuoYi-AI 进行课程实践改造，原项目地址：

```text
https://github.com/ageerle/ruoyi-ai
```

本仓库仅保留与 RUOAI 实践项目相关的说明内容。
