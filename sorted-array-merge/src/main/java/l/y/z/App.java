package l.y.z;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class App {

    public static void main(String[] args) throws IOException {

        /*// 待合并序列
        int[] arr1 = new int[]{1, 3, 5, 7, 9, 10, 11, 12, 14, 17, 18};
        int[] arr2 = new int[]{0, 2, 3, 4, 6, 8, 11, 13, 15, 16, 19};
        int[] arr3 = new int[]{-20, -1, 0, 1, 20, 20};

        // 头指针
        int h1 = 0, h2 = 0, h3 = 0;

        // 结果集，容量为待合并序列的总和
        Integer[] result = new Integer[arr1.length + arr2.length + arr3.length];

        for (int i = 0; i < result.length; i++) {

            // 找出指针处的最小值
            List<Integer> t = new ArrayList<>();

            if (h1 < arr1.length) {
                t.add(arr1[h1]);
            }
            if (h2 < arr2.length) {
                t.add(arr2[h2]);
            }
            if (h3 < arr3.length) {
                t.add(arr3[h3]);
            }

            Integer min = Collections.min(t);

            // 指针后移
            if (h1 < arr1.length && min == arr1[h1]) {
                h1++;
            } else if (h2 < arr2.length && min == arr2[h2]) {
                h2++;
            } else if (h3 < arr3.length && min == arr3[h3]) {
                h3++;
            }

            // 将最小值放入结果集
            result[i] = min;
        }

        System.err.println(String.format("合并后的结果为: %s", Arrays.asList(result)));*/

        System.err.println();

/*        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File("/JAVA_Files/csapp/log/test.txt")));
        String str;
        while (null != (str = bufferedReader.readLine())) {
            System.err.println(str);// 处理当前行数字，处理完可以直接丢弃
        }
        bufferedReader.close();*/
    }

}
