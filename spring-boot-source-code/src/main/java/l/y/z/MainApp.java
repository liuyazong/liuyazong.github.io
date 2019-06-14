package l.y.z;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * author: liuyazong <br>
 * datetime: 2019-01-15 10:16 <br>
 * <p></p>
 */
@SpringBootApplication
public class MainApp {

    public static void main(String[] args) {
        System.err.println(Runtime.getRuntime().availableProcessors());
        SpringApplication.run(MainApp.class, args);
    }
}
