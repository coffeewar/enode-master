package com.qianzhui.enode.rocketmq.client.ons;

import com.alibaba.rocketmq.remoting.CommandCustomHeader;
import com.alibaba.rocketmq.remoting.RPCHook;
import com.alibaba.rocketmq.remoting.protocol.RemotingCommand;

import java.lang.reflect.Field;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @auther lansheng.zj
 */
public class ClientRPCHook implements RPCHook {

    private SessionCredentials sessionCredentials;
    private ConcurrentHashMap<Class<? extends CommandCustomHeader>, Field[]> fieldCache;


    public ClientRPCHook(SessionCredentials sessionCredentials) {
        this.sessionCredentials = sessionCredentials;
        fieldCache = new ConcurrentHashMap<>();
    }


    @Override
    public void doBeforeRequest(String remoteAddr, RemotingCommand request) {
        CommandCustomHeader header = request.readCustomHeader();
        try {
            // sort property
            SortedMap<String, String> map = new TreeMap<String, String>();
            map.put(SessionCredentials.AccessKey, sessionCredentials.getAccessKey());

            // add header properties
            if (null != header) {
                Field[] fields = fieldCache.get(header.getClass());
                if (null == fields) {
                    fields = header.getClass().getDeclaredFields();
                    for (Field field : fields) {
                        field.setAccessible(true);
                    }
                    Field[] tmp = fieldCache.putIfAbsent(header.getClass(), fields);
                    if (null != tmp) {
                        fields = tmp;
                    }
                }

                for (Field field : fields) {
                    Object value = field.get(header);
                    if (null != value) {
                        map.put(field.getName(), value.toString());
                    }
                }
            }

            StringBuilder sb = new StringBuilder("");
            for (Entry<String, String> entry : map.entrySet()) {
                sb.append(entry.getValue());
            }

            byte[] total = ONSUtil.combineBytes(sb.toString().getBytes(SessionCredentials.CHARSET), request.getBody());
            String signature = SpasSigner.sign(total, sessionCredentials.getSecretKey());

            request.addExtField(SessionCredentials.Signature, signature);
            request.addExtField(SessionCredentials.AccessKey, sessionCredentials.getAccessKey());
        } catch (Exception e) {
            throw new RuntimeException("incompatible exception.", e);
        }
    }


    @Override
    public void doAfterResponse(String remoteAddr, RemotingCommand request, RemotingCommand response) {
    }

}
