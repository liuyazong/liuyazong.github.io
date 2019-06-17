package l.y.z;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * author: liuyazong <br>
 * datetime: 2019-06-17 13:46 <br>
 * <p></p>
 */
@Slf4j
public class LruCacheTest {

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
}
