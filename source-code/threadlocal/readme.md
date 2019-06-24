# ThreadLocal

ThreadLocal提供了线程局部变量（thread-local variables），这些变量只属于当前线程，而不会被其它线程读取或者修改。

它通常在类中被定义为private static的。

文章主要分析ThreadLocal用法及实现原理，涉及到Thread、ThreadLocal、ThreadLocal.ThreadLocalMap三个类。

其中，每个Thread都持有一个ThreadLocal.ThreadLocalMap实例；而ThreadLocal实例则是用来操作ThreadLocal.ThreadLocalMap实例的。

## 源码分析

只是看看与thread-local变量相关的部分源码。

### Thread

```java
    // 每个Thread类的实例都持有一个ThreadLocal.ThreadLocalMap实例，在用到threal-local变量的时候，该实例会被创建
    ThreadLocal.ThreadLocalMap threadLocals = null;
```

### ThreadLocalMap

ThreadLocalMap以ThreadLocal实例作为key，以用户提供的thread-local变量作为value。

### ThreadLocal

#### get

```java
    /**
     * 返回thread-local变量，如果没有，使用initialValue方法初始化一个并将该值放入当前线程的ThreadLocalMap实例中
     */
    public T get() {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t); // 获取当前线程t的ThreadLocalMap实例引用
        if (map != null) {
            ThreadLocalMap.Entry e = map.getEntry(this); // 以当前ThreadLocal实例为key，从ThreadLocalMap实例中查找该key关联的value
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }
        // ThreadLocalMap实例为null，或者该ThreadLocalMap实例中没有与当前ThreadLocal关联的值
        return setInitialValue();
    }

    /**
     * 初始化ThreadLocalMap实例和thread-local变量的值，并将其与当前ThreadLocal实例关联
     */
    private T setInitialValue() {
        T value = initialValue();
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
        return value;
    }
    
    /**
     * 给线程t的 threadLocals 属性赋值
     */
    void createMap(Thread t, T firstValue) {
        t.threadLocals = new ThreadLocalMap(this, firstValue);
    }
    
    /**
     * 获取线程t的 threadLocals 属性值
     */
    ThreadLocalMap getMap(Thread t) {
        return t.threadLocals;
    }
```

#### set

以当前ThreadLocal实例为key，将value放入ThreadLocalMap实例中

```java
    public void set(T value) {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
    }
```


#### remove

将当前ThreadLocal实例及与其关联value从当前线程的ThreadLocalMap实例中删除

```java
     public void remove() {
         ThreadLocalMap m = getMap(Thread.currentThread());
         if (m != null)
             m.remove(this);
     }
```

## 应用

```java
    // thread-local变量为当前线程的名字 + 随机数
    private static final ThreadLocal<String> THREAD_LOCAL =
            ThreadLocal.withInitial(() -> Thread.currentThread().getName() + "--随机数: " + new Random().nextInt(100));

    public static void main(String[] args) {

        int nThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < nThreads << 1; i++) {
            int finalI = i;
            Future<String> submit =
                    executorService.submit(() -> {
                        String s = THREAD_LOCAL.get() + "----" + finalI;
                        THREAD_LOCAL.remove(); //注释掉这一行，则线程池里每一个线程对应的thread-local变量将不会改变
                        return s;
                    });
            futures.add(submit);
        }

        executorService.shutdown();

        futures.forEach(e -> {
            try {
                String string = e.get();
                log.info(string);
            } catch (InterruptedException | ExecutionException e1) {
                e1.printStackTrace();
            }
        });
    }
```

## 总结

Thread、ThreadLocal、ThreadLocal.ThreadLocalMap三者关系

1. 每一个Thread实例都持有一个叫做threadLocals的属性，它是ThreadLocal.ThreadLocalMap的实例
2. ThreadLocal是面向用户的，作用有2
    1. 用户用它来读取(get)、更新(set)thread-local变量
    2. 内部使用它来获取或者创建线程Thread的threadLocals属性
    
    也就是说ThreadLocal使Thread和ThreadLocal.ThreadLocalMap协作来完成thread-local变量的操作
3. 在ThreadLocal.ThreadLocalMap实例中，是以ThreadLocal实例为key的


ThreadLocal是一个范型类，你可以创建多个ThreadLocal实例以存储不同类型的thread-local变量。

由于线程Thread的threadLocals属性不会被多线程访问；并且，ThreadLocal的get、set、remove等方法，也总是操作当前线程（Thread.currentThread()）的threadLocals属性。
所以这就避免了多线程竞争和线程安全的问题。但是，由于每个线程都有了自己独立的变量，内存上占用会多一些。这是一种以空间换时间的做法。






 