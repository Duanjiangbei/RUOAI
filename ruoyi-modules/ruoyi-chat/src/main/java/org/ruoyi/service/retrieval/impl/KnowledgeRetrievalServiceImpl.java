package org.ruoyi.service.retrieval.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.domain.bo.rerank.RerankRequest;
import org.ruoyi.domain.bo.rerank.RerankResult;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.entity.knowledge.KnowledgeFragment;
import org.ruoyi.domain.vo.knowledge.KnowledgeFragmentVo;
import org.ruoyi.domain.vo.knowledge.KnowledgeRetrievalVo;
import org.ruoyi.factory.RerankModelFactory;
import org.ruoyi.mapper.knowledge.KnowledgeFragmentMapper;
import org.ruoyi.service.rerank.RerankModelService;
import org.ruoyi.service.retrieval.KnowledgeRetrievalService;
import org.ruoyi.service.vector.VectorStoreService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalServiceImpl implements KnowledgeRetrievalService {

    private static final int RERANK_EXPANSION_FACTOR = 3;

    private final VectorStoreService vectorStoreService;
    private final RerankModelFactory rerankModelFactory;
    private final KnowledgeFragmentMapper fragmentMapper;

    @Override
    public List<String> retrieveTexts(QueryVectorBo queryVectorBo) {
        List<KnowledgeRetrievalVo> results = retrieve(queryVectorBo);
        return results.stream()
            .map(KnowledgeRetrievalVo::getContent)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeRetrievalVo> retrieve(QueryVectorBo queryVectorBo) {
        log.info("Start knowledge retrieval, kid={}, query={}", queryVectorBo.getKid(), queryVectorBo.getQuery());

        List<KnowledgeRetrievalVo> coarseResults = performCoarseRetrieval(queryVectorBo);
        log.info("Knowledge retrieval coarse result count: {}", coarseResults.size());

        if (coarseResults.isEmpty()) {
            coarseResults = fallbackToStoredFragments(queryVectorBo);
            log.info("Vector retrieval returned empty; fallback fragment count: {}", coarseResults.size());
            if (coarseResults.isEmpty()) {
                return coarseResults;
            }
        }

        for (int i = 0; i < coarseResults.size(); i++) {
            coarseResults.get(i).setOriginalIndex(i);
        }

        List<KnowledgeRetrievalVo> finalResults = coarseResults;
        if (Boolean.TRUE.equals(queryVectorBo.getEnableRerank())
            && StringUtils.isNotBlank(queryVectorBo.getRerankModelName())) {
            finalResults = performRerank(queryVectorBo, coarseResults);
        }

        double threshold = queryVectorBo.getRerankScoreThreshold() != null
            ? queryVectorBo.getRerankScoreThreshold()
            : 0.0;

        return finalResults.stream()
            .filter(res -> res.getScore() == null || res.getScore() >= threshold)
            .collect(Collectors.toList());
    }

    private List<KnowledgeRetrievalVo> performCoarseRetrieval(QueryVectorBo queryVectorBo) {
        int originalMaxResults = queryVectorBo.getMaxResults() != null ? queryVectorBo.getMaxResults() : 10;
        int targetMaxResults = originalMaxResults;
        if (Boolean.TRUE.equals(queryVectorBo.getEnableRerank())
            && StringUtils.isNotBlank(queryVectorBo.getRerankModelName())) {
            targetMaxResults = originalMaxResults * RERANK_EXPANSION_FACTOR;
        }

        if (!Boolean.TRUE.equals(queryVectorBo.getEnableHybrid())) {
            List<KnowledgeRetrievalVo> results = vectorStoreService.search(copyOf(queryVectorBo, targetMaxResults));
            return filterBySimilarity(queryVectorBo, results);
        }

        log.info("Run hybrid retrieval, kid={}, query={}", queryVectorBo.getKid(), queryVectorBo.getQuery());
        try {
            int finalTargetMaxResults = targetMaxResults;
            CompletableFuture<List<KnowledgeRetrievalVo>> vectorFuture = CompletableFuture.supplyAsync(() ->
                filterBySimilarity(queryVectorBo, vectorStoreService.search(copyOf(queryVectorBo, finalTargetMaxResults)))
            );

            CompletableFuture<List<KnowledgeRetrievalVo>> keywordFuture = CompletableFuture.supplyAsync(() ->
                searchByKeyword(queryVectorBo, finalTargetMaxResults)
            );

            return calculateRRF(vectorFuture.get(), keywordFuture.get(), queryVectorBo.getHybridAlpha());
        } catch (Exception e) {
            log.error("Hybrid retrieval failed, fallback to vector retrieval: {}", e.getMessage(), e);
            return filterBySimilarity(queryVectorBo, vectorStoreService.search(copyOf(queryVectorBo, targetMaxResults)));
        }
    }

    private List<KnowledgeRetrievalVo> filterBySimilarity(QueryVectorBo queryVectorBo, List<KnowledgeRetrievalVo> results) {
        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }
        if (queryVectorBo.getSimilarityThreshold() == null) {
            return results;
        }
        return results.stream()
            .filter(r -> r.getScore() == null || r.getScore() >= queryVectorBo.getSimilarityThreshold())
            .collect(Collectors.toList());
    }

    private List<KnowledgeRetrievalVo> searchByKeyword(QueryVectorBo queryVectorBo, int limit) {
        try {
            Long kid = Long.valueOf(queryVectorBo.getKid());
            List<KnowledgeFragmentVo> fragments = fragmentMapper.searchByKeyword(kid, queryVectorBo.getQuery(), limit);
            return fragments.stream().map(f -> KnowledgeRetrievalVo.builder()
                .id(String.valueOf(f.getId()))
                .docId(f.getDocId())
                .knowledgeId(f.getKnowledgeId())
                .idx(f.getIdx())
                .content(f.getContent())
                .score(10.0)
                .build()
            ).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Keyword retrieval failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<KnowledgeRetrievalVo> fallbackToStoredFragments(QueryVectorBo queryVectorBo) {
        try {
            Long kid = Long.valueOf(queryVectorBo.getKid());
            int limit = queryVectorBo.getMaxResults() != null ? queryVectorBo.getMaxResults() : 3;
            List<KnowledgeFragment> fragments = fragmentMapper.selectList(
                Wrappers.<KnowledgeFragment>lambdaQuery()
                    .eq(KnowledgeFragment::getKnowledgeId, kid)
                    .orderByAsc(KnowledgeFragment::getIdx)
                    .last("LIMIT " + Math.max(1, limit))
            );
            return fragments.stream().map(fragment -> KnowledgeRetrievalVo.builder()
                .id(String.valueOf(fragment.getId()))
                .docId(fragment.getDocId())
                .knowledgeId(fragment.getKnowledgeId())
                .idx(fragment.getIdx())
                .content(fragment.getContent())
                .score(1.0)
                .build()
            ).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Fallback fragment retrieval failed: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private List<KnowledgeRetrievalVo> performRerank(QueryVectorBo queryVectorBo,
                                                     List<KnowledgeRetrievalVo> coarseResults) {
        try {
            RerankModelService rerankModel = rerankModelFactory.createModel(queryVectorBo.getRerankModelName());
            List<String> contents = coarseResults.stream()
                .map(KnowledgeRetrievalVo::getContent)
                .collect(Collectors.toList());

            int topN = queryVectorBo.getRerankTopN() != null
                ? queryVectorBo.getRerankTopN()
                : queryVectorBo.getMaxResults();

            RerankRequest rerankRequest = RerankRequest.builder()
                .query(queryVectorBo.getQuery())
                .documents(contents)
                .topN(topN)
                .build();

            RerankResult rerankResult = rerankModel.rerank(rerankRequest);
            for (RerankResult.RerankDocument doc : rerankResult.getDocuments()) {
                if (doc.getIndex() != null && doc.getIndex() < coarseResults.size()) {
                    KnowledgeRetrievalVo vo = coarseResults.get(doc.getIndex());
                    vo.setRawScore(vo.getScore());
                    vo.setScore(doc.getRelevanceScore());
                }
            }

            coarseResults.sort((a, b) -> b.getScore().compareTo(a.getScore()));
            return coarseResults.subList(0, Math.min(topN, coarseResults.size()));
        } catch (Exception e) {
            log.error("Rerank failed: {}", e.getMessage());
            int limit = queryVectorBo.getMaxResults() != null ? queryVectorBo.getMaxResults() : 10;
            return coarseResults.subList(0, Math.min(limit, coarseResults.size()));
        }
    }

    private List<KnowledgeRetrievalVo> calculateRRF(List<KnowledgeRetrievalVo> vectorList,
                                                    List<KnowledgeRetrievalVo> keywordList,
                                                    Double hybridAlpha) {
        Map<String, KnowledgeRetrievalVo> allMap = new LinkedHashMap<>();
        Map<String, Double> vectorScores = new HashMap<>();
        Map<String, Double> keywordScores = new HashMap<>();
        int k = 60;

        for (int i = 0; i < vectorList.size(); i++) {
            KnowledgeRetrievalVo vo = vectorList.get(i);
            allMap.put(vo.getId(), vo);
            vectorScores.put(vo.getId(), 1.0 / (k + i + 1));
        }

        for (int i = 0; i < keywordList.size(); i++) {
            KnowledgeRetrievalVo vo = keywordList.get(i);
            allMap.putIfAbsent(vo.getId(), vo);
            keywordScores.put(vo.getId(), 1.0 / (k + i + 1));
        }

        double alpha = hybridAlpha != null ? hybridAlpha : 0.5;
        List<KnowledgeRetrievalVo> fusedResults = new ArrayList<>();
        for (Map.Entry<String, KnowledgeRetrievalVo> entry : allMap.entrySet()) {
            String id = entry.getKey();
            double score = (1 - alpha) * vectorScores.getOrDefault(id, 0.0)
                + alpha * keywordScores.getOrDefault(id, 0.0);
            KnowledgeRetrievalVo vo = entry.getValue();
            vo.setScore(score * 60.0);
            fusedResults.add(vo);
        }

        fusedResults.sort((a, b) -> b.getScore().compareTo(a.getScore()));
        return fusedResults;
    }

    private QueryVectorBo copyOf(QueryVectorBo original, int maxResults) {
        QueryVectorBo copy = new QueryVectorBo();
        copy.setQuery(original.getQuery());
        copy.setKid(original.getKid());
        copy.setMaxResults(maxResults);
        copy.setVectorModelName(original.getVectorModelName());
        copy.setEmbeddingModelName(original.getEmbeddingModelName());
        copy.setApiKey(original.getApiKey());
        copy.setBaseUrl(original.getBaseUrl());
        copy.setSimilarityThreshold(original.getSimilarityThreshold());
        copy.setEnableHybrid(original.getEnableHybrid());
        copy.setHybridAlpha(original.getHybridAlpha());
        copy.setEnableRerank(original.getEnableRerank());
        copy.setRerankModelName(original.getRerankModelName());
        copy.setRerankTopN(original.getRerankTopN());
        copy.setRerankScoreThreshold(original.getRerankScoreThreshold());
        return copy;
    }
}
