package com.zhuweitung.analysis.jieba;

import lombok.Data;

/**
 * @author zhuweidong
 * @create 2021/4/23
 */
@Data
public class Keyword implements Comparable<Keyword> {

    private double score;
    private String name;

    public Keyword(double score, String name) {
        this.score = score;
        this.name = name;
    }

    @Override
    public int compareTo(Keyword o) {
        return o.score - this.score > 0 ? 1 : -1;
    }
}
