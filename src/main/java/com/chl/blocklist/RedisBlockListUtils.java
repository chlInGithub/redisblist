package com.chl.blocklist;

import static com.chl.blocklist.RedisBlockListConstants.*;

/**
 * @author hailongchen
 */
public class RedisBlockListUtils {

    /**
     * 表示阻塞队列，对应的key
     * @param originalListKey 用户自定义的队列名
     * @param env 环境标识 与使用的profile有关
     * @return
     */
    public static String getBlockListKey(String originalListKey, String env) {
        String key = BLOCK_LIST_PREFIX + env + ":" + originalListKey;
        return key;
    }

    /**
     * 表示正在处理的元素构成的队列，对应的key
     * @param blockListKey  from getBlockListKey()
     * @return
     */
    public static String getProcessingKey(String blockListKey) {
        String key = blockListKey + PROCESSING_SUFFIX;
        return key;
    }

    /**
     * 检查器key
     * @param blockListKey   from getBlockListKey()
     * @return
     */
    public static String getCheckProcessingKey(String blockListKey) {
        String key = blockListKey + CHECK_PROCESSING_SUFFIX;
        return key;
    }

    /**
     * 表示阻塞队列中所有元素都已经处理完成，使用的key
     * @param blockListKey   from getBlockListKey()
     * @return
     */
    public static String getDoneKey(String blockListKey) {
        String key = blockListKey + DONE_SUFFIX;
        return key;
    }

    /**
     * 表示阻塞队列剩余元素数量，使用的key
     * @param blockListKey   from getBlockListKey()
     * @return
     */
    public static String getCountKey(String blockListKey) {
        String key = blockListKey + COUNT_SUFFIX;
        return key;
    }

    public static String getSuccessCountKey(String blockListKey) {
        String key = blockListKey + SUCCESS_COUNT_SUFFIX;
        return key;
    }

    /**
     * 表示处理失败的个数，对应的key
     * @param blockListKey   from getBlockListKey()
     * @return
     */
    public static String getFailCountKey(String blockListKey) {
        String key = blockListKey + FAIL_COUNT_SUFFIX;
        return key;
    }
}
