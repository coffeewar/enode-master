package com.qianzhui.enode.common.socketing;

import com.qianzhui.enode.common.function.Action3;
import com.qianzhui.enode.common.logging.ENodeLogger;
import com.qianzhui.enode.common.utilities.Ensure;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Created by junbo_xu on 2016/3/3.
 */
public class ServerSocket {
    private static final Logger logger = ENodeLogger.getLog();

    private final ServerBootstrap serverBootstrap;
    // 服务器线程组 用于网络事件的处理 一个用于服务器接收客户端的连接
    // 另一个线程组用于处理SocketChannel的网络读写
    private final EventLoopGroup eventLoopGroupBoss;
    private final EventLoopGroup eventLoopGroupWorker;
    private DefaultEventExecutorGroup defaultEventExecutorGroup;

    private NettyServerConfig nettyServerConfig;
    // 本地server绑定的端口
    private SocketAddress listeningEndPoint;
    private int port;
    private List<IConnectionEventListener> connectionEventListeners;
    private Action3<Channel, byte[], Consumer<byte[]>> messageArrivedHandler;

    public ServerSocket(NettyServerConfig nettyServerConfig, Action3<Channel, byte[], Consumer<byte[]>> messageArrivedHandler) {
        Ensure.notNull(nettyServerConfig, "nettyServerConfig");
        Ensure.notNull(messageArrivedHandler, "messageArrivedHandler");

        this.serverBootstrap = new ServerBootstrap();

        this.eventLoopGroupBoss = new NioEventLoopGroup(1, new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);


            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r,
                        String.format("NettyBossSelector_%d", this.threadIndex.incrementAndGet()));
            }
        });

        this.eventLoopGroupWorker =
                new NioEventLoopGroup(nettyServerConfig.getServerSelectorThreads(), new ThreadFactory() {
                    private AtomicInteger threadIndex = new AtomicInteger(0);
                    private int threadTotal = nettyServerConfig.getServerSelectorThreads();


                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, String.format("NettyServerSelector_%d_%d", threadTotal,
                                this.threadIndex.incrementAndGet()));
                    }
                }
                );

        this.nettyServerConfig = nettyServerConfig;
        this.connectionEventListeners = new ArrayList<>();
        this.messageArrivedHandler = messageArrivedHandler;
    }

    public void registerConnectionEventListener(IConnectionEventListener listener) {
        connectionEventListeners.add(listener);
    }

    public void start() {

        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(//
                nettyServerConfig.getServerWorkerThreads(), //
                new ThreadFactory() {

                    private AtomicInteger threadIndex = new AtomicInteger(0);


                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "NettyServerWorkerThread_" + this.threadIndex.incrementAndGet());
                    }
                });

        // NIO服务器端的辅助启动类 降低服务器开发难度
        ServerBootstrap childHandler = serverBootstrap.group(eventLoopGroupBoss, eventLoopGroupWorker)
                .channel(NioServerSocketChannel.class) // 类似NIO中serverSocketChannel
                .option(ChannelOption.SO_BACKLOG, 5000) // 配置TCP参数，允许的最多积压连接请求
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_SNDBUF, nettyServerConfig.getServerSocketSndBufSize())
                .option(ChannelOption.SO_RCVBUF, nettyServerConfig.getServerSocketRcvBufSize())
                .localAddress(new InetSocketAddress(this.nettyServerConfig.getListenPort()))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                //
                                defaultEventExecutorGroup, //
                                new LengthFieldPrepender(4), //
                                new LengthFieldBasedFrameDecoder(1024004, 0, 4, 0, 4),
                                new NettyServerHandler()
                                /*new IdleStateHandler(0, 0, nettyServerConfig
                                        .getServerChannelMaxIdleTimeSeconds())*/);
                    }
                });

        if (nettyServerConfig.isServerPooledByteBufAllocatorEnable()) {
            // 这个选项有可能会占用大量堆外内存，暂时不使用。
            childHandler.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        }

        try {
            // 服务器启动后 绑定监听端口 同步等待成功 主要用于异步操作的通知回调 回调处理用的ChildChannelHandler
            ChannelFuture future = serverBootstrap.bind().sync();

            this.listeningEndPoint = future.channel().localAddress();
            this.port = ((InetSocketAddress) listeningEndPoint).getPort();

            logger.info("Server启动成功");
            // 等待服务端监听端口关闭
//            future.channel().closeFuture().sync();

        } catch (InterruptedException ex) {
            logger.error("this.serverBootstrap.bind().sync() InterruptedException", ex);
            throw new RuntimeException("this.serverBootstrap.bind().sync() InterruptedException", ex);
        }
    }

    public void shutdown() {
        try {
            eventLoopGroupBoss.shutdownGracefully();
            eventLoopGroupWorker.shutdownGracefully();

            if(defaultEventExecutorGroup != null) {
                defaultEventExecutorGroup.shutdownGracefully();
            }
            logger.info("Socket server shutdown, listening TCP endpoint: {}.", port);
        } catch (Exception e) {
            logger.error("NettyRemotingServer shutdown exception, ", e);
        }
    }

    private void onSocketAccepted(Channel channel) {
        CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Socket accepted, remote endpoint:{}", channel.remoteAddress());

                connectionEventListeners.forEach(listener -> {
                    try {
                        listener.onConnectionAccepted(channel);
                    } catch (Exception ex) {
                        logger.error(String.format("Notify connection accepted failed, listener type:%s", listener.getClass().getName()), ex);
                    }
                });
            } catch (Exception ex) {
                logger.info("Accept socket client has unknown exception.", ex);
            }
            return null;
        });
    }

    private void onMessageArrived(Channel channel, byte[] message) {
        try {
            messageArrivedHandler.apply(channel, message, (reply) -> channel.writeAndFlush(reply));
        } catch (Exception ex) {
            logger.error("Handle message error.", ex);
        }
    }

    private void onConnectionClosed(Channel channel) {
        connectionEventListeners.forEach(listener -> {
            try {
                listener.onConnectionClosed(channel);
            } catch (Exception ex) {
                logger.error(String.format("Notify connection closed failed, listener type:{0}", listener.getClass().getName()), ex);
            }
        });
    }

    public SocketAddress getListeningEndPoint() {
        return listeningEndPoint;
    }

    public int getPort() {
        return port;
    }

    class NettyServerHandler extends SimpleChannelInboundHandler {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            onSocketAccepted(ctx.channel());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            onConnectionClosed(ctx.channel());
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf buf = (ByteBuf) msg;
            byte[] req = new byte[buf.readableBytes()];
            buf.readBytes(req);
            onMessageArrived(ctx.channel(), req);
            /*ByteBuf buf = (ByteBuf) msg;
            byte[] req = new byte[buf.readableBytes()];
            buf.readBytes(req);
            String body = new String(req, "UTF-8");
            System.out.println("the time server receive order:" + body);
            String curentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
            ByteBuf resp = Unpooled.copiedBuffer(curentTime.getBytes());
            ctx.write(resp);
            System.out.println("服务器做出了响应");*/
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
                throws Exception {
            ctx.close();
            logger.error("服务器异常退出" + cause.getMessage());
        }
    }
}
