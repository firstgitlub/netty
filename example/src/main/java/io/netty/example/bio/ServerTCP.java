package io.netty.example.bio;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerTCP {

    private static final int PORT = 8888;

    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("服务器启动,等待连接。。。。。");

        while (true){
            //等待客户端连接
            Socket socket=serverSocket.accept();
            //从套接字中获取输入流
            InputStream is=socket.getInputStream();
            //读取数据
            byte[] b=new byte[1024];
            int len=is.read(b);
            System.out.println("客户端说："+new String(b,0,len));
        }

    }
}
