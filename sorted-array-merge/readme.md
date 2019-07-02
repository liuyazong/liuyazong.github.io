# 有序数列的合并

假设有N个有序（正序或者倒序）数列。

1. 每个序列都有一个指向第一个元素的指针
2. 找出这些指针指向的元素的最小值，把这个最小值放入结果集中。假设该最小值在数列X中，则使X的指针后移一个位置，其它数列的指针不动。
3. 重复2，直到所有的序列都处理完。

## 示例代码如下

```java
    // 待合并序列
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

    System.err.println(String.format("合并后的结果为: %s", Arrays.asList(result)));
```

## 假设这些序列不在内存中而是在文件中，并且数据量是非常大的比如几十G，该怎样处理？

那么大的文件不可能全部加载到内存中。

只需要像下面这样按行读取文件，读到的每一行即是上面提到的序列头指针指向的序列头部。

```java
    BufferedReader bufferedReader = new BufferedReader(new FileReader(new File("/JAVA_Files/csapp/log/test.txt")));
    String str;
    while (null != (str = bufferedReader.readLine())) {
        System.err.println(str);// 处理当前行数字，处理完可以直接丢弃
    }
    bufferedReader.close();
```

然后依次将每次取得的最小值写入到一个新的文件，最终也就完成了文件的合并。