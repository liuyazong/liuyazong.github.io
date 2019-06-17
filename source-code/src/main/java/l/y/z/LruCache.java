package l.y.z;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * author: liuyazong <br>
 * datetime: 2019-06-17 13:44 <br>
 * <p></p>
 */
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
