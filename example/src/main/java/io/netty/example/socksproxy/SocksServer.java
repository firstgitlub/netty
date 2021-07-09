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
