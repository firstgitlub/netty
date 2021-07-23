/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.example.socksproxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public final class SocksServer {

    static final int PORT = Integer.parseInt(System.getProperty("port", "8088"));

    public static void main(String[] args) throws Exception {

        //用于处理接受客户端新连接 并分配新的channel给工作组的线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        //真正处理和客户端socket进行通信的线程组
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            // netty服务器的创建, 辅助工具类，用于服务器通道的一系列配置
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)    //绑定两个线程组
                //这里传的channel 就是下面bind方法里的 通过channelFactory中设置好的构造器，然后通过newChannel方法
                //返回的NioServerSocketChannel 类型的实例对象
             .channel(NioServerSocketChannel.class)   //指定NIO的模式
                //接受客户端连接的线程组处理事件时候 被调用
             .handler(new LoggingHandler(LogLevel.INFO))
                //设置一些Tcp的连接参数
             .option(ChannelOption.SO_BACKLOG,1024)

             // 子处理器，在workerGroup 线程组中的线程处理 channel事件的时候会去 调用
             .childHandler(new SocksServerInitializer());

            // 启动server，并且设置8088为启动的端口号，同时同步等待结果
            ChannelFuture channelFuture = b.bind(PORT).sync();

            // 监听关闭的channel，设置为同步方式，同步等到关闭结果
            channelFuture.channel().closeFuture().sync();
        } finally {

            //退出线程组
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
/**
 *
 * selector其实它底层包装的是native api,再底层实现是jvm虚拟机使用系统调用systemCall kernel去实现的。
 * 操作系统kernel提供的select函数原理：每次调用kernel select函数，都会涉及到 用户态与内核态的切换，还需要传递需要检查的socket集合，其实就是需要检查的fd(文件描述符id)，因为程序都是运行在linux或unix操作系统上，这种操作系统上，一切皆文件，socket也不例外，这里传递的fd其实就是文件系统中对应socket生成的文件描述符;操作系统select函数被调用后，首先按照fd集合去检查内存中的socket套接字状态，这个复杂度是O(N)，然后检查一遍后，如果有就绪状态的socket,那么直接返回，不会阻塞当前线程，否则，就说明当前指定fd集合对应的socket没有就绪状态，然后需要阻塞当前调用线程了，直到某个socket有数据之后，才唤醒线程。
 * 四.select函数去监听socket的数量限制？
 * ●它默认最大可以监听1024个socket，实际要小于1024个。
 * 原因在于：
 * 第一个，因为fd_set(select函数的参数之一)这个结构是一个bitmap位图结构，这个位图结构就是一个长二进制数，这个bitmap默认长度是1024个bit，想要修改这个长度的话，非常麻烦，还需要重新编译操作系统内核，一般人是搞不定的。
 * 第二个，也处于性能考虑，因为select函数它检查到就绪状态的socket后，做了两件事：第一是跑到就绪状态的socket对应的filedescriptor(fd文件)中设置一个标记，标记一个mask，表示当前fd对应的socket就绪了；第二返回select函数，对应的也就是唤醒java线程，站到java层面会受到一个int结果值，表示有几个socket处于就绪状态，具体是哪个socket就绪，java程序目前是不知道的，所以接下来又是1个O(n)系统调用,检查fd_set集合中每一个socket的就绪状态，其实就是检查文件系统中指定socket的文件描述符状态，过程中涉及到用户态和内核态的来回切换，就非常耗时了，如果bitmap再大，会严重影响性能了，还需要更多的系统调用，系统调用涉及到参数的数据拷贝，如果数据太庞大，不利于系统调用速度。
 * 五.select函数检查就绪状态的socket
 * 铺垫：操作系统调度和操作系统中断
 * 操作系统调度：CPU同一时刻，它只能运行一个进程(单核心)，操作系统最主要的任务是系统调度，就是有n个进程，然后让这n个进程在cpu上切换执行，未挂起的进程都在工作队列内，都有机会获取CPU执行权；挂起的进程，就会从这个工作队列内移除出去，反映到java层面就是线程阻塞了，linux系统线程其实就是轻量级进程。
 * 操作系统中断：比如说，咱们用键盘打字，如果CPU正在执行其他程序一直不释放，那咱们打字也没法打吗？事实不是这样的，因为就是有了系统中断的存在，按一个键之后，会给主板发送一个电流信号，主板感知到以后，就会触发cpu中断；所谓中断其实就是让CPU正在执行的进程先保留程序上下文，然后避让cpu，给中断程序让道。中断程序就会拿到cpu执行权限，进行相应代码执行，比如说键盘的中断程序，它就会执行输出逻辑。
 * ●①select函数第一遍轮询，它没有发现就绪状态的socket，它就会把当前的进程，保留到需要检查的socket的等待队列中，先补充一下，socket结构有三块核心区域分别是：读缓存、写缓存和等待缓存队列；select函数把当前进程保留到每个需要检查的socket 等待队列之后，就会把当前进程从工作队列中移除了，移除之后，就是挂起当前进程，此时select函数就不会再运行了。(放到等待队列→工作队列移除→挂起进程)
 * ②假设我们客户端往服务端发送了数据，数据通过网线到网卡，再到DMA硬件的这种方式将数据写到内存里，整个过程CPU是不参与的，当数据完成传输以后，它就会触发网络数据传输完毕的中断程序了，这个中断程序会把cpu正在执行的进程给顶掉，然后cpu就会执行中断程序的逻辑了。
 * ③中断程序的逻辑是：根据发送到内存中的数据包，来分析数据包是哪个socket的数据，tcp/ip协议，它又保证传输时根据端口号就能找到它对应的socket实例，找到socket实例后，就把数据导入到socket的读缓冲区里边，导入完成后，它就开始检查socket的等待队列是不是有等待者，如果有就把等待者移动到工作队列中，中断程序到这一步就执行完了；咱们的进程又回归到工作队列了，又有机会获取到cpu时间片了。(数据包→找socket实例→导入socket读缓冲区→检查等待队列等待者→移动到工作队列)
 * ④当前进程执行select函数再次检查就发现这个就绪的socket了，就会给就绪的socket的fd文件描述符打标记，然后select函数就执行完成了，返回到java层面，就涉及到内核态与用户态的转换了；之后，就是轮询检查每个socket的fd是否被打标记，然后处理被打了标记的socket就可以了。
 * 六.poll与select的区别，重点说下epoll
 * ●最大的区别就是传参不一样，select使用的bitmap，表示要检查的socket集合，poll使用的是数组结构，表示需要检查的socket集合；主要是 为了解决select bitmap长度是1024的问题，poll使用数组就没有这个限制了，可以让监听超过1024个socket限制，其他基本没什么区别。
 * ●epoll的产生背景：
 * 主要解决select函数和poll函数的缺陷：
 * ①数据拷贝：selec和poll函数，每次调用都需要我们提供给它所有需要监听的socket文件描述符集合，并且程序主线程是死循环调用select /poll函数的，涉及到用户空间数据到内核拷贝的过程，比较耗费性能；并且需要监听的socket集合，数据变化非常小，可能它每次就1~2个socket_fd需要更改，而selec和poll只是一个很单纯的函数，在kernel层面不会保留任何数据信息，所有每次调用都进行数据拷贝，这是一个缺陷。
 * ②无法指定具体就绪socket：selec和poll函数它的返回值是个int整形值，只能代表有几个socket就绪或者有错误了，没办法表示具体是哪个socket就绪了，导致程序被唤醒以后，它还需要新的一轮系统调用去检查哪个socket是就绪状态的，然后再进行socket数据逻辑，然而系统调用涉及到用户态和内核态的来回切换，这个缺陷就更严重了。
 * ●epoll函数的设计：
 * 主要解决两个问题，一个是函数调用参数拷贝的问题，另一个是系统调用返回后不知道哪些socket就绪的问题；epoll函数在内核空间内，它有一个对应的数据结构eventpoll对象去存储一些数据，通过一个系统函数epoll_create()去创建，创建完成后，系统函数返回一个eventpoll对象的id，是epfd文件号，相当于在内核开辟了一小块儿空间，也知道这块儿空间的位置。
 * ●eventpoll数据结构：
 * 主要有两块重要的区域，一块儿是存放需要监听的socket_fd描述符列表，另一块儿就是就绪列表，存放就绪状态的socket信息；另外还系统两个函数，一个是epoll_ctl函数，另一个是epoll_wait函数。
 * ◆epoll_ctl函数：根据eventpoll_id去增删改内核空间上的eventpoll对象的检查列表，即关注的socket信息，去增加或修改需要检查的socket文件描述符。
 * ◆epoll_wait函数：主要的参数是eventpoll_id，表示此次系统调用需要监听的socket_fd集合，是eventpoll中已经指定好的那些socket信息；epoll_wait函数默认情况下会阻塞调用线程，直到eventpoll中关联的某个或某些个socket就绪以后，epoll_wait函数才会返回。
 * ●维护就绪列表
 * ◆socket对象有三块区域:读缓冲区，写缓冲区，还有等待队列，select函数调用时会把当前调用进程从工作队列里拿出来，然后把进程引用追加到当前进程关注的每一个socket对象的等待队列中，当socket连接的客户端发送完数据之后，数据还是通过硬件DMA方式把数据写入到内存，然后响应的硬件就会向cpu发送中断信号，cpu就会让占用的进程让出位置，然后去执行网络数据就绪的中断程序，这个中断程序会把内存中的网络数据写入到对应的socket读缓冲区里，把socket等待队列中的进程全部移动到工作队列内，再然后select函数返回了，这个是select函数调用的过程。
 * ◆epoll工作流程和select非常类似，当我们系统调用系统函数epoll_ctl时，比如说需要添加一个需要关注的socket，其实内核程序它会把当前eventpoll对象追加到socket等待队列里，当socket对应的客户端发送完数据，还是通过网线进入到服务器，再通过DMA硬件把数据直接写入到内存，再触发中断程序，cpu将当前进程让出位置，去执行中断程序，中断程序将网络数据转移到对应的socket读写缓冲区里，再去检查socket的等待队列，发现socket等待队列内等待的不是进程，而是一个eventpoll对象引用，此时根据eventpoll引用，将当前socket的引用追加到eventpoll的就绪链表的末尾。
 * ◆eventpoll还有一块空间是eventpoll等待队列，保存的就是调用epoll_wait函数的进程；当中断程序把socket的引用追加到就绪列表以后，就继续检查eventpoll对象的等待队列，如果有进程，就会把进程转移到工作队列内，转移完成后，进程就会获取到cpu执行时间片了，然后调用epoll_wait函数，把进程返回到java层面的事儿了，也就是eventpol对象等待队列里边，它有进程，进程就是调用epoll_wait函数进去的进程，然后再把这个进程，从eventpoll等待队列迁移到工作队列里边。
 * ●epoll_wait函数的返回值是int类型，返回0表示没有就绪的socket，大于0表示有几个就绪的socket，-1表示异常，也没有表示出来哪个socket是就绪的，获取就绪的socket是怎么实现呢？
 * epoll_wait函数调用的时候会传入一个epoll_event事件数组指针，epoll_wait函数正常返回前就会把就绪的socket事件信息拷贝到这个数据组指针里，也就是指针表示的数组里，返回到上层程序，就可以通过这个数组拿到就绪列表了。
 * ●epoll_wait函数阻塞与非阻塞
 * epoll_wait函数默认是阻塞的，可以设置成非阻塞，有个参数表示阻塞时间的长度，如果设置为0，表示epoll_wait函数是非阻塞的，即每次调用都会检查就绪列表。
 * ●eventpoll中需要存放需要监视的socket集合信息的数据结构
 * 采用红黑树结构，因为socket集合信息会经常有增删改查需求，红黑树是最合适了，它能保持一种相对稳定的查找效率，复杂度是O(log(n))。
 *
 */
