package l.y.z;

import java.util.concurrent.ConcurrentHashMap;

/**
 * author: liuyazong <br>
 * datetime: 2019-06-17 15:51 <br>
 * <p></p>
 */
public class ConcurrentHashMapTest {
    public static void main(String[] args) {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        map.put("1", "1");
    }
}
