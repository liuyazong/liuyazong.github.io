# TreeMap

不像HashMap那样使用`数组+链表/红黑树`的hash实现，TreeMap是完全使用`红黑树`实现的，所以，在查找性能上可能没有HashMap高效。

但是，它是一棵树，所有它是有序的，按照Comparator排序，或者key实现了Comparable接口。