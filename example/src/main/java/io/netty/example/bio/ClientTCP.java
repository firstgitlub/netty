package io.netty.example.bio;

import java.io.OutputStream;
import java.net.Socket;

public class ClientTCP {

    public static void main(String[] args) {
//        while (true){
//
//        }
        try{
            Socket socket=new Socket("localhost", 8888);
            OutputStream os=socket.getOutputStream();
            System.out.println("客户端启动成功，开始发送数据");
            //发送消息给服务端
            os.write("hello".getBytes());

        }catch(Exception e) {
            e.printStackTrace();
        }

    }
}
