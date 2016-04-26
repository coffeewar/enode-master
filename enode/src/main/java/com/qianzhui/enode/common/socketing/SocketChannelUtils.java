package com.qianzhui.enode.common.socketing;

import com.qianzhui.enode.common.utilities.Helper;

import java.io.IOException;
import java.net.*;
import java.nio.channels.ServerSocketChannel;

/**
 * Created by junbo_xu on 2016/3/4.
 */
public class SocketChannelUtils {
    /*public static IPAddress GetLocalIPV4()
    {
        return Dns.GetHostEntry(Dns.GetHostName()).AddressList.First(x => x.AddressFamily == AddressFamily.InterNetwork);
    }*/
    public static ServerSocketChannel createServerSocketChannel(int receiveBufferSize) throws IOException {
        ServerSocketChannel socketChannel = ServerSocketChannel.open();

        java.net.ServerSocket socket = socketChannel.socket();
        socket.setReceiveBufferSize(receiveBufferSize);

        socketChannel.configureBlocking(false);
        socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        socketChannel.setOption(StandardSocketOptions.SO_LINGER, 0);


        return socketChannel;
    }

    public static void shutdownSocket(Socket socket) {
        if (socket == null) return;


        Helper.eatException(() -> {
            socket.shutdownInput();
            socket.shutdownOutput();
        });
        Helper.eatException(() -> socket.close());
    }

    public static void closeSocket(Socket socket) {
        if (socket == null) return;

        Helper.eatException(() -> socket.close());
    }
}
