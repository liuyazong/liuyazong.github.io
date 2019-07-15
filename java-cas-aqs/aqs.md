# AbstractQueuedSynchronizer

`java.util.concurrent.locks.AbstractQueuedSynchronizer`类是以下几个同步工具类实现的基础。

```java
//
java.util.concurrent.locks.ReentrantLock
//
java.util.concurrent.locks.ReentrantReadWriteLock
//
java.util.concurrent.Semaphore
//
java.util.concurrent.CountDownLatch
//
java.util.concurrent.CyclicBarrier
//
java.util.concurrent.ThreadPoolExecutor
```

以下以ReentrantLock中默认的AQS实现`java.util.concurrent.locks.ReentrantLock.NonfairSync`来看AQS的实现原理。

## AQS内部有什么？

```java
    // 内部类Node，后面等待队列中的结点的定义
    static final class Node {
        //...
    }
    // 等待队列的头结点，延迟初始化。除了初始化，它的值只能被setHead方法修改。如果该结点存在，则要保证它的waitStatus属性不是CANCELLED（-1）。
    private transient volatile Node head;
    // 等待队列的尾结点，延迟初始化。除了初始化，它的值只能被enq方法在添加新结点时修改。
    private transient volatile Node tail;
    // 同步状态
    private volatile int state;
```

## AQS是怎样获取锁的？

其实是ReentrantLock是怎样获取锁的？

默认情况下，ReentrantLock使用抽象类Sync的NonfairSync实现。

ReentrantLock中获取锁的lock方法有以下4个：

```java
    /**
     *  获取锁，阻塞直到成功
     *  如果锁未被其它线程持有，则获取到锁，将获取锁的次数设置为1，返回；
     *  如果当前线程是已获取到锁的线程，则将获取锁的次数增加1，返回；
     *  如果锁已被其它线程持有，则当前线程进入休眠状态，不可被线程调度器调度，直到获取到锁，然后将获取锁的次数设置为1
     */
    public void lock() {
        // 调用ReentrantLock.NonfairSync.lock方法
        sync.lock();
    }
    /**
     *  获取锁，直到成功，除非当前线程被中断
     *  如果锁未被其它线程持有，则获取到锁，将获取锁的次数设置为1，返回；
     *  如果锁已被其它线程持有，则当前线程进入休眠状态，不可被线程调度器调度，直到以下两者之一发生：
     *      锁成功被当前线程获取到
     *      或者，其它线程调用了当前线程的interrupt方法中断了当前线程
     *  如果当前线程是已获取到锁的线程，则将获取锁的次数增加1，返回；
     *  如果当前线程被中断了，则抛出InterruptedException异常，并且清除当前线程的中断状态
     */
    public void lockInterruptibly() throws InterruptedException {
        //AbstractQueuedSynchronizer.acquireInterruptibly方法
        sync.acquireInterruptibly(1);
    }
    /**
     *  尝试获取锁，仅在锁未被其它线程持有的情况下，获取成功后将获取锁的次数设置为1
     */    
    public boolean tryLock() {
        // 调用ReentrantLock.Sync.nonfairTryAcquire方法
        return sync.nonfairTryAcquire(1);
    }
    /**
     *  尝试获取锁，带有超时时间
     */
    public boolean tryLock(long timeout, TimeUnit unit)
            throws InterruptedException {
        // 调用AbstractQueuedSynchronizer.tryAcquireNanos方法
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }
```

### sync.lock();

即ReentrantLock.NonfairSync.lock方法

```java
    final void lock() {
        if (compareAndSetState(0, 1))// 尝试使用CAS修改state字段值，如果修改成功，则获取到锁，将exclusiveOwnerThread设置为当前线程
            setExclusiveOwnerThread(Thread.currentThread());
        else// CAS失败，调用AbstractQueuedSynchronizer.acquire方法获取锁
            acquire(1);
    }
```

AbstractQueuedSynchronizer.acquire方法分为两步：
1. tryAcquire方法
    如果返回true，则获取到锁，短路返回；否则
2. 调用acquireQueued
    如果返回true，则获取到锁
    
```java
    public final void acquire(int arg) {
        if (!tryAcquire(arg) &&
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    }
```

#### tryAcquire

即ReentrantLock.NonfairSync.tryAcquire方法

```java
    protected final boolean tryAcquire(int acquires) {
        return nonfairTryAcquire(acquires);
    }
```

ReentrantLock.Sync.nonfairTryAcquire方法

```java
    final boolean nonfairTryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        int c = getState();
        if (c == 0) { // state为0表示锁未被其它线程持有，尝试使用CAS更新state值，更新成功则获取到锁
            if (compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(current);
                return true;
            }
        }
        else if (current == getExclusiveOwnerThread()) { // 如果已获取到锁的线程是当前线程，则增加state的值 
            int nextc = c + acquires;
            if (nextc < 0) // overflow
                throw new Error("Maximum lock count exceeded");
            setState(nextc);// 因为当前线程已经获取到锁，这里不使用CAS也是线程安全的
            return true;
        }
        return false; // 未获取到锁
    }
```

#### acquireQueued

结点加入链表

```java
    /**
    * 尝试使用CAS将当前线程对应的结点加入到链表尾部，如果失败使用自旋CAS的方式将该结点加入到链表尾部 
    */
    private Node addWaiter(Node mode) {
        Node node = new Node(Thread.currentThread(), mode);
        // Try the fast path of enq; backup to full enq on failure
        Node pred = tail;
        if (pred != null) {//如果尾结点已存在，则尝试使用CAS将新结点添加到链表并使之成为尾结点
            node.prev = pred;
            if (compareAndSetTail(pred, node)) {
                pred.next = node;
                return node; // 
            }
        }
        // 上面失败，则调用enq使用自旋CAS的方式将新结点加入到链表中并使之成为尾结点
        enq(node);
        return node;
    }
    /**
    * 使用自旋CAS的方式将该结点加入到链表尾部，返回
    */
    private Node enq(final Node node) {
            for (;;) {
                Node t = tail;
                if (t == null) { // Must initialize // 尾结点未初始化，则对链表进行初始化，此时头、尾是同一个结点 
                    if (compareAndSetHead(new Node()))
                        tail = head;
                } else { // 将当前结点加入到链表尾部，返回先前的尾结点的引用；对于addWaiter方法，忽略该返回值
                    node.prev = t;
                    if (compareAndSetTail(t, node)) {
                        t.next = node;
                        return t;
                    }
                }
            }
        }
```

使用CAS自旋的方式获取锁

```java
    final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {// 只有try，没有catch，将循环中抛出的异常交给调用者处理
            boolean interrupted = false;
            for (;;) {
                final Node p = node.predecessor(); // 当前结点的前驱结点
                if (p == head && tryAcquire(arg)) {// 如果p是头结点，则调用tryAcquire方法获取锁
                    setHead(node); // 获取成功，设置当前结点为head结点，并把node的thread属性和prev属性置为null。因为当前线程已经获取到锁，这里不使用CAS也是线程安全的
                    p.next = null; // help GC
                    failed = false;
                    return interrupted;
                }
                
                // 当前结点不是head、或者tryAcquire未成功
                // shouldParkAfterFailedAcquire返回true，表示当前线程需要被block，然后调用parkAndCheckInterruptpark该线程；
                // shouldParkAfterFailedAcquire返回false，则继续执行循环
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }
```

shouldParkAfterFailedAcquire：每次循环内获取锁失败后，是否使当前线程不可被线程调度器调度

```java
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        int ws = pred.waitStatus;
        if (ws == Node.SIGNAL)
            /*
             * 前驱结点是waitStatus是Node.SIGNAL，则park当前线程
             * This node has already set status asking a release
             * to signal it, so it can safely park.
             */
            return true;
        if (ws > 0) {
            /*
             * 将这些waitStatus>0的结点从链表中删除
             * Predecessor was cancelled. Skip over predecessors and
             * indicate retry.
             */
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            /*
             * 使用CAS将前驱结点的waitStatus设置为Node.SIGNAL
             * waitStatus must be 0 or PROPAGATE.  Indicate that we
             * need a signal, but don't park yet.  Caller will need to
             * retry to make sure it cannot acquire before parking.
             */
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }
```           
 
parkAndCheckInterrupt

```java
    private final boolean parkAndCheckInterrupt() {
        // 禁用对当前线程的调度，直到许可可用
        // 如果许可可用，方法将使用这个许可并且立即返回
        // 否则，当前线程的调度被禁用并且进入休眠状态，直到以下事件之一发生：
        //      其它线程以当前线程为目标调用了unpark方法
        //      其它线程调用了当前线程的interrupt方法中断了当前线程
        //      毫无理由的返回
        // 该方法不会报告返回的原因。调用这应该重新检查是哪个条件导致了线程暂停。
        LockSupport.park(this);
        return Thread.interrupted();
    }
```