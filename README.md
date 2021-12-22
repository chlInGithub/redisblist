# redisblist
redis阻塞队列执行器

出于更加简洁的使用redis阻塞队列满足业务需求的目的，避免重复写业务无关的代码，我对通用性的代码进行了封装，并且增加了一些特性。

使用者需要做的事情：实现RedisTemplate接口，向队列添加元素，添加元素处理者.

特性：
可记录本轮阻塞队列元素处理完毕；
当本轮阻塞队列元素处理完毕，可回调；
检查机制，高可用；
记录处理失败的数量；

https://blog.csdn.net/chl87783255/article/details/117925291
