package com.chl.blocklist;

import java.util.Arrays;
import java.util.Date;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;

import static com.chl.blocklist.RedisBlockListConstants.*;

/**
 * 等待阻塞队列并处理元素，需要自定义如何处理元素
 * @author hailongchen
 */
@Slf4j
public abstract class RedisBlockListWorker implements Runnable{

    /**
     * 使用的线程
     */
    Thread thread;

    /**
     * must ， 用于访问redis
     */
    @Setter
    RedisTemplate redisTemplate;

    /**
     * must
     */
    @Setter
    RedisBlockListConfig redisBlockListConfig;

    @Override
    public void run() {
        while (true){
            // 阻塞获取
            String val = redisTemplate.bPop(redisBlockListConfig.getBlockListKey());

            // 由redis保证队列中val只由一个客户端获取到，所以这里不做并发限制

            // 删除done
            delDone();

            // 进入正在处理队列
            String processingVal = addProcessingList(val);

            boolean noEx = true;
            ProcessResult processResult = null;
            try {
                processResult = process(val);
            } catch (Throwable e) {
                log.error("RedisBlockListWorker|{}|{}", redisBlockListConfig.blockListKey, val, e);
                noEx = false;
            }

            // 退出正在处理队列
            delProcessingList(processingVal);

            if (noEx) {
                if (null != processResult && processResult.failCount > 0L) {
                    redisTemplate.incr(RedisBlockListUtils.getFailCountKey(redisBlockListConfig.blockListKey), processResult.failCount);
                }

                // 判断本周期list是否都处理
                boolean hasDone = hasDone();
                if (hasDone) {
                    doneCallBack(val);
                }
            }
            else {
                // 处理异常，则再次进入队列
                RedisBlockListExecutor.add(redisTemplate, redisBlockListConfig.getBlockListKey(), val);
            }
        }
    }

    private void delDone() {
        String doneKey = redisBlockListConfig.getBlockListKey() + DONE_SUFFIX;
        redisTemplate.del(doneKey);
    }

    /**
     * 队列任务全部完成时的处理
     */
    private boolean hasDone() {
        // 是否需要判断
        if (!redisBlockListConfig.isDealDone()) {
            return false;
        }

        String countKey = RedisBlockListUtils.getCountKey(redisBlockListConfig.getBlockListKey());
        String countString = redisTemplate.get(countKey);

        // 非数字
        if (!NumberUtils.isDigits(countString)) {
            redisTemplate.del(countKey);
            return false;
        }

        long count = NumberUtils.toLong(countString);
        if (count > 0) {
            // -1
            count = redisTemplate.decr(countKey);
            System.out.println(countString + " :count: " +count);
        }

        if (count <= 0) {
            redisTemplate.del(countKey);

            // 设置本周期已done
            String doneKey = RedisBlockListUtils.getDoneKey(redisBlockListConfig.getBlockListKey());
            if (null != redisBlockListConfig.getDoneExpire()) {
                redisTemplate.set(doneKey, DONE_VALUE, redisBlockListConfig.getDoneExpire());
            }else {
                redisTemplate.set(doneKey, DONE_VALUE);
            }

            if (log.isInfoEnabled()) {
                log.info("{}Done", redisBlockListConfig.getBlockListKey());
            }

            return true;
        }

        return false;
    }

    private void delProcessingList(String val) {
        String processingKey = RedisBlockListUtils.getProcessingKey(redisBlockListConfig.getBlockListKey());
        redisTemplate.rem(processingKey, val);
    }

    private String addProcessingList(String val) {
        ProcessingVal processingVal = new ProcessingVal();
        processingVal.setVal(val);
        processingVal.setStart(new Date());
        String processingValJson = JSONObject.toJSONString(processingVal);

        String processingKey = RedisBlockListUtils.getProcessingKey(redisBlockListConfig.getBlockListKey());
        redisTemplate.push(processingKey, Arrays.asList(processingValJson));

        return processingValJson;
    }

    public abstract ProcessResult process(String val);

    /**
     * 本轮所有元素已处理完成，触发的回调
     */
    public void doneCallBack(String val){}

    /**
     * process result
     */
    public static class ProcessResult{
        Long failCount = 0L;
        public void incrFail(){
            failCount += 1;
        }
    }

    /**
     * 正在处理的元素信息
     */
    @Data
    public static class ProcessingVal{

        /**
         * 元素
         */
        String val;

        /**
         * 开始处理时间
         */
        Date start;
    }
}
