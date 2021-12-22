package com.chl.blocklist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

/**
 * redis阻塞队列处理器
 * <br/>
 * 分为两个部分：向队列添加val，添加数据处理者
 * @author hailongchen
 */
public class RedisBlockListExecutor {
    static {
        new ProcessingChecker().runCheck();
    }

    /**
     * 正处理队列  检查器，作用：避免特殊情况(如停止服务器)造成 队列中val 未处理完毕的情况
     */
    static class ProcessingChecker {
        /**
         * 检查间隔秒数
         */
        private Integer CHECK_PROCESSING_INTERVAL = 30 * 60;

        void runCheck() {
            Runnable runnable = new Runnable() {

                @Override
                public void run() {
                    for (RedisBlockListWorker node : workers) {
                        try {
                            checkProcessing(node);
                        } catch (Exception e) {
                        }
                    }
                }
            };
            // 一段时间执行一次 正处理队列 检查
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(runnable, 0, 5 * 60, TimeUnit.SECONDS);
        }

        private void checkProcessing(RedisBlockListWorker worker) {
            // 阻塞队列key
            String blockListKey = worker.redisBlockListConfig.getBlockListKey();
            // 正在处理队列key
            String processingKey = RedisBlockListUtils.getProcessingKey(blockListKey);
            String checkProcessingKey = RedisBlockListUtils.getCheckProcessingKey(blockListKey);
            // 预估val的处理时间
            Integer processingTime = worker.redisBlockListConfig.getProcessingTime();

            RedisTemplate redisTemplate = worker.redisTemplate;

            // 排他
            Boolean setNx = redisTemplate.setNx(checkProcessingKey, System.nanoTime() + "", CHECK_PROCESSING_INTERVAL - 10);
            if (!setNx) {
                return;
            }

            // 正在处理队列中的所有val
            List<String> vals = redisTemplate.lcard(processingKey);
            if (null == vals || vals.size() == 0) {
                return;
            }

            for (String val : vals) {
                RedisBlockListWorker.ProcessingVal processingVal = null;
                try {
                    processingVal = JSONObject
                            .parseObject(val, RedisBlockListWorker.ProcessingVal.class);
                } catch (Exception e) {
                    // 转化失败，认为数据格式错误，删除
                    redisTemplate.rem(processingKey, val);
                }

                // 处理开始时间 + 预估val处理时间
                Date afterProcessingTime = DateUtils.addSeconds(processingVal.getStart(), processingTime);

                // 已经超过处理时间
                if (afterProcessingTime.before(new Date())) {
                    // 从正在处理队列删除
                    redisTemplate.rem(processingKey, val);
                    // 重新加入到任务队列
                    add(redisTemplate, blockListKey, processingVal.val);
                }
            }
        }

    }

    /**
     * 执行器负责的阻塞队列的当前情况
     * @return
     */
    public static List<RedisBlockListCurrentProcess> getCurrentProcess() {
        List<RedisBlockListCurrentProcess> processes = new ArrayList<>();
        for (RedisBlockListWorker node : workers) {
            try {
                RedisBlockListCurrentProcess process = getCurrentProcess(node);
                processes.add(process);
            } catch (Exception e) {
            }
        }
        return processes;
    }

    private static RedisBlockListCurrentProcess getCurrentProcess(RedisBlockListWorker node) {
        String blockListKey = node.redisBlockListConfig.blockListKey;
        long blockListEleCount = node.redisTemplate.llen(blockListKey);
        long processingEleCount = node.redisTemplate
                .llen(RedisBlockListUtils.getProcessingKey(blockListKey));
        String done = node.redisTemplate.get(RedisBlockListUtils.getDoneKey(blockListKey));

        RedisBlockListCurrentProcess process = new RedisBlockListCurrentProcess();
        process.setContext(node.redisBlockListConfig);
        process.setDone(done);
        process.setBlockListEleCount(blockListEleCount);
        process.setProcessingEleCount(processingEleCount);

        return process;
    }

    /**
     * 向队列中添加val，不设置count
     * @param redisTemplate
     * @param key
     * @param val
     */
    public static void add(RedisTemplate redisTemplate, String key, String val) {
        redisTemplate.push(key, Arrays.asList(val));
    }

    /**
     * 向队列中添加vals
     * @param redisTemplate
     * @param key
     * @param vals
     * @param setCount 是否设置count
     */
    public static void add(RedisTemplate redisTemplate, String key, List<String> vals, boolean setCount) {
        String blockListKey = key;
        if (setCount) {
            String countKey = RedisBlockListUtils.getCountKey(blockListKey);
            redisTemplate.incr(countKey, Long.valueOf(vals.size()));
        }
        redisTemplate.push(blockListKey, vals);
    }

    /**
     * new data period, sweep keys
     * @param redisTemplate
     * @param key
     * @param vals
     */
    public static void add4NewPeriod(RedisTemplate redisTemplate, String key, List<String> vals) {
        String blockListKey = key;

        // del blocklist
        redisTemplate.del(blockListKey);
        // del processing list
        redisTemplate.del(RedisBlockListUtils.getProcessingKey(blockListKey));
        // del count
        String countKey = RedisBlockListUtils.getCountKey(blockListKey);
        redisTemplate.del(countKey);
        // del done
        redisTemplate.del(RedisBlockListUtils.getDoneKey(blockListKey));

        redisTemplate.del(RedisBlockListUtils.getFailCountKey(blockListKey));

        redisTemplate.set(countKey, vals.size() + "");

        redisTemplate.push(blockListKey, vals);
    }

    /**
     *
     */
    static List<RedisBlockListWorker> workers = new ArrayList<>();

    public static void execute(RedisBlockListWorker worker) {
        // check
        if (null == worker.redisBlockListConfig){
            throw new IllegalArgumentException("need redisBlockListContext");
        }
        if (StringUtils.isBlank(worker.redisBlockListConfig.getBlockListKey())) {
            throw new IllegalArgumentException("need redisBlockListContext.blocklistkey");
        }
        if (null == worker.redisTemplate) {
            throw new IllegalArgumentException("need redisTemplate");
        }

        // 新建线程
        Thread thread = threadFactory.newThread(worker);

        // 上下文
        worker.thread = thread;

        synchronized (workers) {
            workers.add(worker);
        }

        // 启动线程
        thread.start();
    }

    private static ThreadFactory threadFactory = new ThreadFactory() {

        AtomicInteger no = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("RedisBlockListExecutor-thread-" + no.getAndAdd(1));
            return thread;
        }
    };
}
