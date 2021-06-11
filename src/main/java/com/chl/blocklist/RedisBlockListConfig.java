package com.chl.blocklist;

import lombok.Data;

/**
 * 对应一个阻塞队列，包含处理过程的配置信息
 * @author chenhailong
 */
@Data
public class RedisBlockListConfig {

    /**
     * 队列对应的redis key
     */
    String blockListKey;

    /**
     * 是否判断队列中val已全部处理，默认false
     */
    boolean dealDone = false;

    /**
     * done key 保存秒数，默认12小时
     */
    Integer doneExpire = RedisBlockListConstants.HOUR12;

    /**
     * 处理队列中一个val所需要的时间，默认一小时
     */
    Integer processingTime = 60 * 60;
}
