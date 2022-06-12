package com.mosect.smali.plugin.parser;

/**
 * 节点匹配器
 */
public interface SmaliNodeMatcher {

    /**
     * 重置匹配器
     */
    void reset();

    /**
     * 节点匹配
     *
     * @param parent 父节点
     * @param node   节点
     * @param index  节点下标
     * @return 0，不匹配；1，匹配；2，匹配结束
     */
    int match(SmaliNode parent, SmaliNode node, int index);
}
