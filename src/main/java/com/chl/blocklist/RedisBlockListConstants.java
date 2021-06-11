package com.chl.blocklist;

/**
 * @author hailongchen
 */
public class RedisBlockListConstants {

    public static final String BLOCK_LIST_PREFIX = "BLOCK_LIST:";
    /**
     * 正在处理队列
     */
    public static final String PROCESSING_SUFFIX = ":PROCESSING";

    /**
     * 检查 正在执行队列 前，作为排他锁
     */
    public static final String CHECK_PROCESSING_SUFFIX = ":PROCESSING:CHECK";

    /**
     * 阻塞队列中val当前总数量
     */
    public static final String COUNT_SUFFIX = ":COUNT";

    /**
     *
     */
    public static final String SUCCESS_COUNT_SUFFIX = ":SUCCESS_COUNT";

    /**
     *
     */
    public static final String FAIL_COUNT_SUFFIX = ":FAIL_COUNT";

    /**
     * 阻塞队列 本周期vals处理完成
     */
    public static final String DONE_SUFFIX = ":DONE";

    public static final String DONE_VALUE = "1";

    public static final Integer HOUR12 = 12 * 60 * 60;
}
