package com.qianzhui.enode.common.socketing;

import com.qianzhui.enode.common.function.Action2;
import com.qianzhui.enode.common.logging.ENodeLogger;
import com.qianzhui.enode.common.threading.ManualResetEvent;
import com.qianzhui.enode.common.utilities.Ensure;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.slf4j.Logger;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by junbo_xu on 2016/3/11.
 */
public class ClientSocket {

    private static final Logger logger = ENodeLogger.getLog();

    private final SocketAddress serverEndPoint;
    private SocketAddress localEndPoint;
    private final Bootstrap bootstrap;
    private final NettyClientConfig nettyClientConfig;
    private final Action2<ChannelHandlerContext, byte[]> messageArrivedHandler;
    private final EventLoopGroup eventLoopGroupWorker;
    private DefaultEventExecutorGroup defaultEventExecutorGroup;
    private List<IConnectionEventListener> connectionEventListeners;
    private ManualResetEvent waitConnectHandle;
    private Channel channel;
    private boolean connected;

    public ClientSocket(SocketAddress serverEndPoint, SocketAddress localEndPoint, NettyClientConfig nettyClientConfig, Action2<ChannelHandlerContext, byte[]> messageArrivedHandler) {
        Ensure.notNull(nettyClientConfig, "nettyClientConfig");
        Ensure.notNull(messageArrivedHandler, "messageArrivedHandler");
        Ensure.notNull(serverEndPoint, "serverEndPoint");

        this.serverEndPoint = serverEndPoint;
        this.localEndPoint = localEndPoint;
        this.bootstrap = new Bootstrap();
        this.connectionEventListeners = new ArrayList<>();

        this.nettyClientConfig = nettyClientConfig;
        this.messageArrivedHandler = messageArrivedHandler;
        this.waitConnectHandle = new ManualResetEvent(false);

        this.eventLoopGroupWorker =
                new NioEventLoopGroup(1, new ThreadFactory() {
                    private AtomicInteger threadIndex = new AtomicInteger(0);


                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, String.format("NettyClientSelector_%d",
                                this.threadIndex.incrementAndGet()));
                    }
                }
                );
    }

    public ClientSocket registerConnectionEventListener(IConnectionEventListener listener) {
        connectionEventListeners.add(listener);
        return this;
    }

    public boolean isConnected() {
        return connected;
    }

    public ClientSocket start(long waitMilliseconds) {
        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(//
                nettyClientConfig.getClientWorkerThreads(), //
                new ThreadFactory() {

                    private AtomicInteger threadIndex = new AtomicInteger(0);


                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "NettyClientWorkerThread_" + this.threadIndex.incrementAndGet());
                    }
                });

        Bootstrap handler = this.bootstrap.group(this.eventLoopGroupWorker).channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_SNDBUF, nettyClientConfig.getClientSocketSndBufSize())
                .option(ChannelOption.SO_RCVBUF, nettyClientConfig.getClientSocketRcvBufSize())
                .remoteAddress(serverEndPoint)
                .localAddress(localEndPoint)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(//
                                defaultEventExecutorGroup, //
                                new LengthFieldPrepender(4), //
                                new LengthFieldBasedFrameDecoder(1024004, 0, 4, 0, 4),
                                //new IdleStateHandler(0, 0, nettyClientConfig.getClientChannelMaxIdleTimeSeconds()),
                                new NettyClientHandler());

                        //TODO FlowControlIfNecessary TrafficShaping
                    }
                });

        ChannelFuture connectFuture = handler.connect().addListener(f -> {
            if (!f.isSuccess()) {
                onConnectionFailed(f.cause());
            }
        });

        waitConnectHandle.waitOne(waitMilliseconds < 0 ? 5000 : waitMilliseconds);

        return this;
    }

    public void sendMessage(byte[] message) {
        ByteBuf msgBuf = Unpooled.copiedBuffer(message);
        this.channel.writeAndFlush(msgBuf);
    }

    public ClientSocket shutdown() {
        try {
            eventLoopGroupWorker.shutdownGracefully();

            if(defaultEventExecutorGroup != null) {
                defaultEventExecutorGroup.shutdownGracefully();
            }
        } catch (Exception e) {
            logger.error("NettyRemotingClient shutdown exception, ", e);
        }
        return this;
    }

    private void onMessageArrived(ChannelHandlerContext channelHandlerContext, byte[] message) {
        try {
            messageArrivedHandler.apply(channelHandlerContext, message);
        } catch (Exception ex) {
            logger.error("Handle message error.", ex);
        }
    }

    private void onConnectionEstablished(ChannelHandlerContext channelHandlerContext) {
        connected = true;
        this.channel = channelHandlerContext.channel();
        this.localEndPoint = channelHandlerContext.channel().localAddress();
        connectionEventListeners.forEach(listener -> {
            try {
                listener.onConnectionEstablished(this.channel);
            } catch (Exception ex) {
                logger.error(String.format("Notify connection established failed, listener type:%s", listener.getClass().getName()), ex);
            }
        });

        waitConnectHandle.set();
    }

    private void onConnectionFailed(Throwable cause) {
        connected = false;
        connectionEventListeners.forEach(listener -> {
            try {
                listener.onConnectionFailed(cause);
            } catch (Exception ex) {
                logger.error(String.format("Notify connection failed has exception, listener type:%s", listener.getClass().getName()), ex);
            }
        });
    }

    private void onConnectionClosed(ChannelHandlerContext channelHandlerContext) {
        connected = false;
        connectionEventListeners.forEach(listener -> {
            try {
                listener.onConnectionClosed(channelHandlerContext.channel());
            } catch (Exception ex) {
                logger.error(String.format("Notify connection closed failed, listener type:%s", listener.getClass().getName()), ex);
            }
        });
    }

    class NettyClientHandler extends SimpleChannelInboundHandler {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            onConnectionEstablished(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            onConnectionClosed(ctx);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf buf = (ByteBuf) msg;

            byte[] req = new byte[buf.readableBytes()];
            buf.readBytes(req);

            onMessageArrived(ctx, req);
        }
    }
}
