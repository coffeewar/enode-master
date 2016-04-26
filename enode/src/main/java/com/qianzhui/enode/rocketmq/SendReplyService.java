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
import java.net.SocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Created by junbo_xu on 2016/3/14.
 */
public class SendReplyService {
    private ConcurrentMap<String, SocketRemotingClientWrapper> _clientWrapperDict;
    private IJsonSerializer _jsonSerializer;
    private IScheduleService _scheduleService;
    private IOHelper _ioHelper;
    private ILogger _logger;
    private final int MaxNotActiveTimeSeconds = 300;
    private final int ScanNotActiveClientInterval = 5000;

    public SendReplyService() {
        _clientWrapperDict = new ConcurrentHashMap<>();
        _jsonSerializer = ObjectContainer.resolve(IJsonSerializer.class);
        _scheduleService = ObjectContainer.resolve(IScheduleService.class);
        _ioHelper = ObjectContainer.resolve(IOHelper.class);
        _logger = ObjectContainer.resolve(ILoggerFactory.class).create(SendReplyService.class);
    }

    public void start() {
        _scheduleService.startTask("RemoveNotActiveRemotingClient", this::removeNotActiveRemotingClient, 1000, ScanNotActiveClientInterval);
    }

    public void stop() {
        _scheduleService.stopTask("RemoveNotActiveRemotingClient");
        _clientWrapperDict.values().stream().forEach(x->x.getRemotingClient().shutdown());
    }

    public void sendReply(short replyType, Object replyData, String replyAddress) {
        CompletableFuture.runAsync(()->{
            SendReplyContext context = new SendReplyContext(replyType, replyData, replyAddress);

            try {
                SocketRemotingClientWrapper clientWrapper = getRemotingClientWrapper(context.getReplyAddress());
                if (clientWrapper == null)
                    return;

                String message = _jsonSerializer.serialize(context.replyData);
                byte[] body = BitConverter.getBytes(message);
                RemotingRequest request = new RemotingRequest(context.getReplyType(), body);

                clientWrapper.getRemotingClient().invokeOneway(request);
            } catch (Exception ex) {
                _logger.error("Send command reply failed, replyAddress: " + context.getReplyAddress(), ex);
            }
        });
    }

    private void removeNotActiveRemotingClient() {
        List<Map.Entry<String, SocketRemotingClientWrapper>> expiredEntries = _clientWrapperDict.entrySet().stream().filter(x -> x.getValue().isNotActive(MaxNotActiveTimeSeconds)).collect(Collectors.toList());
        expiredEntries.forEach(entry -> {
            SocketRemotingClientWrapper clientWrapper = _clientWrapperDict.remove(entry.getKey());
            if (clientWrapper != null) {
                clientWrapper.getRemotingClient().shutdown();
                _logger.info("Closed and removed not active remoting client: %s, lastActiveTime: %tc", entry.getKey(), new Date(clientWrapper.getLastActiveMillTime()));
            }
        });
    }

    private SocketRemotingClientWrapper getRemotingClientWrapper(String replyAddress) {
        SocketRemotingClientWrapper remotingClientWrapper = _clientWrapperDict.get(replyAddress);

        if (remotingClientWrapper != null) {
            return remotingClientWrapper;
        }

        SocketAddress replyEndpoint = tryParseReplyAddress(replyAddress);
        if (replyEndpoint == null) return null;

        return _ioHelper.tryIOFunc("CreateReplyRemotingClient", () -> "replyAddress:" + replyAddress, () ->
                        createReplyRemotingClient(replyEndpoint)
                , 3);
    }

    private SocketAddress tryParseReplyAddress(String replyAddress) {
        try {
            String[] items = replyAddress.split(":");
            Ensure.equal(2, items.length, "reply address");
            return new InetSocketAddress(items[0], Integer.valueOf(items[1]));
        } catch (Exception ex) {
            _logger.error(String.format("Invalid reply address : %s", replyAddress), ex);
            return null;
        }
    }

    private SocketRemotingClientWrapper createReplyRemotingClient(SocketAddress replyEndpoint) {
        return _clientWrapperDict.computeIfAbsent(replyEndpoint.toString(), key -> {
            SocketRemotingClient remotingClient = new SocketRemotingClient(replyEndpoint).start();
            return new SocketRemotingClientWrapper(replyEndpoint, remotingClient, System.currentTimeMillis());
        });
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

    class SocketRemotingClientWrapper {

        private SocketAddress replyEndpoint;
        private SocketRemotingClient remotingClient;
        private long lastActiveMillTime;

        public SocketRemotingClientWrapper(SocketAddress replyEndpoint, SocketRemotingClient remotingClient, long lastActiveMillTime) {
            this.replyEndpoint = replyEndpoint;
            this.remotingClient = remotingClient;
            this.lastActiveMillTime = lastActiveMillTime;
        }

        public boolean isNotActive(int maxNotActiveSeconds) {
            return (System.currentTimeMillis() - lastActiveMillTime) >= maxNotActiveSeconds;
        }

        public SocketAddress getReplyEndpoint() {
            return replyEndpoint;
        }

        public void setReplyEndpoint(SocketAddress replyEndpoint) {
            this.replyEndpoint = replyEndpoint;
        }

        public SocketRemotingClient getRemotingClient() {
            return remotingClient;
        }

        public void setRemotingClient(SocketRemotingClient remotingClient) {
            this.remotingClient = remotingClient;
        }

        public long getLastActiveMillTime() {
            return lastActiveMillTime;
        }

        public void setLastActiveMillTime(long lastActiveMillTime) {
            this.lastActiveMillTime = lastActiveMillTime;
        }
    }
}
