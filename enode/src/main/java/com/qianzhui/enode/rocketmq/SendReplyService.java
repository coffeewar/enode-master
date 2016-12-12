package com.qianzhui.enode.rocketmq;

import com.qianzhui.enode.common.container.ObjectContainer;
import com.qianzhui.enode.common.io.IOHelper;
import com.qianzhui.enode.common.logging.ILogger;
import com.qianzhui.enode.common.logging.ILoggerFactory;
import com.qianzhui.enode.common.remoting.RemotingRequest;
import com.qianzhui.enode.common.remoting.SocketRemotingClient;
import com.qianzhui.enode.common.scheduling.IScheduleService;
import com.qianzhui.enode.common.serializing.IJsonSerializer;
import com.qianzhui.enode.common.utilities.BitConverter;
import com.qianzhui.enode.common.utilities.Ensure;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Created by junbo_xu on 2016/3/14.
 */
public class SendReplyService {
    private ConcurrentMap<String, SocketRemotingClient> _remotingClientDict;
    private IJsonSerializer _jsonSerializer;
    private IScheduleService _scheduleService;
    private IOHelper _ioHelper;
    private ILogger _logger;
    private final String _scanInactiveCommandRemotingClientTaskName;

    public SendReplyService() {
        _remotingClientDict = new ConcurrentHashMap<>();
        _jsonSerializer = ObjectContainer.resolve(IJsonSerializer.class);
        _scheduleService = ObjectContainer.resolve(IScheduleService.class);
        _ioHelper = ObjectContainer.resolve(IOHelper.class);
        _logger = ObjectContainer.resolve(ILoggerFactory.class).create(SendReplyService.class);


        _scanInactiveCommandRemotingClientTaskName = "ScanInactiveCommandRemotingClient_" + System.nanoTime() + new Random().nextInt(10000);
    }

    public void start() {
        _scheduleService.startTask(_scanInactiveCommandRemotingClientTaskName, this::scanInactiveRemotingClients, 5000, 5000);
    }

    public void stop() {
        _scheduleService.stopTask(_scanInactiveCommandRemotingClientTaskName);
        _remotingClientDict.values().stream().forEach(x -> x.shutdown());
    }

    public void sendReply(short replyType, Object replyData, String replyAddress) {
        CompletableFuture.runAsync(() -> {
            SendReplyContext context = new SendReplyContext(replyType, replyData, replyAddress);

            try {
                SocketRemotingClient remotingClient = getRemotingClient(context.getReplyAddress());
                if (remotingClient == null) return;

                if (!remotingClient.isConnected()) {
                    _logger.error("Send command reply failed as remotingClient is not connected, replyAddress: " + context.getReplyAddress());
                    return;
                }

                String message = _jsonSerializer.serialize(context.replyData);
                byte[] body = BitConverter.getBytes(message);
                RemotingRequest request = new RemotingRequest(context.getReplyType(), body);

                remotingClient.invokeOneway(request);
            } catch (Exception ex) {
                _logger.error("Send command reply has exeption, replyAddress: " + context.getReplyAddress(), ex);
            }
        });
    }

    private void scanInactiveRemotingClients() {
        List<Map.Entry<String, SocketRemotingClient>> inactiveList = _remotingClientDict.entrySet().stream()
                .filter(x -> !x.getValue().isConnected())
                .collect(Collectors.toList());

        inactiveList.stream().forEach(entry -> {
            if (_remotingClientDict.remove(entry.getKey()) != null) {
                _logger.info("Removed disconnected command remoting client, remotingAddress: %s", entry.getKey());
            }
        });
    }

    private SocketRemotingClient getRemotingClient(String replyAddress) {
        InetSocketAddress replyEndpoint = tryParseReplyAddress(replyAddress);
        if (replyEndpoint == null) return null;

        SocketRemotingClient remotingClient = _remotingClientDict.get(toReplyAddress(replyEndpoint));

        if (remotingClient != null) {
            return remotingClient;
        }

        _ioHelper.tryIOAction("CreateReplyRemotingClient", () -> "replyAddress:" + replyAddress, () -> createReplyRemotingClient(replyEndpoint), 3);

        return _remotingClientDict.get(toReplyAddress(replyEndpoint));
    }

    private String toReplyAddress(InetSocketAddress replyAddress) {
        return String.format("%s:%d", replyAddress.getAddress().getHostAddress(), replyAddress.getPort());
    }

    private InetSocketAddress tryParseReplyAddress(String replyAddress) {
        try {
            String[] items = replyAddress.split(":");
            Ensure.equal(2, items.length, "reply address");
            return new InetSocketAddress(items[0], Integer.valueOf(items[1]));
        } catch (Exception ex) {
            _logger.error(String.format("Invalid reply address : %s", replyAddress), ex);
            return null;
        }
    }

    private SocketRemotingClient createReplyRemotingClient(InetSocketAddress replyEndpoint) {
        return _remotingClientDict.computeIfAbsent(toReplyAddress(replyEndpoint), key ->
                new SocketRemotingClient(replyEndpoint).start()
        );
    }

    class SendReplyContext {
        private short replyType;
        private Object replyData;
        private String replyAddress;

        public SendReplyContext(short replyType, Object replyData, String replyAddress) {
            this.replyType = replyType;
            this.replyData = replyData;
            this.replyAddress = replyAddress;
        }

        public short getReplyType() {
            return replyType;
        }

        public Object getReplyData() {
            return replyData;
        }

        public String getReplyAddress() {
            return replyAddress;
        }
    }
}
