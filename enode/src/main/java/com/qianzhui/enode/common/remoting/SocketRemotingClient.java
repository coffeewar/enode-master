package com.qianzhui.enode.common.remoting;

import com.qianzhui.enode.common.container.ObjectContainer;
import com.qianzhui.enode.common.logging.ILogger;
import com.qianzhui.enode.common.logging.ILoggerFactory;
import com.qianzhui.enode.common.remoting.exceptions.RemotingRequestException;
import com.qianzhui.enode.common.remoting.exceptions.RemotingServerUnAvailableException;
import com.qianzhui.enode.common.remoting.exceptions.RemotingTimeoutException;
import com.qianzhui.enode.common.remoting.exceptions.ResponseFutureAddFailedException;
import com.qianzhui.enode.common.scheduling.IScheduleService;
import com.qianzhui.enode.common.socketing.ClientSocket;
import com.qianzhui.enode.common.socketing.IConnectionEventListener;
import com.qianzhui.enode.common.socketing.NettyClientConfig;
import com.qianzhui.enode.common.socketing.buffermanagement.IBufferPool;
import com.qianzhui.enode.common.utilities.BitConverter;
import com.qianzhui.enode.common.utilities.Ensure;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by junbo_xu on 2016/3/11.
 */
public class SocketRemotingClient {
    private static InetAddress localAddress;

    static {
        try {
            localAddress = InetAddress.getLocalHost();
        } catch (Exception e) {
            //ignore
        }
    }

    private byte[] timeoutMessage = BitConverter.getBytes("Remoting request timeout.");
    private Map<Integer, IResponseHandler> _responseHandlerDict;
    private List<IConnectionEventListener> _connectionEventListeners;
    private ConcurrentMap<Long, ResponseFuture> _responseFutureDict;
    private IScheduleService _scheduleService;
    private ILogger _logger;
    private NettyClientConfig _nettyClientConfig;

    private SocketAddress _serverEndPoint;
    private SocketAddress _localEndPoint;
    private ClientSocket _clientSocket;
    private AtomicBoolean _reconnecting = new AtomicBoolean(false);
    private boolean _shutteddown = false;

    public SocketRemotingClient() {
        this(new InetSocketAddress(localAddress, 5000), null, null);
    }

    public SocketRemotingClient(SocketAddress serverEndPoint) {
        this(serverEndPoint, null, null);
    }

    public SocketRemotingClient(SocketAddress serverEndPoint, SocketAddress localEndPoint) {
        this(serverEndPoint, null, localEndPoint);
    }

    public SocketRemotingClient(SocketAddress serverEndPoint, NettyClientConfig nettyClientConfig, SocketAddress localEndPoint) {
        Ensure.notNull(serverEndPoint, "serverEndPoint");

        _serverEndPoint = serverEndPoint;
        _localEndPoint = localEndPoint;
        _nettyClientConfig = nettyClientConfig == null ? new NettyClientConfig() : nettyClientConfig;
        _clientSocket = new ClientSocket(_serverEndPoint, localEndPoint, _nettyClientConfig, this::handleReplyMessage);
        _responseFutureDict = new ConcurrentHashMap<>();
//        _replyMessageQueue = new BlockingCollection<byte[]>(new ConcurrentQueue<byte[]>());
        _responseHandlerDict = new HashMap<>();
        _connectionEventListeners = new ArrayList<>();
        _scheduleService = ObjectContainer.resolve(IScheduleService.class);
        _logger = ObjectContainer.resolve(ILoggerFactory.class).create(SocketRemotingClient.class);

        registerConnectionEventListener(new ConnectionEventListener(this));
    }

    public SocketRemotingClient registerResponseHandler(int requestCode, IResponseHandler responseHandler) {
        _responseHandlerDict.put(requestCode, responseHandler);
        return this;
    }

    public SocketRemotingClient registerConnectionEventListener(IConnectionEventListener listener) {
        _connectionEventListeners.add(listener);
        _clientSocket.registerConnectionEventListener(listener);
        return this;
    }

    public SocketRemotingClient start() {
        startClientSocket();
        startScanTimeoutRequestTask();
        _shutteddown = false;
        return this;
    }

    public void shutdown() {
        _shutteddown = true;
        stopReconnectServerTask();
        stopScanTimeoutRequestTask();
        shutdownClientSocket();
    }

    public RemotingResponse invokeSync(RemotingRequest request, int timeoutMillis) {
        CompletableFuture<RemotingResponse> task = invokeAsync(request, timeoutMillis);

        RemotingResponse response = null;
        try {
            response = task.get(timeoutMillis + 1000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RemotingRequestException(_serverEndPoint, request, "Remoting response is null due to unkown exception.");
        }

        if (response == null) {
            if (!task.isDone()) {
                throw new RemotingTimeoutException(_serverEndPoint, request, timeoutMillis);
            } else if (task.isCompletedExceptionally()) {
                throw new RemotingRequestException(_serverEndPoint, request, "Remoting respoinse is null.");
            } else {
                throw new RemotingRequestException(_serverEndPoint, request, "Remoting response is null due to unkown exception.");
            }
        }
        return response;
    }

    public CompletableFuture<RemotingResponse> invokeAsync(RemotingRequest request, int timeoutMillis) throws ResponseFutureAddFailedException, RemotingServerUnAvailableException {
        ensureClientStatus();

        request.setType(RemotingRequestType.Async);
//        var taskCompletionSource = new TaskCompletionSource<RemotingResponse>();
        CompletableFuture<RemotingResponse> taskCompletionSource = new CompletableFuture<>();
        ResponseFuture responseFuture = new ResponseFuture(request, timeoutMillis, taskCompletionSource);


        if (_responseFutureDict.containsKey(request.getSequence())) {
            throw new ResponseFutureAddFailedException(request.getSequence());
        }

        _responseFutureDict.put(request.getSequence(), responseFuture);

        _clientSocket.sendMessage(RemotingUtil.buildRequestMessage(request));

        return taskCompletionSource;
    }

    public void invokeWithCallback(RemotingRequest request) {
        ensureClientStatus();

        request.setType(RemotingRequestType.Callback);
        _clientSocket.sendMessage(RemotingUtil.buildRequestMessage(request));
    }

    public void invokeOneway(RemotingRequest request) {
        ensureClientStatus();

        request.setType(RemotingRequestType.Oneway);
        _clientSocket.sendMessage(RemotingUtil.buildRequestMessage(request));
    }


    private void handleReplyMessage(ChannelHandlerContext channelHandlerContext, byte[] message) {
        if (message == null) return;

        RemotingResponse remotingResponse = RemotingUtil.parseResponse(message);

        if (remotingResponse.getType() == RemotingRequestType.Callback) {
            IResponseHandler responseHandler = _responseHandlerDict.get(remotingResponse.getRequestCode());

            if (responseHandler != null) {
                responseHandler.handleResponse(remotingResponse);
            } else {
                _logger.error("No response handler found for remoting response:%s", remotingResponse);
            }
        } else if (remotingResponse.getType() == RemotingRequestType.Async) {
            ResponseFuture responseFuture = _responseFutureDict.get(remotingResponse.getSequence());
            if (responseFuture != null) {
                if (responseFuture.setResponse(remotingResponse)) {
                    if (_logger.isDebugEnabled()) {
                        _logger.debug("Remoting response back, request code:%d, requect sequence:%d, time spent:%d", responseFuture.getRequest().getCode(), responseFuture.getRequest().getSequence(), System.currentTimeMillis() - responseFuture.getBeginTimestamp());
                    }
                } else {
                    _logger.error("Set remoting response failed, response:" + remotingResponse);
                }
            }
        }
    }

    private void scanTimeoutRequest() {
        List<Long> timeoutKeyList = new ArrayList<>();
        _responseFutureDict.forEach((k, v) -> {
            if (v.isTimeout()) {
                timeoutKeyList.add(k);
            }
        });

        timeoutKeyList.forEach(key -> {
            ResponseFuture responseFuture = _responseFutureDict.get(key);
            if (responseFuture != null) {
                _responseFutureDict.remove(key);
                responseFuture.setResponse(new RemotingResponse(responseFuture.getRequest().getCode(), (short) 0, responseFuture.getRequest().getType(), timeoutMessage, responseFuture.getRequest().getSequence()));
                if (_logger.isDebugEnabled()) {
                    _logger.debug("Removed timeout request:{0}", responseFuture.getRequest());
                }
            }
        });
    }

    private void reconnectServer() {
        _logger.info("Try to reconnect to server, address: %s", _serverEndPoint);

        if (_clientSocket.isConnected()) return;
        if (!enterReconnecting()) return;

        try {
            _clientSocket.shutdown();
            _clientSocket = new ClientSocket(_serverEndPoint, _localEndPoint, _nettyClientConfig, this::handleReplyMessage);
            _connectionEventListeners.forEach(listener -> _clientSocket.registerConnectionEventListener(listener));
            _clientSocket.start(5000);
        } catch (Exception ex) {
            _logger.error("Reconnect to server error.", ex);
            exitReconnecting();
        }
    }

    private void startClientSocket() {
        _clientSocket.start(5000);
    }


    private void shutdownClientSocket() {
        _clientSocket.shutdown();
    }


    private void startScanTimeoutRequestTask() {
        _scheduleService.startTask(String.format("%s.ScanTimeoutRequest", SocketRemotingClient.class.getName()), this::scanTimeoutRequest, 1000, 1000);
    }

    private void stopScanTimeoutRequestTask() {
        _scheduleService.stopTask(String.format("%s.ScanTimeoutRequest", this.getClass().getName()));
    }

    private void startReconnectServerTask() {
        _scheduleService.startTask(String.format("%s.ReconnectServer", this.getClass().getName()), this::reconnectServer, 1000, 1000);
    }


    private void stopReconnectServerTask() {
        _scheduleService.stopTask(String.format("%s.ReconnectServer", this.getClass().getName()));
    }

    private void ensureClientStatus() throws RemotingServerUnAvailableException {
        if (!_clientSocket.isConnected()) {
            throw new RemotingServerUnAvailableException(_serverEndPoint);
        }
    }

    private boolean enterReconnecting() {
        return _reconnecting.compareAndSet(false, true);
    }


    private void exitReconnecting() {
        _reconnecting.set(false);
    }

    private void setLocalEndPoint(SocketAddress localEndPoint) {
        _localEndPoint = localEndPoint;
    }

    public boolean isConnected() {
        return _clientSocket.isConnected();
    }

    public SocketAddress getLocalEndPoint() {
        return _localEndPoint;
    }

    class ConnectionEventListener implements IConnectionEventListener {
        private final SocketRemotingClient _remotingClient;

        public ConnectionEventListener(SocketRemotingClient remotingClient) {
            _remotingClient = remotingClient;
        }

        public void onConnectionAccepted(Channel socketChannel) {
        }

        public void onConnectionEstablished(Channel channel) {
            _remotingClient.stopReconnectServerTask();
            _remotingClient.exitReconnecting();

            _remotingClient.setLocalEndPoint(channel.localAddress());
        }

        public void onConnectionFailed(Throwable cause) {
            if (_remotingClient._shutteddown) return;

            _remotingClient.exitReconnecting();
            _remotingClient.startReconnectServerTask();
        }

        public void onConnectionClosed(Channel socketChannel) {
            if (_remotingClient._shutteddown) return;

            _remotingClient.exitReconnecting();
            _remotingClient.startReconnectServerTask();
        }
    }
}
