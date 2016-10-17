package com.qianzhui.enode.rocketmq.trace.core;

import com.alibaba.rocketmq.client.hook.SendMessageContext;
import com.alibaba.rocketmq.client.hook.SendMessageHook;
import com.alibaba.rocketmq.client.producer.SendStatus;
import com.qianzhui.enode.rocketmq.trace.core.common.OnsTraceBean;
import com.qianzhui.enode.rocketmq.trace.core.common.OnsTraceContext;
import com.qianzhui.enode.rocketmq.trace.core.common.OnsTraceType;
import com.qianzhui.enode.rocketmq.trace.core.dispatch.AsyncDispatcher;

import java.util.ArrayList;

/**
 * Created by junbo_xu on 2016/10/17.
 */
public class OnsClientSendMessageHookImpl implements SendMessageHook {
    private AsyncDispatcher localDispatcher;

    public OnsClientSendMessageHookImpl(AsyncDispatcher localDispatcher) {
        this.localDispatcher = localDispatcher;
    }

    public String hookName() {
        return "OnsClientSendMessageHook";
    }

    public void sendMessageBefore(SendMessageContext context) {
        if (context != null && !context.getMessage().getTopic().startsWith("rmq_sys_")) {
            OnsTraceContext onsContext = new OnsTraceContext();
            onsContext.setTraceBeans(new ArrayList(1));
            context.setMqTraceContext(onsContext);
            onsContext.setTraceType(OnsTraceType.Pub);
            onsContext.setGroupName(context.getProducerGroup());
            OnsTraceBean traceBean = new OnsTraceBean();
            traceBean.setTopic(context.getMessage().getTopic());
            traceBean.setTags(context.getMessage().getTags());
            traceBean.setKeys(context.getMessage().getKeys());
            traceBean.setStoreHost(context.getBrokerAddr());
            traceBean.setBodyLength(context.getMessage().getBody().length);
            traceBean.setMsgType(context.getMsgType());
            onsContext.getTraceBeans().add(traceBean);
        }
    }

    public void sendMessageAfter(SendMessageContext context) {
        if (context != null && !context.getMessage().getTopic().startsWith("rmq_sys_TRACE_DATA_") && context.getMqTraceContext() != null) {
            OnsTraceContext onsContext = (OnsTraceContext) context.getMqTraceContext();
            OnsTraceBean traceBean = (OnsTraceBean) onsContext.getTraceBeans().get(0);
            int costTime = (int) ((System.currentTimeMillis() - onsContext.getTimeStamp()) / (long) onsContext.getTraceBeans().size());
            onsContext.setCostTime(costTime);
            if (context.getSendResult().getSendStatus().equals(SendStatus.SEND_OK)) {
                onsContext.setSuccess(true);
            } else {
                onsContext.setSuccess(false);
            }

            onsContext.setRegionId(context.getSendResult().getRegionId());
            traceBean.setMsgId(context.getSendResult().getMsgId());
            traceBean.setOffsetMsgId(context.getSendResult().getOffsetMsgId());
            traceBean.setStoreTime(onsContext.getTimeStamp() + (long) (costTime / 2));
            this.localDispatcher.append(onsContext);
        }
    }
}

