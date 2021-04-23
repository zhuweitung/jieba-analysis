package com.zhuweitung.analysis.jieba;

import com.huaban.analysis.jieba.JiebaSegmenter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 基于jieba分词实现TextRank提取摘要关键词算法
 * 参考：https://blog.csdn.net/u012998680/article/details/107713952
 *
 * @author zhuweidong
 * @create 2021/4/23
 */
public class TextRankAnalyzer {

    private static TextRankAnalyzer instance = new TextRankAnalyzer();

    private HashSet<String> stopWords;
    private JiebaSegmenter segmenter = new JiebaSegmenter();

    /**
     * 差值最小
     */
    private static float minDiff = 0.001f;
    /**
     * 最大迭代次数
     */
    private static int maxIter = 200;
    /**
     * 窗口大小
     */
    private static int k = 2;
    private static float d = 0.85f;


    public static TextRankAnalyzer getInstance() {
        return instance;
    }

    private TextRankAnalyzer() {
        //加载停用词
        if (stopWords == null) {
            synchronized (TextRankAnalyzer.class) {
                if (stopWords == null) {
                    stopWords = new HashSet<>();
                    loadStopWords(this.getClass().getResourceAsStream("/stop_words.txt"));
                }
            }
        }
    }

    /**
     * 加载停用词
     *
     * @param resourceStream
     */
    public void loadStopWords(InputStream resourceStream) {
        stopWords = new HashSet<>();
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = buffer.readLine()) != null) {
                stopWords.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * TextRank算法
     *
     * @param content 需要分析的文本
     * @param topN    取前N个关键字
     * @return java.util.List<com.zhuweitung.analysis.jieba.Keyword>
     * @author zhuweidong
     * @date 2021/4/23
     */
    public List<Keyword> analyze(String content, Integer topN) {

        //分词
        List<String> segments = segmenter.sentenceProcess(content);

        //去停用词
        List<String> words = new ArrayList<>();
        segments.forEach(token -> {
            if (token.trim().length() > 1 && !stopWords.contains(token.toLowerCase())) {
                words.add(token.trim());
            }
        });

        Map<String, Set<String>> relationWords = new HashMap<>();
        //获取每个关键词 前后k个的组合
        for (int i = 0; i < words.size(); i++) {
            String keyword = words.get(i);
            Set<String> keySets = relationWords.get(keyword);
            if (keySets == null) {
                keySets = new HashSet<>();
                relationWords.put(keyword, keySets);
            }

            for (int j = i - k; j <= i + k; j++) {
                if (j < 0 || j >= words.size() || j == i) {
                    continue;
                } else {
                    keySets.add(words.get(j));
                }
            }
        }

        Map<String, Float> score = new HashMap<>();
        //迭代
        for (int i = 0; i < maxIter; i++) {
            Map<String, Float> m = new HashMap<>();
            float maxDiff = 0;
            for (String key : relationWords.keySet()) {
                Set<String> value = relationWords.get(key);
                //先给每个关键词一个默认rank值
                m.put(key, 1 - d);
                //一个关键词的TextRank由其它成员投票出来
                for (String other : value) {
                    int size = relationWords.get(other).size();
                    if (key.equals(other) || size == 0) {
                        continue;
                    } else {
                        m.put(key, m.get(key) + d / size * (score.get(other) == null ? 0 : score.get(other)));
                    }
                }
                maxDiff = Math.max(maxDiff, Math.abs(m.get(key) - (score.get(key) == null ? 0 : score.get(key))));
            }
            score = m;
            if (maxDiff <= minDiff) {
                break;
            }
        }

        //封装
        List<Keyword> keywords = new ArrayList<>();
        for (String s : score.keySet()) {
            Keyword keyword = new Keyword(score.get(s), s);
            keywords.add(keyword);
        }
        //降序
        Collections.sort(keywords);

        //取topN个关键字
        return keywords.size() > topN ? keywords.subList(0, topN) : keywords;
    }

    public static void main(String[] args) {
        String content = "你认识那个和主席握手的的哥吗？他开一辆黑色的士。";
        int topN = 5;
        List<Keyword> list = getInstance().analyze(content, topN);
        list.forEach(word -> System.out.println(word.getName() + ":" + word.getScore()));
    }
}
