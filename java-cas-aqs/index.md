---
layout: post
title: CAS & AQS
date: 2019-01-04
author: yz
tags: java,cas,aqs
categories: java,cas,aqs
---

本文介绍两方面的知识：
* cas: compare and swap
* aqs: AbstractQueuedSynchronizer
# cas

cas是 compare and swap的缩写，由Unsafe类调用native方法实现。cas由cpu指令支持，是一个原子操作。

代码实例基于Java 8。

## Unsafe类api

以下仅列出关于cas的方法。

```java
    //获取实例属性相对于持有它的对象的地址偏移量
    public native long objectFieldOffset(Field var1);
    //获取类属性相对于持有它的对象的地址偏移量
    public native long staticFieldOffset(Field var1);
    
    //获取属性值，以Volatile结尾的方法具有volatile语义
    //获取实例var1地址偏移量var2处的int属性值
    public native int getInt(Object var1, long var2);
    public native int getIntVolatile(Object var1, long var2);
    //获取实例var1地址偏移量var2处的long属性值
    public native long getLong(Object var1, long var2);
    public native long getLongVolatile(Object var1, long var2);
    //获取实例var1地址偏移量var2处的Object属性值
    public native Object getObject(Object var1, long var2);
    public native Object getObjectVolatile(Object var1, long var2);
    
    //cas操作，var1表示要操作的对象，var2表示属性的偏移量，var4表示cas时期望的旧值，var5表示更新的值
    //操作Object类型
    public final native boolean compareAndSwapObject(Object var1, long var2, Object var4, Object var5);
    //操作int类型
    public final native boolean compareAndSwapInt(Object var1, long var2, int var4, int var5);
    //操作long类型
    public final native boolean compareAndSwapLong(Object var1, long var2, long var4, long var6);
```

它还有putXXX的一些方法，这里不再列出。

Unsafe类还实现了一些循环cas的方法，这些方法使用cas保证对变量的更新是线程安全的。

```java
    public final int getAndAddInt(Object var1, long var2, int var4) {
        int var5;
        do {
            var5 = this.getIntVolatile(var1, var2);
        } while(!this.compareAndSwapInt(var1, var2, var5, var5 + var4));

        return var5;
    }

    public final long getAndAddLong(Object var1, long var2, long var4) {
        long var6;
        do {
            var6 = this.getLongVolatile(var1, var2);
        } while(!this.compareAndSwapLong(var1, var2, var6, var6 + var4));

        return var6;
    }

    public final int getAndSetInt(Object var1, long var2, int var4) {
        int var5;
        do {
            var5 = this.getIntVolatile(var1, var2);
        } while(!this.compareAndSwapInt(var1, var2, var5, var4));

        return var5;
    }

    public final long getAndSetLong(Object var1, long var2, long var4) {
        long var6;
        do {
            var6 = this.getLongVolatile(var1, var2);
        } while(!this.compareAndSwapLong(var1, var2, var6, var4));

        return var6;
    }

    public final Object getAndSetObject(Object var1, long var2, Object var4) {
        Object var5;
        do {
            var5 = this.getObjectVolatile(var1, var2);
        } while(!this.compareAndSwapObject(var1, var2, var5, var4));

        return var5;
    }
```

## Unsafe类实例

一个完整的计数器实例，使用Unsafe类的cas方法实现了线程安全的递增操作。

```java
    public static class Counter {
        private volatile int value;
        private Unsafe unsafe;
        private long valueOffset;

        public Counter(int value) throws Exception {
            this.value = value;
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            unsafe = (Unsafe) theUnsafeField.get(null);
            valueOffset = unsafe.objectFieldOffset(Counter.class.getDeclaredField("value"));
        }

        public int increment() {
            int t;
            do {
                t = unsafe.getIntVolatile(this, valueOffset);
            } while (!unsafe.compareAndSwapInt(this, valueOffset, t, t + 1));
            return t;
        }
    } 
```
   

事实上，实例中的increment方法在Unsafe类中已经有实现了，它等同于

```java
    public int increment() {
        return unsafe.getAndAddInt(this, valueOffset, 1);
    }
```
    
单元测试，可以看到最终打印结果为320

```java
    @Test
    public void testCounter() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() << 1);
        Counter counter = new Counter(0);
        for (int i = 0; i < 320; i++) {
            executorService.execute(() -> {
                int r = counter.increment();
                log.debug("更新前的值：{}", r);
            });
        }
        executorService.shutdown();
        do {
            log.debug("等待任务全部执行");
        } while (!executorService.awaitTermination(2, TimeUnit.SECONDS));

        log.info("最终值：{}", counter.value);
    }
```

以上就是cas操作的简单介绍。
下文开始介绍aqs相关知识。

# aqs