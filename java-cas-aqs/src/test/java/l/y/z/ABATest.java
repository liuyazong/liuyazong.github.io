package l.y.z;

import lombok.extern.slf4j.Slf4j;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class ABATest {

    private volatile int value;
    private Unsafe unsafe;
    private long valueOffset;

    ABATest(int value) throws Exception {
        this.value = value;
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
        theUnsafeField.setAccessible(true);
        unsafe = (Unsafe) theUnsafeField.get(null);
        valueOffset = unsafe.objectFieldOffset(UnsafeTest.Counter.class.getDeclaredField("value"));
    }

    public static void main(String[] args) throws Exception {

        CountDownLatch countDownLatch1 = new CountDownLatch(1);
        CountDownLatch countDownLatch2 = new CountDownLatch(1);
        CountDownLatch countDownLatch3 = new CountDownLatch(1);

        ABATest abaTest = new ABATest(1);
        Thread thread2 = new Thread(() -> {
            int v = abaTest.unsafe.getIntVolatile(abaTest, abaTest.valueOffset);
            try {
                countDownLatch1.await();
            } catch (InterruptedException e) {
            }
            boolean b = abaTest.unsafe.compareAndSwapInt(abaTest, abaTest.valueOffset, v, 2);
            log.info("update: {}, value: {} --->> {}", b, v, 2);
            countDownLatch3.countDown();
        });
        Thread thread1 = new Thread(() -> {
            try {
                countDownLatch3.await();
            } catch (InterruptedException e) {
            }
            int v = abaTest.unsafe.getIntVolatile(abaTest, abaTest.valueOffset);
            boolean b = abaTest.unsafe.compareAndSwapInt(abaTest, abaTest.valueOffset, v, 1);
            log.info("update: {}, value: {} --->> {}", b, v, 1);
            countDownLatch2.countDown();
        });
        Thread thread3 = new Thread(() -> {
            int v = abaTest.unsafe.getIntVolatile(abaTest, abaTest.valueOffset);
            countDownLatch1.countDown();
            try {
                countDownLatch2.await();
            } catch (InterruptedException e) {
            }
            boolean b = abaTest.unsafe.compareAndSwapInt(abaTest, abaTest.valueOffset, v, 3);
            log.info("update: {}, value: {} --->> {}", b, v, 3);
        });

        thread1.start();
        thread2.start();
        thread3.start();
    }
}
