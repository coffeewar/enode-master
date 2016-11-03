package com.qianzhui.enode.rocketmq.client.ons;

import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.common.message.Message;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.alibaba.rocketmq.common.message.MessageQueue;
import com.qianzhui.enode.rocketmq.client.AbstractProducer;
import com.qianzhui.enode.rocketmq.client.MQClientInitializer;
import com.qianzhui.enode.rocketmq.client.Producer;
import com.qianzhui.enode.rocketmq.trace.core.OnsClientSendMessageHookImpl;
import com.qianzhui.enode.rocketmq.trace.core.common.OnsTraceConstants;
import com.qianzhui.enode.rocketmq.trace.core.dispatch.AsyncDispatcher;
import com.qianzhui.enode.rocketmq.trace.core.dispatch.impl.AsyncArrayDispatcher;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Properties;

/**
 * Created by junbo_xu on 2016/4/20.
 */
public class ONSProducerImpl extends AbstractProducer implements Producer {

    private static final Log log = LogFactory.getLog(ONSProducerImpl.class);

    public static void main(String[] args) {
        startConsumer();

        Properties properties = new Properties();

        properties.put(PropertyKeyConst.ProducerId, "jslink-test");

        properties.put(PropertyKeyConst.AccessKey, "G6aUujQD6m1Uyy68");

        properties.put(PropertyKeyConst.SecretKey,
                "TR6MUs6R8dK6GTOKudmaaY80K2dmxI");
        ONSProducerImpl producer = new ONSProducerImpl(properties);

        producer.start();

        Message msg = new Message(

                // Message Topic
                "jslink-test",
                // Message Tag,
                // 可理解为Gmail中的标签，对消息进行再归类，方便Consumer指定过滤条件在ONS服务器过滤
                "Tags",
                // Message Body
                // 任何二进制形式的数据，ONS不做任何干预，需要Producer与Consumer协商好一致的序列化和反序列化方式
                "TestWithConsumer".getBytes());


        SendResult sendResult = producer.send(msg, (final List<MessageQueue> mqs, final Message m, final Object arg) -> mqs.get(0), "test");

        System.out.println(sendResult);

        // 在应用退出前，销毁Producer对象
        // 注意：如果不销毁也没有问题
        producer.shutdown();
    }

    private static void startConsumer() {
        Properties properties = new Properties();

        properties.put(PropertyKeyConst.ConsumerId, "CID-jstest");

        properties.put(PropertyKeyConst.AccessKey, "G6aUujQD6m1Uyy68");

        properties.put(PropertyKeyConst.SecretKey,
                "TR6MUs6R8dK6GTOKudmaaY80K2dmxI");

        ONSConsumerImpl consumer = new ONSConsumerImpl(properties);

        consumer.registerMessageListener((final List<MessageExt> msgs,
                                          final ConsumeConcurrentlyContext context) -> {
            MessageExt message = msgs.get(0);
            System.out.println("Test11-cluster:" + message);

            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        consumer.subscribe("jslink-test", "*");

        consumer.start();

        System.out.println("Consumer Started");
    }

    public ONSProducerImpl(final Properties properties) {
        super(properties, new ONSClientInitializer());
    }

    @Override
    protected DefaultMQProducer initProducer(Properties properties, MQClientInitializer clientInitializer) {
        SessionCredentials sessionCredentials = ((ONSClientInitializer) clientInitializer).sessionCredentials;
        DefaultMQProducer defaultMQProducer = new DefaultMQProducer(new ClientRPCHook(sessionCredentials));

        String producerGroup =
                properties.getProperty(PropertyKeyConst.ProducerId, "__ONS_PRODUCER_DEFAULT_GROUP");
        defaultMQProducer.setProducerGroup(producerGroup);

        boolean isVipChannelEnabled = Boolean.parseBoolean(properties.getProperty("isVipChannelEnabled", "false"));
        defaultMQProducer.setVipChannelEnabled(isVipChannelEnabled);

        String sendMsgTimeoutMillis = properties.getProperty(PropertyKeyConst.SendMsgTimeoutMillis, "5000");
        defaultMQProducer.setSendMsgTimeout(Integer.parseInt(sendMsgTimeoutMillis));

        defaultMQProducer.setInstanceName(clientInitializer.buildIntanceName());
        defaultMQProducer.setNamesrvAddr(clientInitializer.getNameServerAddr());
        // 消息最大大小4M
        defaultMQProducer.setMaxMessageSize(1024 * 1024 * 4);

        // 为Producer增加消息轨迹回发模块
        try {
            Properties tempProperties = new Properties();
            tempProperties.put(OnsTraceConstants.AccessKey, sessionCredentials.getAccessKey());
            tempProperties.put(OnsTraceConstants.SecretKey, sessionCredentials.getSecretKey());
            tempProperties.put(OnsTraceConstants.MaxMsgSize, "128000");
            tempProperties.put(OnsTraceConstants.AsyncBufferSize, "2048");
            tempProperties.put(OnsTraceConstants.MaxBatchNum, "10");
            tempProperties.put(OnsTraceConstants.NAMESRV_ADDR, clientInitializer.getNameServerAddr());
            tempProperties.put(OnsTraceConstants.InstanceName, clientInitializer.buildIntanceName());
            traceDispatcher = new AsyncArrayDispatcher(tempProperties);
            traceDispatcher.start(null, defaultMQProducer.getInstanceName());
            defaultMQProducer.getDefaultMQProducerImpl().registerSendMessageHook(
                    new OnsClientSendMessageHookImpl(traceDispatcher));
        } catch (Throwable e) {
            log.error("system mqtrace hook init failed ,maybe can't send msg trace data");
        }

        return defaultMQProducer;
    }
}
