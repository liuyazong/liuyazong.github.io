# HashMap

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
     * 扩容时需要分配的数组的长度
     */
    int threshold;

    /**
     * 加载因子
     */
    final float loadFactor;        
```

## put操作

```java
    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }

    /**
     * Implements Map.put and related methods.
     *
     * @param hash hash for key
     * @param key the key
     * @param value the value to put
     * @param onlyIfAbsent if true, don't change existing value
     * @param evict if false, the table is in creation mode.
     * @return previous value, or null if none
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
        
        如果table在计算出索引（idx）位置的值不为null
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
```