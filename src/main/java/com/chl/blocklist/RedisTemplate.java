package com.chl.blocklist;

import java.util.List;

/**
 * redis操作接口，需要使用者提供实现类
 * @author hailongchen
 */
public interface RedisTemplate {

    /**
     * 向队列加数据
     * @param listKey
     * @param vals
     */
    void push(String listKey, List<String> vals);

    /**
     * 从队列删除
     * @param listKey
     * @param val
     */
    void rem(String listKey, String val);

    /**
     * 阻塞队列获取数据
     * @param listKey
     * @return
     */
    String bPop(String listKey);

    /**
     * string set key val
     * @param key
     * @param val
     * @param seconds expire seconds
     */
    void set(String key, String val, Integer seconds);

    Boolean setNx(String key, String val, Integer seconds);

    void set(String key, String val);

    String get(String key);

    Long decr(String key);

    Long incr(String key, Long interval);

    /**
     * del key
     * @param key
     */
    void del(String key);

    List<String> lcard(String key);

    long llen(String key);
}
