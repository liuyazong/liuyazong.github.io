# HashMap

主要看put、get、remove、resize操作，不看红黑树的结点插入、删除、查找等操作

## 内部属性

静态属性

```java
    /**
     * 默认初始化容量，值为16，必须是2的n次方
     */
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16
    
    /**
     * 默认加载因子
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;  
    
    /**
     * 链表需要转红黑树时的长度。
     * 此时未必会做换红黑树的操作，需要结合MIN_TREEIFY_CAPACITY，即链表长度达到8且容量达到64时，才会做红黑树的转换；
     * 否则，进行扩容操作。
     */
    static final int TREEIFY_THRESHOLD = 8;    
    
    /**
     * 红黑树转链表时的元素个数
     */
    static final int UNTREEIFY_THRESHOLD = 6;


    /**
     * 
     */
    static final int MIN_TREEIFY_CAPACITY = 64;    
```

实例属性

```java
    /**
     * 内部数组，数组元素可能是链表头节点，也可能是红黑树跟节点
     */
    transient Node<K,V>[] table;

    /**
     * key-value对的数量
     */
    transient int size;
    
    /**
     * 当size大于threshold时，需要进行扩容
     */
    int threshold;

    /**
     * 加载因子
     */
    final float loadFactor;        
```

## 结点结构

```java
    /**
     * 链表结点，实现Map.Entry接口
     */
    static class Node<K,V> implements Map.Entry<K,V> {
        final int hash;
        final K key;
        V value;
        Node<K,V> next;
        
        // ... 
    }
    
    /**
     * LinkedHashMap中元素的结点类型
     */
    static class Entry<K,V> extends HashMap.Node<K,V> {
        Entry<K,V> before, after;
        Entry(int hash, K key, V value, Node<K,V> next) {
            super(hash, key, value, next);
        }
    }    
    
    /**
     * 红黑树结点，继承了LinkedHashMap.Entry，间接继承了HashMap.Node，所以也具有链表的性质。
     * 实际上该结点类型既可以作为红黑树结点，又可以作为双向链表结点。
     */
    static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> {
        TreeNode<K,V> parent;  // red-black tree links
        TreeNode<K,V> left;
        TreeNode<K,V> right;
        TreeNode<K,V> prev;    // needed to unlink next upon deletion
        boolean red;
        
        // ...
    }    
```

## put操作

```java
    /**
     * 将k-v对放入table中
     */
    public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }

    /**
     * 将k-v对放入table中
     */
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        Node<K,V>[] tab; Node<K,V> p; int n, i;
        
        // 如果table数组为null或者table数组长度为0，则调用resize方法初始化table数组，返回数组长度
        if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;
        
        // 如果table在计算出索引（idx）位置的值为null，则将新的k-v对构建为Node节点放在table数组的这个位置
        // 同时这里为p变量赋值了
        if ((p = tab[i = (n - 1) & hash]) == null)
            tab[i] = newNode(hash, key, value, null);
        
        // 如果table在计算出索引（idx）位置的值不为null
        else {
            Node<K,V> e; K k;
            
            // p的hash与当前k的hash相等，且p的key与当前k的值相等，则视为同一个K，后面会对e处理
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                e = p;
            
            // 如果p的类型是TreeNode，则执行TreeNode节点的插入，即将当前k-v插入到红黑树中    
            else if (p instanceof TreeNode)
                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
            else {

                // 到这里就表示有hash冲突了
                for (int binCount = 0; ; ++binCount) {

                    // 如果p的next节点（e）为空，则使用当前k-v构建Node并将其作为p的next节点
                    if ((e = p.next) == null) {
                        p.next = newNode(hash, key, value, null);
                        
                        // 当循环到第8次，binCount = 7，条件为真，对table进行扩容或者将当前idx处的链表转换为红黑树具体看treeifyBin方法
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                            treeifyBin(tab, hash);

                        // 到这里表示k-v是一个全新的节点，并且已经放入table中，循环结束    
                        break;
                    }

                    // 如果p的next节点（e）不为null，且e节点的hash与当前k的hash相等、e节点的key与当前k相等，则视为空一格K，循环结束，后面会对e处理
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        break;

                    // 如果前面两个if都不成立，继续查看链表的下一个节点是否满足上面的两个if语句片，直到链表的结尾即p.next值为null    
                    p = e;
                }
            }

            // 条件为真，表示有相同的K进来了，根据onlyIfAbsent和当前已映射的v值是否为空来进行节点value的操作
            if (e != null) { // existing mapping for key
                V oldValue = e.value;
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;

                // LinkedHashMap有实现，用来维护节点的添加或者访问顺序    
                afterNodeAccess(e);

                // 返回该key关联的旧值
                return oldValue;
            }
        }

        ++modCount;

        // 执行到这里说明在putVal执行之前当前key在table中不存在，size记录的是key的个数
        // 当size > 数组长度*0.75时，对table数组进行扩容
        if (++size > threshold)
            resize();
        
        // LinkedHashMap有实现，用来维护节点的添加或者访问顺序
        afterNodeInsertion(evict);
        return null;
    }
    
    /**
     * 当table太小时，使用扩容操作，而不是使用红黑树替换链表
     */
    final void treeifyBin(Node<K,V>[] tab, int hash) {
        int n, index; Node<K,V> e;
        
        //如果table为null，或者table的长度小于MIN_TREEIFY_CAPACITY（64），进行扩容操作；否则，转为红黑树
        if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
            resize();
        else if ((e = tab[index = (n - 1) & hash]) != null) {
            
            // 分别使用每个k-v对构造一个TreeNode结点，进而使用这些TreeNode结点构造了一个双向链表，hd、tl分别是头结点、尾结点
            TreeNode<K,V> hd = null, tl = null;
            do {
                TreeNode<K,V> p = replacementTreeNode(e, null);
                if (tl == null)
                    hd = p;
                else {
                    p.prev = tl;
                    tl.next = p;
                }
                tl = p;
            } while ((e = e.next) != null);
            
            if ((tab[index] = hd) != null)
                
                // 将双向链表构造为红黑树，并将这颗红黑树放入table中对应的下标处
                hd.treeify(tab);
        }
    }


        /**
         * TreeNode中的方法，将之前使用TreeNode构造的双向链表转换为红黑树
         */
        final void treeify(Node<K,V>[] tab) {
            TreeNode<K,V> root = null;
            for (TreeNode<K,V> x = this, next; x != null; x = next) {
                next = (TreeNode<K,V>)x.next;
                x.left = x.right = null;
                if (root == null) {
                    x.parent = null;
                    x.red = false;
                    root = x;
                }
                else {
                    K k = x.key;
                    int h = x.hash;
                    Class<?> kc = null;
                    for (TreeNode<K,V> p = root;;) {
                        int dir, ph;
                        K pk = p.key;
                        if ((ph = p.hash) > h)
                            dir = -1;
                        else if (ph < h)
                            dir = 1;
                        else if ((kc == null &&
                                  (kc = comparableClassFor(k)) == null) ||
                                 (dir = compareComparables(kc, k, pk)) == 0)
                            dir = tieBreakOrder(k, pk);

                        TreeNode<K,V> xp = p;
                        if ((p = (dir <= 0) ? p.left : p.right) == null) {
                            x.parent = xp;
                            if (dir <= 0)
                                xp.left = x;
                            else
                                xp.right = x;
                            root = balanceInsertion(root, x);
                            break;
                        }
                    }
                }
            }
            
            // 将构造的红黑树放入table对应的下标出，并将红黑树的跟结点移动到双向链表的头部
            moveRootToFront(tab, root);
        }


        /**
         * TreeNode中的方法，将构造的红黑树放入table对应的下标处，并将红黑树的跟结点移动到双向链表的头部
         */
        static <K,V> void moveRootToFront(Node<K,V>[] tab, TreeNode<K,V> root) {
            int n;
            if (root != null && tab != null && (n = tab.length) > 0) {
                int index = (n - 1) & root.hash;
                TreeNode<K,V> first = (TreeNode<K,V>)tab[index];
                if (root != first) {
                    Node<K,V> rn;
                    
                    // 将构造的红黑树放入table相应的下标处
                    tab[index] = root;
                    
                    // 将红黑树跟结点移动到双向链表的头部
                    TreeNode<K,V> rp = root.prev;
                    if ((rn = root.next) != null)
                        ((TreeNode<K,V>)rn).prev = rp;
                    if (rp != null)
                        rp.next = rn;
                    if (first != null)
                        first.prev = root;
                    root.next = first;
                    root.prev = null;
                }
                assert checkInvariants(root);
            }
        }
```

## resize操作

1. 如果table未初始化，则对table初始化
2. 否则，将table数字大小扩展为原来的2倍
3. 将原table中的元素移动到新table的相应下标处

```java
    /**
     * 对table初始化或者2倍扩容，返回新的table。
     */
    final Node<K,V>[] resize() {
        Node<K,V>[] oldTab = table;
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        int oldThr = threshold;
        int newCap, newThr = 0;
        if (oldCap > 0) {
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            }
            
            // 计算扩容后的table数组大小和下次需要扩容时size的临界值
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                     oldCap >= DEFAULT_INITIAL_CAPACITY)
                newThr = oldThr << 1; // double threshold
        }
        else if (oldThr > 0) // initial capacity was placed in threshold
            newCap = oldThr;
        else {               // zero initial threshold signifies using defaults
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                      (int)ft : Integer.MAX_VALUE);
        }
        // 创建新的table数组
        threshold = newThr;
        @SuppressWarnings({"rawtypes","unchecked"})
        Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
        table = newTab;
        
        // 移动old table中的结点到新table的相应下标处
        if (oldTab != null) {
            for (int j = 0; j < oldCap; ++j) {
                Node<K,V> e;
                if ((e = oldTab[j]) != null) {
                    oldTab[j] = null;
                    if (e.next == null)// 表示j下标处只有一个结点
                        
                        // 将结点e放入新table的相应下标处
                        newTab[e.hash & (newCap - 1)] = e;
                    else if (e instanceof TreeNode)
                        
                        // 将红黑树分解为两个双向链表，如果双向链表长度大于6，则分解出来的双向链表转换为红黑树。
                        // 然后将双向链表或红黑树放入新table的相应下标处
                        ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                    else { // preserve order // 处理链表，将原链表拆分为两个（至少一个）链表，并且维持元素在原链表中的相对顺序
                        Node<K,V> loHead = null, loTail = null;
                        Node<K,V> hiHead = null, hiTail = null;
                        Node<K,V> next;
                        do {
                            next = e.next;
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null)
                                    loHead = e;
                                else
                                    loTail.next = e;
                                loTail = e;
                            }
                            else {
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        
                        // 将新的链表放入新table的相应下标处
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        return newTab;
    }
```

## get操作

返回key关联的value值；如果key不存在，返回null

```java
    /**
     * 
     */
    public V get(Object key) {
        Node<K,V> e;
        return (e = getNode(hash(key), key)) == null ? null : e.value;
    }

    /**
     * 
     */
    final Node<K,V> getNode(int hash, Object key) {
        Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
        if ((tab = table) != null && (n = tab.length) > 0 &&
            (first = tab[(n - 1) & hash]) != null) {
            if (first.hash == hash && // always check first node
                ((k = first.key) == key || (key != null && key.equals(k))))
                return first;
            if ((e = first.next) != null) {
                if (first instanceof TreeNode)
                    return ((TreeNode<K,V>)first).getTreeNode(hash, key);
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;
                } while ((e = e.next) != null);
            }
        }
        return null;
    }
```

## remove操作

删除key，返回key关联的value；如果key不存在，返回null

```java
    /**
     * 
     */
    public V remove(Object key) {
        Node<K,V> e;
        return (e = removeNode(hash(key), key, null, false, true)) == null ?
            null : e.value;
    }

    /**
     * 
     */
    final Node<K,V> removeNode(int hash, Object key, Object value,
                               boolean matchValue, boolean movable) {
        Node<K,V>[] tab; Node<K,V> p; int n, index;
        if ((tab = table) != null && (n = tab.length) > 0 &&
            (p = tab[index = (n - 1) & hash]) != null) {
            Node<K,V> node = null, e; K k; V v;
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                node = p;
            else if ((e = p.next) != null) {
                if (p instanceof TreeNode)
                    node = ((TreeNode<K,V>)p).getTreeNode(hash, key);
                else {
                    do {
                        if (e.hash == hash &&
                            ((k = e.key) == key ||
                             (key != null && key.equals(k)))) {
                            node = e;
                            break;
                        }
                        p = e;
                    } while ((e = e.next) != null);
                }
            }
            if (node != null && (!matchValue || (v = node.value) == value ||
                                 (value != null && value.equals(v)))) {
                if (node instanceof TreeNode)
                    ((TreeNode<K,V>)node).removeTreeNode(this, tab, movable);
                else if (node == p)
                    tab[index] = node.next;
                else
                    p.next = node.next;
                ++modCount;
                --size;
                afterNodeRemoval(node);
                return node;
            }
        }
        return null;
    }
```