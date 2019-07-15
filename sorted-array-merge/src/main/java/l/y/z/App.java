package l.y.z;


import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

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

        /*System.err.println("5>>>1: "+(5>>>1));
        System.err.println("4>>>1: "+(4>>>1));
        System.err.println("3>>>1: "+(3>>>1));
        System.err.println("2>>>1: "+(2>>>1));
        System.err.println("1>>>1: "+(1>>>1));
        System.err.println("0>>>1: "+(0>>>1));

        PriorityQueue<Integer> priorityQueue = new PriorityQueue<>();
        priorityQueue.add(1);
        priorityQueue.add(5);
        priorityQueue.add(4);
        priorityQueue.add(3);
        priorityQueue.add(2);
        System.err.println(priorityQueue);

        System.err.println(3003/1000);*/

        int[] nums1 = {4, 9, 5}, nums2 = {9, 4, 9, 8, 4};



        Set<Integer> result = new HashSet<>();// 交集


        for (int i = 0; i < nums1.length; i++) {
            result.add(nums1[i]);
        }
        int size = result.size();
        Iterator<Integer> iterator = result.iterator();
        int[] ints1 = new int[size];
        for (int i = 0; i < ints1.length; i++) {
            ints1[i] = iterator.next();
        }

        result.clear();

        for (int i = 0; i < nums2.length; i++) {
            result.add(nums2[i]);
        }
        size = result.size();
        iterator = result.iterator();
        int[] ints2 = new int[size];
        for (int i = 0; i < ints2.length; i++) {
            ints2[i] = iterator.next();
        }

        for (int i = 0; i < ints1.length; i++) {
            System.err.println(ints1[i]);
        }

        for (int i = 0; i < ints2.length; i++) {
            System.out.println(ints2[i]);
        }

        result.clear();

        int max = ints1.length;
        int min = ints2.length;
        int[] mx = ints1;
        int[] mn = ints2;
        if(max<ints2.length){
            max = ints2.length;
            min = ints1.length;
            mx = ints2;
            mn = ints1;
        }

        for (int i = 0; i < max; i++) {
            result.add(mx[i]);

            if(i<min){
                if(result.add(mn[i])){

                }
            }
        }



        /*for (int i = 0; i < nums1.length; i++) {
            set.add(nums1[i]);
        }

        for (int i = 0; i < nums2.length; i++) {
            if (set.contains(nums2[i])) {
                result.add(nums2[i]);
            }
        }

        int size = result.size();
        Iterator<Integer> iterator = result.iterator();
        int[] ints = new int[size];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = iterator.next();
        }

        Integer[] integers = result.toArray(new Integer[0]);*/

    }

}
