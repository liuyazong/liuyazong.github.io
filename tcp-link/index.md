---
layout: post
title: TCP链接的建立与断开
date: 2019-01-12
author: yz
tags: TCP, HTTP
categories: TCP, HTTP
---

# TCP链接的建立与断开

TCP链接的建立需要三次挥手，而断开需要四次挥手。

## 三次握手

什么是三次握手？如下图

三次握手数据报

![三次握手](p0.png)

三次握手流程、状态转换如图

![三次握手](p00.png)

为什么是三次握手？

第一次握手，C端向S端发送SYN，告诉S端要建立链接。如果只有这一次握手，C端无法确定S端是否收到这个SYN。

第二次握手，S端向C端发送ACK，告诉C端S端收到了它的SYN。如果只有两次握手，S端无法确定C端是否收到了这个ACK。

第三次握手，C端向S端发送ACK，告诉S端收到了它的ACK。这时候C端、S端都已确定到对方的链路是通的。所以S端不再向B端发送ACK，也就没有第四次握手了。

## 四次挥手

什么是四次握手？如下图

四次挥手数据报

![四次握手](p2.png)

四次挥手流程、状态转换如图

![四次握手](p22.png)

为什么是四次挥手？

TCP链接是全双工的，因此每个方向都必须单独进行关闭。

假设断开链接的发起方为A，另一方为B。

第一次挥手，A端向B端发送FIN，告诉B端它不再发送数据了，但是此时A端还可能需要接受A端发来的响应数据，所以不能在此处断开链接。

第二次挥手，B端向A端发送ACK，告诉A端收到了它的FIN，但是此时B端可能还有响应数据要向A端发送，所以不能在此处断开链接。

第三次挥手，B端向A端发送FIN，告诉A端我处理完你的请求、该发的数据已全部发给你了、不再向你发送数据了。

第四次挥手，A端向B端发送ACK，告诉B端收到了它的FIN，A端等待2MSL后断开链接。B端收到这个ACK后断开链接。

## 如何触发链接的关闭

对于HTTP，它的头部参数connection可以触发链接的关闭。

在HTTP 1.1版本中，connection默认为keep-alive。

下面表格基于HTTP 1.1。

|   client connection   |   server connection   |    发起断开连接请求   |     备注                      |
|:---------------------:|:---------------------:|:---------------------:|:-----------------------------:|
|   keep-alive          |       keep-alive      |       server          |  HTTP 1.1默认值               |
|   keep-alive          |       close           |       client          |                               |
|   close               |       keep-alive      |       server          |                               |
|   close               |       close           |       server          |                               |
|   close               |       --              |       server          |                               |
|   --                  |       --              |       server          |  HTTP 1.1默认值，keep-alive   |
|   --                  |       close           |       client          |                               |

从上面表格可以看出，在HTTP 1.1版本中：
* 只要客户端发送的头部connection的值为close，则由服务端发起断开链接请求。
* 如果客户端发送的头部connection的值为keep-alive或者未设置，
    1. 如果服务端返回connection的值为keep-alive或未设置，则由服务器等待超时后发起断开链接请求。
    2. 如果服务端返回connection的值为close，则由客户端发起断开链接请求

即：首先接收到头部connection的值为close的一方发起断开链接请求。