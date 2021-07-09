package io.netty.example.socksproxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.example.echo.EchoClientHandler;

import java.net.InetSocketAddress;

public class NioSocketClient {
    public static void main(String[] args) throws Exception{

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group) // 注册线程池
                .channel(NioSocketChannel.class) // 使用NioSocketChannel来作为连接用的channel类
                .remoteAddress(new InetSocketAddress("localhost", 8088)) // 绑定连接端口和host信息
                .handler(new ChannelInitializer() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        //这里放入自定义助手类
                        ch.pipeline().addLast(new EchoClientHandler());
                    } // 绑定连接初始化器
//                    @Override
//                    protected void initChannel(SocketChannel ch) throws Exception {
//                        ch.pipeline().addLast(new EchoClientHandler());
//                    }
                });

            ChannelFuture cf = b.connect().sync(); // 异步连接服务器
            cf.channel().closeFuture().sync(); // 异步等待关闭连接channel

        } finally {
            group.shutdownGracefully().sync(); // 释放线程池资源
        }
    }

}

