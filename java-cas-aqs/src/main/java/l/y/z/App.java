package l.y.z;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.LockSupport;

@Slf4j
public class App {
    public static void main(String[] args) {
        lockSupport();
    }

    private static void lockSupport() {

        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {

            Thread park = new Thread(() -> {
                String name = Thread.currentThread().getName();
//                log.info("{} before park", name);
                LockSupport.park(App.class);
                log.info("{} after  park", name);
            }, String.format("T-%s", i));
            park.start();
            threads[i] = park;
        }

        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                Thread park = threads[i];
//                log.info("{} before unpark", park.getName());
                LockSupport.unpark(park);
                log.info("{} after  unpark", park.getName());
            }

        }).start();

    }
}
