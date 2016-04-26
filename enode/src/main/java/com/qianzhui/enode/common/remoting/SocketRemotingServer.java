package com.qianzhui.enode.common.remoting;

import com.qianzhui.enode.common.container.ObjectContainer;
import com.qianzhui.enode.common.function.Action1;
import com.qianzhui.enode.common.logging.ILogger;
import com.qianzhui.enode.common.logging.ILoggerFactory;
import com.qianzhui.enode.common.socketing.IConnectionEventListener;
import com.qianzhui.enode.common.socketing.NettyServerConfig;
import com.qianzhui.enode.common.socketing.ServerSocket;
import com.qianzhui.enode.common.utilities.BitConverter;
import com.qianzhui.enode.infrastructure.WrappedRuntimeException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;


/**
 * Created by junbo_xu on 2016/3/6.
 */
public class SocketRemotingServer {
    private ServerSocket _serverSocket;
    private Map<Integer, IRequestHandler> _requestHandlerDict;
    private ILogger _logger;
    private NettyServerConfig _nettyServerConfig;
    private boolean _isShuttingdown = false;

    public SocketRemotingServer() {
        this("Server", null);
    }

    public SocketRemotingServer(String name, NettyServerConfig nettyServerConfig) {
        _nettyServerConfig = (nettyServerConfig == null ? new NettyServerConfig() : nettyServerConfig);
        _serverSocket = new ServerSocket(_nettyServerConfig, this::handleRemotingRequest);
        _requestHandlerDict = new HashMap<>();
        _logger = ObjectContainer.resolve(ILoggerFactory.class).create(name == null ? SocketRemotingServer.class.getName() : name);
    }

    public SocketRemotingServer registerConnectionEventListener(IConnectionEventListener listener) {
        _serverSocket.registerConnectionEventListener(listener);
        return this;
    }

    public SocketRemotingServer start() {
        _isShuttingdown = false;
        _serverSocket.start();
        return this;
    }

    public SocketRemotingServer shutdown() {
        _isShuttingdown = true;
        _serverSocket.shutdown();
        return this;
    }

    public ServerSocket getServerSocket() {
        return _serverSocket;
    }

    public SocketRemotingServer registerRequestHandler(int requestCode, IRequestHandler requestHandler) {
        _requestHandlerDict.put(requestCode, requestHandler);
        return this;
    }

    private void handleRemotingRequest(Channel channel, byte[] message, Consumer<byte[]> sendReplyAction) {
        if (_isShuttingdown) return;

        RemotingRequest remotingRequest = RemotingUtil.parseRequest(message);
        SocketRequestHandlerContext requestHandlerContext = new SocketRequestHandlerContext(channel, sendReplyAction);

        IRequestHandler requestHandler = _requestHandlerDict.get((int) remotingRequest.getCode());

        if (requestHandler == null) {
            String errorMessage = String.format("No request handler found for remoting request:%s", remotingRequest);
            _logger.error(errorMessage);
            if (remotingRequest.getType() != RemotingRequestType.Oneway) {
                try {
                    requestHandlerContext.sendRemotingResponse.accept(new RemotingResponse(remotingRequest.getCode(), (short) -1, remotingRequest.getType(), BitConverter.getBytes(errorMessage), remotingRequest.getSequence()));
                } catch (Exception e) {
                    throw new WrappedRuntimeException(e);
                }
            }
            return;
        }

        try {
            RemotingResponse remotingResponse = requestHandler.handleRequest(requestHandlerContext, remotingRequest);
            if (remotingRequest.getType() != RemotingRequestType.Oneway && remotingResponse != null) {
                requestHandlerContext.sendRemotingResponse.accept(remotingResponse);
            }
        } catch (Exception ex) {
            String errorMessage = String.format("Unknown exception raised when handling remoting request:%s.", remotingRequest);
            _logger.error(errorMessage, ex);
            if (remotingRequest.getType() != RemotingRequestType.Oneway) {
                requestHandlerContext.sendRemotingResponse.accept(new RemotingResponse(remotingRequest.getCode(), (short) -1, remotingRequest.getType(), BitConverter.getBytes(ex.getMessage()), remotingRequest.getSequence()));
            }
        }
    }
}
