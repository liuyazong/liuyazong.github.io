package l.y.z;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicStampedReference;

@Slf4j
public class AtomicStampedReferenceTest {

    public static void main(String[] args) throws InterruptedException {
        /*AtomicStampedReference<Integer> atomicStampedReference = new AtomicStampedReference<>(0, 0);
        int nThreads = Runtime.getRuntime().availableProcessors() << 1;
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        for (int i = 0; i < 320; i++) {
            executorService.execute(() -> {
                int stamp;
                Integer reference;
                do {
                    stamp = atomicStampedReference.getStamp();
                    reference = atomicStampedReference.getReference();
                } while (!atomicStampedReference.compareAndSet(reference, reference + 1, stamp, stamp + 1));
                log.info("value: {} --->> {}, stamp: {} --->> {}", reference, atomicStampedReference.getReference(), stamp, atomicStampedReference.getStamp());
            });
        }
        executorService.shutdown();
        do {
            log.debug("等待任务全部执行");
        } while (!executorService.awaitTermination(2, TimeUnit.SECONDS));
        log.info("最终结果：value：{}，stamp：{}", atomicStampedReference.getReference(), atomicStampedReference.getStamp());*/
        aba();
    }

    public static void aba() {

        CountDownLatch countDownLatch1 = new CountDownLatch(1);
        CountDownLatch countDownLatch2 = new CountDownLatch(1);
        CountDownLatch countDownLatch3 = new CountDownLatch(1);
        AtomicStampedReference<Integer> atomicStampedReference = new AtomicStampedReference<>(1, 0);

        Thread thread2 = new Thread(() -> {
            Integer v = atomicStampedReference.getReference();
            int stamp = atomicStampedReference.getStamp();
            try {
                countDownLatch1.await();
            } catch (InterruptedException e) {
            }
            boolean b = atomicStampedReference.compareAndSet(v, 2, stamp, stamp + 1);
            log.info("update: {}, value: {}  --->>  {}", b, v, 2);
            countDownLatch3.countDown();
        });
        Thread thread1 = new Thread(() -> {
            try {
                countDownLatch3.await();
            } catch (InterruptedException e) {
            }
            Integer v = atomicStampedReference.getReference();
            int stamp = atomicStampedReference.getStamp();
            boolean b = atomicStampedReference.compareAndSet(v, 1, stamp, stamp + 1);
            log.info("update: {}, value: {} --->> {}", b, v, 1);
            countDownLatch2.countDown();
        });
        Thread thread3 = new Thread(() -> {
            Integer v = atomicStampedReference.getReference();
            int stamp = atomicStampedReference.getStamp();
            countDownLatch1.countDown();
            try {
                countDownLatch2.await();
            } catch (InterruptedException e) {
            }
            boolean b = atomicStampedReference.compareAndSet(v, 3, stamp, stamp + 1);
            log.info("update: {}, value: {} --->> {}", b, v, 3);
        });

        thread1.start();
        thread2.start();
        thread3.start();

    }
}
