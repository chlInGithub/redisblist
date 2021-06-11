package com.chl.blocklist;

import lombok.Data;

/**
 * 阻塞队列 处理的当前情况
 * @author hailongchen
 */
@Data
public class RedisBlockListCurrentProcess {

    RedisBlockListConfig context;

    String done;

    /**
     * 阻塞队列中元素数量
     */
    long blockListEleCount;

    /**
     * 处理中元素数量
     */
    long processingEleCount;
}
