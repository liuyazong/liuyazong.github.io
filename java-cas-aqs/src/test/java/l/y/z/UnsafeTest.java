package l.y.z;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class UnsafeTest {


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

    @Test
    public void test() throws Exception {
        //获取Unsafe实例
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
        theUnsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) theUnsafeField.get(null);
        log.info("unsafe: {}", unsafe);

        DataContainer dataContainer = new DataContainer(1, 2, 3L, 4L, new Object(), new Object());
        log.info("data: {}", dataContainer);
        {
            //更新 int v1
            long offset = unsafe.objectFieldOffset(DataContainer.class.getDeclaredField("v1"));
            log.info("更新前 data.v1: {}", unsafe.getInt(dataContainer, offset));
            boolean cas = unsafe.compareAndSwapInt(dataContainer, offset, unsafe.getInt(dataContainer, offset), 11);
            log.info("更新后 data.v1: {}, cas: {}", unsafe.getInt(dataContainer, offset), cas);
        }

        {
            //更新 volatile int v2
            long offset = unsafe.objectFieldOffset(DataContainer.class.getDeclaredField("v2"));
            log.info("更新前 data.v2: {}", unsafe.getIntVolatile(dataContainer, offset));
            boolean cas = unsafe.compareAndSwapInt(dataContainer, offset, unsafe.getIntVolatile(dataContainer, offset), 22);
            log.info("更新后 data.v2: {}, cas: {}", unsafe.getIntVolatile(dataContainer, offset), cas);
        }
        {
            //更新 long v3
            long offset = unsafe.objectFieldOffset(DataContainer.class.getDeclaredField("v3"));
            log.info("更新前 data.v3: {}", unsafe.getLong(dataContainer, offset));
            boolean cas = unsafe.compareAndSwapLong(dataContainer, offset, unsafe.getLong(dataContainer, offset), 33L);
            log.info("更新后 data.v3: {}, cas: {}", unsafe.getLong(dataContainer, offset), cas);
        }

        {
            //更新 volatile long v2
            long offset = unsafe.objectFieldOffset(DataContainer.class.getDeclaredField("v4"));
            log.info("更新前 data.v4: {}", unsafe.getLongVolatile(dataContainer, offset));
            boolean cas = unsafe.compareAndSwapLong(dataContainer, offset, unsafe.getLongVolatile(dataContainer, offset), 44L);
            log.info("更新后 data.v4: {}, cas: {}", unsafe.getLongVolatile(dataContainer, offset), cas);
        }

        {
            //更新 Object v5
            long offset = unsafe.objectFieldOffset(DataContainer.class.getDeclaredField("v5"));
            log.info("更新前 data.v5: {}", unsafe.getObject(dataContainer, offset));
            boolean cas = unsafe.compareAndSwapObject(dataContainer, offset, unsafe.getObject(dataContainer, offset), new Object());
            log.info("更新后 data.v5: {}, cas: {}", unsafe.getObject(dataContainer, offset), cas);
        }

        {
            //更新 volatile Object v5
            long offset = unsafe.objectFieldOffset(DataContainer.class.getDeclaredField("v6"));
            log.info("更新前 data.v6: {}", unsafe.getObjectVolatile(dataContainer, offset));
            boolean cas = unsafe.compareAndSwapObject(dataContainer, offset, unsafe.getObjectVolatile(dataContainer, offset), new Object());
            log.info("更新后 data.v6: {}, cas: {}", unsafe.getObjectVolatile(dataContainer, offset), cas);
        }
    }

    @Data
    @AllArgsConstructor
    private static class DataContainer {
        private int v1;
        private volatile int v2;
        private long v3;
        private volatile long v4;
        private Object v5;
        private volatile Object v6;
    }

    public static class Counter {
        private volatile int value;
        private Unsafe unsafe;
        private long valueOffset;

        Counter(int value) throws Exception {
            this.value = value;
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            unsafe = (Unsafe) theUnsafeField.get(null);
            valueOffset = unsafe.objectFieldOffset(Counter.class.getDeclaredField("value"));
        }

        int increment() {
            int t;
            do {
                t = unsafe.getIntVolatile(this, valueOffset);
            } while (!unsafe.compareAndSwapInt(this, valueOffset, t, t + 1));
            return t;
        }
    }
}
