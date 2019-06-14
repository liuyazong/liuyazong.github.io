package l.y.z;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

public class App {
    public static void main(String[] args) {
        //1000000000，1002960701
        Integer min = 1300000001;
        Integer max = 1300112114;

        Collection<Future<List<Integer>>> futures = new ArrayList<>();
        while (true) {
            Integer t = min + 10000 - 1; // 加法溢出？？？！！！
            boolean done = t >= max;

            if (done) {
                t = max;
            }
            // 查询 [min,t] 之间需要计息的user_code，平均10000的区间内大概有100个user_code需要计息
            Integer finalMin = min;
            Integer finalT = t;
            System.err.println(String.format("%s, %s", finalMin,finalT));
            if (done) {
                break;
            }
            min += 10000;
        }
    }
}
