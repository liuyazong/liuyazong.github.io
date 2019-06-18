# ConcurrentHashMap

## put操作

```java
    /**
     * put 
     */
    public V put(K key, V value) {
        return putVal(key, value, false);
    }
    
    /**
     * 如果key不存在，则将key-value放入；否则，返回已存在的value
     */
    public V putIfAbsent(K key, V value) {
        return putVal(key, value, true);
    }

    /** Implementation for put and putIfAbsent */
    final V putVal(K key, V value, boolean onlyIfAbsent) {
        if (key == null || value == null) throw new NullPointerException();
        int hash = spread(key.hashCode());
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)// table属性还未初始化，对table进行初始化
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {// 该idx处没有元素，使用CAS的方式将key-value放入table中
                if (casTabAt(tab, i, null,
                             new Node<K,V>(hash, key, value, null)))
                    break;                   // no lock when adding to empty bin
            }
            else if ((fh = f.hash) == MOVED)// f已在前一个else if分支被赋值，如果它是ForwardingNode结点，表示该idx处的结点正在resize
                tab = helpTransfer(tab, f); // 使当前线程帮助做resize
            else { // 将key-value插入到table的idx处的链表或红黑树中
                V oldVal = null;
                synchronized (f) { // 使用synchronized关键字对idx处的链表或红黑树加锁
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {// fh已在前一个else if分支被赋值，fh >= 0表示这是一个正常的结点，把f插入到链表的尾部
                            binCount = 1;
                            for (Node<K,V> e = f;; ++binCount) {
                                K ek;
                                if (e.hash == hash &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {// 该key已经存在，结束循环
                                    oldVal = e.val;
                                    if (!onlyIfAbsent)
                                        e.val = value;
                                    break;
                                }
                                
                                // 将key-value构造为Node实例并插入到链表的尾部，然后结束循环
                                Node<K,V> pred = e;
                                if ((e = e.next) == null) {
                                    pred.next = new Node<K,V>(hash, key,
                                                              value, null);
                                    break;
                                }
                            }
                        }
                        else if (f instanceof TreeBin) { // 如果f是TreeBin，表示f是一个红黑树，将key-value构造为TreeNode实例并插入到f这个红黑树中
                            Node<K,V> p;
                            binCount = 2;
                            if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                           value)) != null) {
                                oldVal = p.val;
                                if (!onlyIfAbsent)
                                    p.val = value;
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)// 如果链表长度已经达到TREEIFY_THRESHOLD（8）
                        treeifyBin(tab, i);// 这里跟HashMap中的逻辑差不多，进行resize或者转换为红黑树，只是多了线程同步的逻辑
                    if (oldVal != null)
                        return oldVal;
                    break;
                }
            }
        }
        
        // 这个方法内部也会判断是否需要进行扩容
        addCount(1L, binCount);
        return null;
    }
```
### initTable方法

对table进行初始化，借助变量sizeCtl和CAS来实现多线程状态下的安全操作。

```java
    /**
     * Initializes table, using the size recorded in sizeCtl.
     */
    private final Node<K,V>[] initTable() {
        Node<K,V>[] tab; int sc;
        while ((tab = table) == null || tab.length == 0) {
            
            // sizeCtl初始值默认为0（使用无参构造器），或者为table容量
            if ((sc = sizeCtl) < 0)
                Thread.yield(); // lost initialization race; just spin
            else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) { // 使用CAS将sizeCtl更新为 -1，表示在进行初始化操作，更新成功表示当前线程拿到了对table进行初始化的权利
                try {
                    if ((tab = table) == null || tab.length == 0) { // 再次判断table是否初始化过，如果未初始化则进行初始化；否则，表示其它线程已对其进行初始化，结束操作
                        int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                        @SuppressWarnings("unchecked")
                        Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];// 为table数组分配n个大小空间
                        table = tab = nt;
                        sc = n - (n >>> 2);
                    }
                } finally {
                    sizeCtl = sc; // 为sizeCtl赋值。初始化成功，该值表示下一次resize时数组中元素应该达到的个数
                }
                break;
            }
        }
        return tab;
    }
```

### treeifyBin方法

如果table长度已达到MIN_TREEIFY_CAPACITY（64），则进行转红黑树的操作；否则，进行resize操作

```java
    /**
     * Replaces all linked nodes in bin at given index unless table is
     * too small, in which case resizes instead.
     */
    private final void treeifyBin(Node<K,V>[] tab, int index) {
        Node<K,V> b; int n, sc;
        if (tab != null) {
            if ((n = tab.length) < MIN_TREEIFY_CAPACITY)
                tryPresize(n << 1);
            else if ((b = tabAt(tab, index)) != null && b.hash >= 0) {// 
                synchronized (b) { // 使用synchronized关键字对b进行锁定
                    if (tabAt(tab, index) == b) { // check
                        
                        // 与HashMap中类似，将链表转换为使用TreeNode构造的双向链表
                        TreeNode<K,V> hd = null, tl = null;
                        for (Node<K,V> e = b; e != null; e = e.next) {
                            TreeNode<K,V> p =
                                new TreeNode<K,V>(e.hash, e.key, e.val,
                                                  null, null);
                            if ((p.prev = tl) == null)
                                hd = p;
                            else
                                tl.next = p;
                            tl = p;
                        }
                        
                        // 使用TreeBin将上面构造的双向链表构造为一颗红黑树，然后放入到table的idx处
                        setTabAt(tab, index, new TreeBin<K,V>(hd));// TreeBin的hash值为TREEBIN（-2）
                    }
                }
            }
        }
    }
```

## resize操作

```java
    /**
     * Helps transfer if a resize is in progress.
     */
    final Node<K,V>[] helpTransfer(Node<K,V>[] tab, Node<K,V> f) {
        Node<K,V>[] nextTab; int sc;
        if (tab != null && (f instanceof ForwardingNode) &&
            (nextTab = ((ForwardingNode<K,V>)f).nextTable) != null) {
            int rs = resizeStamp(tab.length);
            while (nextTab == nextTable && table == tab &&
                   (sc = sizeCtl) < 0) {
                if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                    sc == rs + MAX_RESIZERS || transferIndex <= 0)
                    break;
                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                    transfer(tab, nextTab);// 当前线程调用transfer进行resize
                    break;
                }
            }
            return nextTab;
        }
        return table;
    }

    /**
     * Tries to presize table to accommodate the given number of elements.
     *
     * @param size number of elements (doesn't need to be perfectly accurate)
     */
    private final void tryPresize(int size) {
        int c = (size >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY :
            tableSizeFor(size + (size >>> 1) + 1);
        int sc;
        while ((sc = sizeCtl) >= 0) {
            Node<K,V>[] tab = table; int n;
            if (tab == null || (n = tab.length) == 0) { // 如果table未初始化，则对table进行初始化，与initTable方法类似
                n = (sc > c) ? sc : c;
                if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                    try {
                        if (table == tab) {
                            @SuppressWarnings("unchecked")
                            Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                            table = nt;
                            sc = n - (n >>> 2);
                        }
                    } finally {
                        sizeCtl = sc;
                    }
                }
            }
            else if (c <= sc || n >= MAXIMUM_CAPACITY) // 达到最大，不再扩容
                break;
            else if (tab == table) { // 表示table没有被替换，即没有进行resize，触发扩容操作
                int rs = resizeStamp(n);
                if (sc < 0) {
                    Node<K,V>[] nt;
                    if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                        sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                        transferIndex <= 0)
                        break;
                    if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                        transfer(tab, nt);
                }
                else if (U.compareAndSwapInt(this, SIZECTL, sc,
                                             (rs << RESIZE_STAMP_SHIFT) + 2))
                    transfer(tab, null);
            }
        }
    }

    /**
     * 将已有结点移动到新创建的table中
     * Moves and/or copies the nodes in each bin to new table. See
     * above for explanation.
     */
    private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab) {
        int n = tab.length, stride;
        if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
            stride = MIN_TRANSFER_STRIDE; // subdivide range
        if (nextTab == null) {            // initiating
            try {
                @SuppressWarnings("unchecked")
                Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n << 1];
                nextTab = nt;
            } catch (Throwable ex) {      // try to cope with OOME
                sizeCtl = Integer.MAX_VALUE;
                return;
            }
            nextTable = nextTab;
            transferIndex = n;
        }
        int nextn = nextTab.length;
        ForwardingNode<K,V> fwd = new ForwardingNode<K,V>(nextTab);// 创建ForwardingNode结点，其内部保存了nextTab的引用
        boolean advance = true;
        boolean finishing = false; // to ensure sweep before committing nextTab
        for (int i = 0, bound = 0;;) {
            Node<K,V> f; int fh;
            while (advance) {
                int nextIndex, nextBound;
                if (--i >= bound || finishing)
                    advance = false;
                else if ((nextIndex = transferIndex) <= 0) {
                    i = -1;
                    advance = false;
                }
                else if (U.compareAndSwapInt
                         (this, TRANSFERINDEX, nextIndex,
                          nextBound = (nextIndex > stride ?
                                       nextIndex - stride : 0))) {
                    bound = nextBound;
                    i = nextIndex - 1;
                    advance = false;
                }
            }
            if (i < 0 || i >= n || i + n >= nextn) {
                int sc;
                if (finishing) {// 完成resize，将nextTab赋值给table
                    nextTable = null;
                    table = nextTab;
                    sizeCtl = (n << 1) - (n >>> 1);
                    return;
                }
                if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {
                    if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT)
                        return;
                    finishing = advance = true;
                    i = n; // recheck before commit
                }
            }
            else if ((f = tabAt(tab, i)) == null)// 如果i位置为null，则使用CAS将i位置设置为fwd，其它线程发现该位置为ForwardingNode结点（其hash值为MOVED）就知道i处的结点正在做resize，则跳过这个位置去resize其它位置
                advance = casTabAt(tab, i, null, fwd);// 如果CAS成功，advance为true，
            else if ((fh = f.hash) == MOVED)// 
                advance = true; // already processed// ForwardingNode结点，已经resize，跳过
            else {
                synchronized (f) { // f已在前面的else if分支被赋值，执行到这里说明它不是ForwardingNode结点，则该线程如果获取锁成功，则可以对f代表的链表或红黑树进行resize操作
                            // 线程能执行到这里，说明i的位置原本就有值，所有需要锁定该位置，然后进行rehash；最终还是会把i的位置设置为ForwardingNode类型，告知其它线程该位置已经被处理过
                    if (tabAt(tab, i) == f) {// 确保i位置的元素真的是f，即确保i位置没有被resize过，因为后面会为i位置设置新值
                        Node<K,V> ln, hn;
                        if (fh >= 0) { // 正常Node结点的hash是>=0的
                            // 以下处理链表的resize，将原链表拆解为高位链表、低位链表
                            int runBit = fh & n;
                            Node<K,V> lastRun = f;
                            for (Node<K,V> p = f.next; p != null; p = p.next) {
                                int b = p.hash & n;
                                if (b != runBit) {
                                    runBit = b;
                                    lastRun = p;
                                }
                            }
                            if (runBit == 0) {
                                ln = lastRun;
                                hn = null;
                            }
                            else {
                                hn = lastRun;
                                ln = null;
                            }
                            for (Node<K,V> p = f; p != lastRun; p = p.next) {
                                int ph = p.hash; K pk = p.key; V pv = p.val;
                                if ((ph & n) == 0)
                                    ln = new Node<K,V>(ph, pk, pv, ln);
                                else
                                    hn = new Node<K,V>(ph, pk, pv, hn);
                            }
                            // 使用CAS分别将拆解出来的两个链表放到nextTab中。
                            // 同时将fwd结点放入table中，这样，在其它线程put时，发现该结点是ForwardingNode结点，表示正在进行resize，这个线程调用helpTransfer方法帮助resize；并且其它线程在resize时，也会跳过该位置
                            setTabAt(nextTab, i, ln);
                            setTabAt(nextTab, i + n, hn);
                            setTabAt(tab, i, fwd);
                            advance = true;
                        }
                        else if (f instanceof TreeBin) {// TreeBin的hash值是TREEBIN
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> lo = null, loTail = null;
                            TreeNode<K,V> hi = null, hiTail = null;
                            int lc = 0, hc = 0;
                            
                            // 同样将红黑树拆解为高位链表、低位链表
                            for (Node<K,V> e = t.first; e != null; e = e.next) {
                                int h = e.hash;
                                TreeNode<K,V> p = new TreeNode<K,V>
                                    (h, e.key, e.val, null, null);
                                if ((h & n) == 0) {
                                    if ((p.prev = loTail) == null)
                                        lo = p;
                                    else
                                        loTail.next = p;
                                    loTail = p;
                                    ++lc;
                                }
                                else {
                                    if ((p.prev = hiTail) == null)
                                        hi = p;
                                    else
                                        hiTail.next = p;
                                    hiTail = p;
                                    ++hc;
                                }
                            }
                            
                            // 如果需要，将链表构造为红黑树
                            ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) :
                                (hc != 0) ? new TreeBin<K,V>(lo) : t;
                            hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) :
                                (lc != 0) ? new TreeBin<K,V>(hi) : t;
                            
                            // 使用CAS将红黑树放入nextTable。
                            // 同时将fwd放入table中，这样，在其它线程put时，发现该结点是ForwardingNode结点，表示正在进行resize，这个线程调用helpTransfer方法帮助resize；并且其它线程在resize时，也会跳过该位置
                            setTabAt(nextTab, i, ln);
                            setTabAt(nextTab, i + n, hn);
                            setTabAt(tab, i, fwd);
                            advance = true;
                        }
                    }
                }
            }
        }
    }
```

## get操作

## remove操作

## 总结