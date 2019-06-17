# LinkedHashMap

```java
public class LinkedHashMap<K,V>
    extends HashMap<K,V>
    implements Map<K,V>
{
    //...
}
```

从LinkedHashMap的类定义可以看出，LinkedHashMap继承了HashMap，它具有HashMap的所有性质。以下主要看其与HashMap不同的地方。

其经典应用场景是实现LRU缓存，文章最后会给出一个简单的实现。

## 内部属性

```java
    /**
     * 双向链表的头结点
     */
    transient LinkedHashMap.Entry<K,V> head;

    /**
     * 双向链表的尾结点
     */
    transient LinkedHashMap.Entry<K,V> tail;

    /**
     * 维护结点顺序，true：维护访问顺序；false：维护插入顺序
     */
    final boolean accessOrder;
```

## 结点结构

```java
    static class Entry<K,V> extends HashMap.Node<K,V> {
        Entry<K,V> before, after;
        Entry(int hash, K key, V value, Node<K,V> next) {
            super(hash, key, value, next);
        }
    }
```

Entry继承了HashMap的Node，并新增两个属性before和after，用来维护LinkedHashMap中的结点顺序的视图（所有结点组成的双向链表）。

## put操作

以下代卖来自HashMap，LinkedHashMap并未重写该方法，而是使用钩子的方式来实现结点顺序的维护，具体由三个方法：

1. newNode

    HashMap中有实现，LinkedHashMap对其进行了重写

2. afterNodeAccess

    HashMap中是一个空方法，LinkedHashMap对其进行了重写

3. afterNodeInsertion

    HashMap中是一个空方法，LinkedHashMap对其进行了重写


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
            
            // 这里构造结点与HashMap不同
            tab[i] = newNode(hash, key, value, null);
        
        // 如果table在计算出索引（idx）位置的值不为null
        else {
            Node<K,V> e; K k;
            
            // p的hash与当前k的hash相等，且p的key与当前k的值相等，则视为同一个K，后面会对e处理
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                e = p;
            
            // 表示有hash冲突，无论链表还是红黑树都是为了解决hash冲突
            // 如果p的类型是TreeNode，则执行TreeNode节点的插入，即将当前k-v插入到红黑树中    
            else if (p instanceof TreeNode)
                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
            else {

                // 构造一个Node结点，并将其插入到链表的尾部；并且，在需要的时候，将该链表转换为红黑树
                for (int binCount = 0; ; ++binCount) {

                    // 如果p的next节点（e）为空，则使用当前k-v构建Node并将其作为p的next节点
                    if ((e = p.next) == null) {
                        
                        // 这里构造结点与HashMap不同
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

下面看看以上三个方法的实现及作用

### nowNode

这里创建新的Entry结点，并将该几点放到双向链表的尾部。

```java
    // 普通结点
    Node<K,V> newNode(int hash, K key, V value, Node<K,V> e) {
        LinkedHashMap.Entry<K,V> p =
            new LinkedHashMap.Entry<K,V>(hash, key, value, e);
        linkNodeLast(p);
        return p;
    }
    // 红黑树结点
    TreeNode<K,V> newTreeNode(int hash, K key, V value, Node<K,V> next) {
        TreeNode<K,V> p = new TreeNode<K,V>(hash, key, value, next);
        linkNodeLast(p);
        return p;
    }
    
    private void linkNodeLast(LinkedHashMap.Entry<K,V> p) {
        LinkedHashMap.Entry<K,V> last = tail;
        tail = p;
        if (last == null)
            head = p;
        else {
            p.before = last;
            last.after = p;
        }
    }
```

### afterNodeAccess

如果accessOrder为true，并且该结点不是双向链表的尾结点，则将该结点移动到双向链表的尾部

```java
    void afterNodeAccess(Node<K,V> e) { // move node to last
        LinkedHashMap.Entry<K,V> last;
        if (accessOrder && (last = tail) != e) {
            LinkedHashMap.Entry<K,V> p =
                (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
            
            // 将p从双向链表中删除
            p.after = null; // 断开p与其后继结点的链接
            
            if (b == null) // p无前驱结点，则将双向链表的头结点设置为p的后继结点，因为p要作为尾结点
                head = a;
            else // p有前驱结点，则使p的前驱结点的后继结点直接指向p的后街结点，即断开p的前驱结点与p的链接
                b.after = a;
            
            if (a != null) // p有后继结点，则将p的后继结点的前驱结点指向p的前驱结点，即断开p的后继结点与p的链接。到这里就将p从双向链表中删除了。
                a.before = b;
            else // p无后继结点，使将p的前驱结点作为双向链表的尾结点
                last = b;
            
            // 将p添加到双向链表的尾部
            if (last == null) // 
                head = p;
            else {
                p.before = last;
                last.after = p;
            }
            // tail指向指向p结点
            tail = p;
            ++modCount;
        }
    }
```


### afterNodeInsertion

LinkedHashMap中removeEldestEntry始终返回false，实际这里也没做什么。

```java
    void afterNodeInsertion(boolean evict) { // possibly remove eldest
        LinkedHashMap.Entry<K,V> first;
        if (evict && (first = head) != null && removeEldestEntry(first)) { // LinkedHashMap中removeEldestEntry始终返回false
            K key = first.key;
            removeNode(hash(key), key, null, false, true);
        }
    }
```

## resize操作

LinkedHashMap未重写该方法。

## get操作

LinkedHashMap重写了该方法。首先get方法调用了父类HashMap的getNode方法，如果查找到该结点，则调用afterNodeAccess方法进行顺序维护的操作

```java
    /**
     * 调用父类HashMap的getNode方法，如果查找到该结点，则进行维护顺序的操作
     */
    public V get(Object key) {
        Node<K,V> e;
        if ((e = getNode(hash(key), key)) == null)
            return null;
        if (accessOrder)
            afterNodeAccess(e);
        return e.value;
    }
```

## remove操作

以下代码来自父类HashMap，LinkedHashMap没有重写该方法，同get操作一样，使用钩子的方式来实现结点顺序的维护：

1. afterNodeRemoval：HashMap中是一个空方法，LinkedHashMap对其进行了重写

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
            
            // 查找key对应的结点
            Node<K,V> node = null, e; K k; V v;
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))// node结点是p，即node结点是链表头结点或红黑树跟结点
                node = p;
            else if ((e = p.next) != null) { // 是红黑树或者链表
                if (p instanceof TreeNode)
                    node = ((TreeNode<K,V>)p).getTreeNode(hash, key);
                else {
                    
                    // 从链表查找key对应的结点，且始终保证p是node的前一个结点
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
            
            // 从红黑树中、或者从链表中将结点node删除
            if (node != null && (!matchValue || (v = node.value) == value ||
                                 (value != null && value.equals(v)))) {
                
                // 从红黑树中删除node结点
                if (node instanceof TreeNode)
                    ((TreeNode<K,V>)node).removeTreeNode(this, tab, movable);
                
                // node结点是链表头结点
                else if (node == p)
                    tab[index] = node.next;
                
                // p是node的前一个结点
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

下面来看看afterNodeRemoval方法

### afterNodeRemoval

该方法将结点从双向链表中删除

```java
    void afterNodeRemoval(Node<K,V> e) { // unlink
        LinkedHashMap.Entry<K,V> p =
            (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
        p.before = p.after = null;
        if (b == null)
            head = a;
        else
            b.after = a;
        if (a == null)
            tail = b;
        else
            a.before = b;
    }
```

那究竟结点顺序是从哪里提现出来的呢？？

## 结点顺序

分别看LinkedKeySet、LinkedValues、LinkedEntrySet的forEach方法

```java
        public final void forEach(Consumer<? super K> action) {
            if (action == null)
                throw new NullPointerException();
            int mc = modCount;
            for (LinkedHashMap.Entry<K,V> e = head; e != null; e = e.after)
                action.accept(e.key);
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
        
        public final void forEach(Consumer<? super V> action) {
            if (action == null)
                throw new NullPointerException();
            int mc = modCount;
            for (LinkedHashMap.Entry<K,V> e = head; e != null; e = e.after)
                action.accept(e.value);
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
        
        public final void forEach(Consumer<? super Map.Entry<K,V>> action) {
            if (action == null)
                throw new NullPointerException();
            int mc = modCount;
            for (LinkedHashMap.Entry<K,V> e = head; e != null; e = e.after)
                action.accept(e);
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
```

还有LinkedHashMap的forEach方法

```java
    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (action == null)
            throw new NullPointerException();
        int mc = modCount;
        for (LinkedHashMap.Entry<K,V> e = head; e != null; e = e.after)
            action.accept(e.key, e.value);
        if (modCount != mc)
            throw new ConcurrentModificationException();
    }
```

这些forEach方法都是从双向链表的头结点开始遍历，直到没有更多元素。

以及LinkedHashMap内部定义迭代器

```java
abstract class LinkedHashIterator {
        LinkedHashMap.Entry<K,V> next;
        LinkedHashMap.Entry<K,V> current;
        int expectedModCount;
        
        // next指向双向链表的头结点
        LinkedHashIterator() {
            next = head;
            expectedModCount = modCount;
            current = null;
        }

        public final boolean hasNext() {
            return next != null;
        }

        // 而获取nextNode时，获取的是当前结点的后继结点
        final LinkedHashMap.Entry<K,V> nextNode() {
            LinkedHashMap.Entry<K,V> e = next;
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (e == null)
                throw new NoSuchElementException();
            current = e;
            next = e.after;
            return e;
        }

        public final void remove() {
            Node<K,V> p = current;
            if (p == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            current = null;
            K key = p.key;
            
            // 这里调用了LinkedHashMap的removeNode方法，也会处罚结点顺序维护的操作
            removeNode(hash(key), key, null, false, false);
            expectedModCount = modCount;
        }
    }
    
    // 以下三个迭代器都调用了父类LinkedHashIterator的nextNode结点
    final class LinkedKeyIterator extends LinkedHashIterator
        implements Iterator<K> {
        public final K next() { return nextNode().getKey(); }
    }

    final class LinkedValueIterator extends LinkedHashIterator
        implements Iterator<V> {
        public final V next() { return nextNode().value; }
    }

    final class LinkedEntryIterator extends LinkedHashIterator
        implements Iterator<Map.Entry<K,V>> {
        public final Map.Entry<K,V> next() { return nextNode(); }
    }
```

迭代器也是按照双向链表的顺序来返回相应的值，直到没有更多结点。

从上可以看出，无论是forEach方法，还是迭代器的next方法，都是按照LinkedHashMap中维护的一个双向链表来进行输出的。

## 总结

LinkedHashMap内部维护了一个双向链表来维护结点遍历时的输出顺序。

LinkedHashMap的典型应用是实现基于LRU算法的缓存，那么，该怎样实现一个固定大小的LRU缓存呢？？？

答案就在afterNodeInsertion方法内调用的removeEldestEntry方法中。removeEldestEntry方法的含义是"是否删除'最老'的结点，即双向链表的头部结点"。

实现如下：

```java
public class LruCache<K, V> extends LinkedHashMap<K, V> {

    /**
     * key-value对的最大数量，当size()方法返回值大于这个值，则进行旧结点的删除
     */
    private int maxKeyCount;
    private static final int INITIAL_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75F;

    /**
     * 只有当accessOrder为true时，才具有LRU的特性；否则，只维护写入顺序
     */
    private static final boolean ACCESS_ORDER = true;

    public LruCache(Integer maxKeyCount) {
        super(INITIAL_CAPACITY, LOAD_FACTOR, ACCESS_ORDER);
        this.maxKeyCount = maxKeyCount;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return super.size() > maxKeyCount;
    }
}
```

测试程序如下：

```java
    @Test
    public void test() {
        int maxKeyCount = 16;
        LruCache<String, String> cache = new LruCache<>(maxKeyCount);

        for (int i = 0; i < maxKeyCount << 1; i++) {
            String key = i + "";
            String value = key;
            String put = cache.put(key, value);// put为null
        }

        // 这里应该输出按 16 - 31 顺序的共16个key
        log.info("keySet: {}", cache.keySet());
        log.info("values: {}", cache.values());
        log.info("entrySet: {}", cache.entrySet());

        String s = cache.get("16");
        log.info("lru cache get {} --->> {}", "16", s);

        // 这里应该输出按 17 - 31, 16 顺序的共16个key
        log.info("keySet: {}", cache.keySet());
        log.info("values: {}", cache.values());
        log.info("entrySet: {}", cache.entrySet());


        String put = cache.put("17", "1717");
        log.info("cache put: {} --- >> {}, {}", "17", "1717", put);

        // 这里应该输出按 18 - 31, 16, 17 顺序的共16个key
        log.info("keySet: {}", cache.keySet());
        log.info("values: {}", cache.values());
        log.info("entrySet: {}", cache.entrySet());
    }
```