# IP与整形值互相转换

以`10.3.6.138`为例

```java
    String ip = "10.3.6.138";
    String[] arr = ip.split("\\.");
```

## ip --->> int

```java
    int ipInt = Integer.valueOf(arr[0]) << 24
            | Integer.valueOf(arr[1]) << 16
            | Integer.valueOf(arr[2]) << 8
            | Integer.valueOf(arr[3]); // 167970442
    System.err.println(String.format("ip地址 %s 对应的整形值为 %d", ip, ipInt));
```

## int --->> ip

```java
    String ipStr = (ipInt >> 24 & 0xFF) + "." +
            (ipInt >> 16 & 0xFF) + "." +
            (ipInt >> 8 & 0xFF) + "." +
            (ipInt & 0xFF); // 10.3.6.138
    System.err.println(String.format("整形值 %d 对应的ip地址为 %s", ipInt, ipStr));
```


