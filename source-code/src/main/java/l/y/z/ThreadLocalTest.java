package l.y.z;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * author: liuyazong <br>
 * datetime: 2019-06-24 14:07 <br>
 * <p></p>
 */
@Slf4j
public class ThreadLocalTest {

    // thread-local变量为当前线程的名字 + 随机数
    private static final ThreadLocal<String> THREAD_LOCAL =
            ThreadLocal.withInitial(() -> Thread.currentThread().getName() + "--随机数: " + new Random().nextInt(100));

    public static void main(String[] args) {

        int nThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < nThreads << 1; i++) {
            int finalI = i;
            Future<String> submit =
                    executorService.submit(() -> {
                        String s = THREAD_LOCAL.get() + "----" + finalI;
                        THREAD_LOCAL.remove(); //注释掉这一行，则线程池里每一个线程对应的thread-local变量将不会改变
                        return s;
                    });
            futures.add(submit);
        }

        executorService.shutdown();

        futures.forEach(e -> {
            try {
                String string = e.get();
                log.info(string);
            } catch (InterruptedException | ExecutionException e1) {
                e1.printStackTrace();
            }
        });
    }
}
