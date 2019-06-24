# Java线程与线程池

关键词：

* `Thread`、`Runnable`
* `ThreadPoolExecutor`、`ScheduledThreadPoolExecutor`
* 线程饥饿死锁（`Thread Starvation Dead Lock`）
* `Executors`

## 线程

### 线程创建

两种方式

* 继承`Thread`类并重写`run`方法

```java
    Thread thread = new Thread() {
        @Override
        public void run() {
            log.debug("thread test");
        }
    };
```
    
* 实现`Runnable`接口

```java
    Runnable runnable = () -> {
        log.debug("thread runnable test");
    };
    Thread thread = new Thread(runnable);
```

### 线程启动

要想启动一个线程，只需调用该线程的`start`方法即可

```java
    thread.start();
```

## 线程池

线程池接口`java.util.concurrent.Executor`，其主要实现类有

```java
    java.util.concurrent.ThreadPoolExecutor
    
    java.util.concurrent.ScheduledThreadPoolExecutor
    
    java.util.concurrent.ForkJoinPool
```

这三个类都直接或间接的继承了抽象类`java.util.concurrent.AbstractExecutorService`。

### ThreadPoolExecutor

#### API

构造器

```java
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler)
```

还有几个重载的构造器与此类似。

参数说明：
    
`corePoolSize`：线程池中保留的线程数，即使线程是空闲的，除非设置了参数`allowCoreThreadTimeOut`

`maximumPoolSize`：线程池中能达到的最大线程数

`keepAliveTime`／`unit`：当线程池中线程数超过`corePoolSize`，这是这些线程在被终止前等待新任务的最大时间

`workQueue`：任务在被执行之前保存在该队列中，该队列可以是有届队列也可以是无届队列

`threadFactory`：线程工厂，创建新线程

`handler`：决定了当线程池中线程数达到`maximumPoolSize`并且`workQueue`已满时新提交的任务会被怎么处理。
    
Java提供了4个实现：

* `AbortPolicy`：直接拒绝新提交的任务并抛出RejectedExecutionException
* `DiscardOldestPolicy`：忽略先前的未被执行的任务并尝试执行当前新提交的任务
* `CallerRunsPolicy`：直接在调用线程池的execute的线程中执行新提交的任务
* `DiscardPolicy`：直接忽略新提交的任务

`corePoolSize`、`maximumPoolSize`、`workQueue`、`handler`四者之间的关

* 当线程池中线程数未到达`corePoolSize`时，创建新线程
* 当线程池中线程数到达`corePoolSize`后并且队列未满时，将任务放入`workQueue`
* 当线程池中线程数到达`corePoolSize`后并且队列已满而线程数还未达到`maximumPoolSiz`e时，创建新线程
* 当线程池中线程数到达`maximumPoolSize`后，新任务将被`handler`处理

#### 示例代码

```java
    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            4,
            8,
            0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(2),
            Executors.defaultThreadFactory(),
            (r, executor) -> log.debug("just ignore {},executor {}", r, executor));
    log.debug("executor {}", threadPoolExecutor);
    for (int i = 0; i < 16; i++) {
        threadPoolExecutor.submit(() -> {
            try {
                Thread.sleep(3000);
                log.debug("thread {} execute", Thread.currentThread());
            } catch (InterruptedException e) {
            }
        });
    }
    threadPoolExecutor.shutdown();
```

示例代码创建了一个`corePoolSize` = 4、`maximumPoolSize` = 8、`workQueue` size = 2、`handler`为仅打印并忽略任务的线程池。

在示例代码中，向线程池提交了16个任务，从日志中观察可知，其中执行了10个，其余6个都被忽略了。

### ScheduledThreadPoolExecutor

`ScheduledThreadPoolExecutor`是一个定时调度器，实现了定点执行任务与周期性执行任务。

它继承了`java.util.concurrent.ThreadPoolExecutor`并实现了接口`java.util.concurrent.ScheduledExecutorService`。

构造器与`ThreadPoolExecutor`类似，提交定时或周期性任务使用scheduleXXX，不再啰嗦。


### 线程饥饿死锁

线程饥饿死锁（`Thread Starvation Dead Lock`）

```java
    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            1,
            8,
            0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(2),
            Executors.defaultThreadFactory(),
            (r, executor) -> log.debug("just ignore {},executor {}", r, executor));

    Callable<String> taskA = () -> {
        Callable<String> taskB = () -> "task b";
        Future<String> future = threadPoolExecutor.submit(taskB);
        String resultOfTaskB = future.get();
        log.debug("task b result: {}", resultOfTaskB);
        return "task a ".concat(resultOfTaskB);
    };

    Future<String> future = threadPoolExecutor.submit(taskA);
    String resultOfTaskA = future.get();
    log.debug("task a result: {}", resultOfTaskA);

    threadPoolExecutor.shutdown();
```
        
如上代码，`taskA`将任务`taskB`提交到同一个`Executor`实例中，由于`corePoolSiz` = 1，
taskB将进入`workQueue`中等待`taskA`执行结束，而`taskA`又在等待`taskB`的执行结果，从而导致了`taskA`、`taskB`互相等待对方的执行。

这种情况叫做线程饥饿死锁（`Thread Starvation Dead Lock`）。

解决线程饥饿死锁的方法：

1. 将`corePoolSize`值设置为足够支持你的线程数的值（这样如果你不设置`allowCoreThreadTimeOut`，你的空闲线程将不能被回收）
2. 为`taskB`创建另一个线程池
3. 将`corePoolSize`值设置为0，将`maximumPoolSize`设置为足够支持你的线程数的值（确保你的线程数不会达到该值，否则新提交的任务将会被`handler`处理），
并且`workQueue`使用`SynchronousQueue`（`Executors`的`newCachedThreadPool`方法就是这样做的）


所以，尽量不要在任务内部向同一个`Executor`实例提交任务，以免造成线程饥饿死锁。

### ThreadPoolExecutor扩展

`ThreadPoolExecutor`提供了三个未做任何操作的空方法，如下：

```java
    //任务执行前被调用
    protected void beforeExecute(Thread t, Runnable r) { }
    //任务执行后被调用
    protected void afterExecute(Runnable r, Throwable t) { }
    /线程池关闭时被调用
    protected void terminated() { }
```
    
可以重写这三个方法来实现对`ThreadPoolExecutor`的扩展，比如实现任务执行耗时统计／日志记录等。

示例代码

```java
    //扩展ThreadPoolExecutor
    class ExtThreadPoolExecutor extends ThreadPoolExecutor {

        //记录线程开始执行的时间
        private ThreadLocal<Long> startThreadLocal = new ThreadLocal<>();
        //记录线程执行的总时间
        private AtomicLong totalTime = new AtomicLong(0);
        //记录已执行的线程数
        private AtomicLong totalCount = new AtomicLong(0);

        public ExtThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        }

        public ExtThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        }

        public ExtThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
        }

        public ExtThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);
            startThreadLocal.set(System.currentTimeMillis());
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            if (t == null) {
                Long start = startThreadLocal.get();
                startThreadLocal.remove();
                long thisTime = System.currentTimeMillis() - start;
                long count = totalCount.addAndGet(1);
                long total = totalTime.addAndGet(thisTime);
                log.debug("task {} taking {} ms, average taking {} ms", r, thisTime, total / count);
            } else {
                log.error("task {} exception {}", r, t);
            }
        }

        @Override
        protected void terminated() {
            super.terminated();
            log.debug("threadPoolExecutor {} terminated", this);
        }

    }

    //测试自己扩展的ExtThreadPoolExecutor
    int nThreads = Runtime.getRuntime().availableProcessors();
    ThreadPoolExecutor threadPoolExecutor = new ExtThreadPoolExecutor(
            nThreads,
            nThreads << 1,
            0, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());
    for (int i = 0; i < nThreads << 1; i++) {
        threadPoolExecutor.submit(() -> {
            try {
                Thread.sleep(new Random().nextInt(3000));
                log.debug("current time: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
    threadPoolExecutor.shutdown();
```

### Executors

Java在其类库中`java.util.concurrent.Executors`中提供了些创建线程池的静态工厂方法：

* `newFixedThreadPool`：创建一个`corePoolSize` = `maximumPoolSize` 、`workQueue`无界的线程池
* `newWorkStealingPool`：创建一个`ForkJoinPool`
* `newCachedThreadPool`：创建一个`corePoolSize` = 0、`maximumPoolSize` = Integer.MAX_VALUE、`workQueue`为`SynchronousQueue`的线程池。
当一个新任务被提交到`SynchronousQueue`时，必须有另一个线程正在等待接受这个任务。如果没有线程正在等待，且线程数未达到最大线程数，则创建新的线程；否则任务将会被`handler`处理。
* `newScheduledThreadPool`：创建一个定时任务线程池
* `newSingleThreadExecutor`：创建一个单线程的线程池
* `newSingleThreadScheduledExecutor`：创建一个单线程的定时任务线程池

