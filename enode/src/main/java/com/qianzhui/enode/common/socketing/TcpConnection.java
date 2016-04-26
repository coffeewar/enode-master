//package com.qianzhui.enode.common.socketing;
//
//import com.qianzhui.enode.common.container.ObjectContainer;
//import com.qianzhui.enode.common.extensions.ArraySegment;
//import com.qianzhui.enode.common.function.Action2;
//import com.qianzhui.enode.common.logging.ILogger;
//import com.qianzhui.enode.common.logging.ILoggerFactory;
//import com.qianzhui.enode.common.socketing.buffermanagement.IBufferPool;
//import com.qianzhui.enode.common.socketing.framing.IMessageFramer;
//import com.qianzhui.enode.common.utilities.Ensure;
//
//import java.net.Socket;
//import java.net.SocketAddress;
//import java.nio.ByteBuffer;
//import java.util.List;
//import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.atomic.AtomicLong;
//
///**
// * Created by junbo_xu on 2016/3/5.
// */
//public class TcpConnection implements ITcpConnection {
//    private Socket _socket;
//    private SocketSetting _setting;
//    private SocketAddress _localEndPoint;
//    private SocketAddress _remotingEndPoint;
//    private SocketAsyncEventArgs _sendSocketArgs;
//    private SocketAsyncEventArgs _receiveSocketArgs;
//    private IBufferPool _receiveDataBufferPool;
//    private IMessageFramer _framer;
//    private ILogger _logger;
//    private ConcurrentLinkedQueue<List<ArraySegment<Byte>>> _sendingQueue = new ConcurrentLinkedQueue<List<ArraySegment<Byte>>>();
//    private ConcurrentLinkedQueue<ReceivedData> _receiveQueue = new ConcurrentLinkedQueue<ReceivedData>();
//    //    private MemoryStream _sendingStream = new MemoryStream();
//    private ByteBuffer _sendingStream = ByteBuffer.allocate(1024);
//    private Object _receivingLock = new Object();
//
//    private Action<ITcpConnection, SocketError> _connectionClosedHandler;
//    private Action2<ITcpConnection, Byte[]> _messageArrivedHandler;
//
//    private AtomicBoolean _sending = new AtomicBoolean(false);
//    private int _receiving;
//    private int _parsing;
//    private AtomicBoolean _closing = new AtomicBoolean(false);
//
//    private AtomicLong _pendingMessageCount;
//
//    @Override
//    public boolean isConnected() {
//        return false;
//    }
//
//    public Socket getScket() {
//        return _socket;
//    }
//
//    @Override
//    public SocketAddress getLocalEndPoint() {
//        return _localEndPoint;
//    }
//
//    @Override
//    public SocketAddress getRemotingEndPoint() {
//        return _remotingEndPoint;
//    }
//
//    public SocketSetting getSetting() {
//        return _setting;
//    }
//
//    public long getPendingMessageCount() {
//        return _pendingMessageCount.get();
//    }
//
//    public TcpConnection(Socket socket, SocketSetting setting, IBufferPool receiveDataBufferPool, Action2<ITcpConnection, Byte[]> messageArrivedHandler, Action<ITcpConnection, SocketError> connectionClosedHandler) {
//        Ensure.notNull(socket, "socket");
//        Ensure.notNull(setting, "setting");
//        Ensure.notNull(receiveDataBufferPool, "receiveDataBufferPool");
//        Ensure.notNull(messageArrivedHandler, "messageArrivedHandler");
//        Ensure.notNull(connectionClosedHandler, "connectionClosedHandler");
//
//        _socket = socket;
//        _setting = setting;
//        _receiveDataBufferPool = receiveDataBufferPool;
//        _localEndPoint = socket.getLocalSocketAddress();
//        _remotingEndPoint = socket.getRemoteSocketAddress();
//        _messageArrivedHandler = messageArrivedHandler;
//        _connectionClosedHandler = connectionClosedHandler;
//
//        _sendSocketArgs = new SocketAsyncEventArgs();
//        _sendSocketArgs.AcceptSocket = _socket;
//        _sendSocketArgs.Completed += OnSendAsyncCompleted;
//
//        _receiveSocketArgs = new SocketAsyncEventArgs();
//        _receiveSocketArgs.AcceptSocket = socket;
//        _receiveSocketArgs.Completed += OnReceiveAsyncCompleted;
//
//        _logger = ObjectContainer.resolve(ILoggerFactory.class).create(this.getClass());
//        _framer = ObjectContainer.resolve(IMessageFramer.class);
//        _framer.registerMessageArrivedCallback(OnMessageArrived);
//
//        tryReceive();
//        trySend();
//    }
//
//    public void QueueMessage(Byte[] message) {
//        if (message.length == 0) {
//            return;
//        }
//
//        List<ArraySegment<Byte>> segments = _framer.frameData(new ArraySegment(message, 0, message.length));
//        _sendingQueue.add(segments);
//        _pendingMessageCount.incrementAndGet();
//
//        trySend();
//    }
//
//    @Override
//    public void close() {
//        CloseInternal(SocketError.Success, "Socket normal close.", null);
//    }
//
//    private void trySend() {
//        if (_closing.get()) return;
//        if (!enterSending()) return;
//
//        //_sendingStream.setLength(0);
//        _sendingStream.clear();
//
//        List<ArraySegment<Byte>> segments = _sendingQueue.poll();
//
//        while (segments != null) {
//            _pendingMessageCount.decrementAndGet();
//
//            segments.forEach(segment -> _sendingStream.put(segment.getArray()));
//            _sendingStream.put()
//            foreach(var segment in segments)
//            {
//                _sendingStream.write(segment.Array, segment.Offset, segment.Count);
//            }
//            if (_sendingStream.Length >= _setting.MaxSendPacketSize) {
//                break;
//            }
//        }
//
//        if (_sendingStream.Length == 0) {
//            exitSending();
//            if (_sendingQueue.size() > 0) {
//                trySend();
//            }
//            return;
//        }
//
//        try {
//            _sendSocketArgs.SetBuffer(_sendingStream.GetBuffer(), 0, (int) _sendingStream.Length);
//            var firedAsync = _sendSocketArgs.AcceptSocket.SendAsync(_sendSocketArgs);
//            if (!firedAsync) {
//                ProcessSend(_sendSocketArgs);
//            }
//        } catch (Exception ex) {
//            CloseInternal(SocketError.Shutdown, "Socket send error, errorMessage:" + ex.Message, ex);
//            exitSending();
//        }
//    }
//
//    private boolean enterSending() {
//        return _sending.compareAndSet(false, true);
//    }
//
//    private void exitSending() {
//        _sending.set(false);
//    }
//}
