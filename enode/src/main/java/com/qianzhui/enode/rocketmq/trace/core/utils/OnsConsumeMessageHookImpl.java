package com.qianzhui.enode.rocketmq.trace.core.utils;

import com.alibaba.rocketmq.client.hook.ConsumeMessageContext;
import com.alibaba.rocketmq.client.hook.ConsumeMessageHook;
import com.alibaba.rocketmq.common.message.MessageConst;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.qianzhui.enode.rocketmq.trace.core.common.OnsTraceBean;
import com.qianzhui.enode.rocketmq.trace.core.common.OnsTraceConstants;
import com.qianzhui.enode.rocketmq.trace.core.common.OnsTraceContext;
import com.qianzhui.enode.rocketmq.trace.core.common.OnsTraceType;
import com.qianzhui.enode.rocketmq.trace.core.dispatch.AsyncDispatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by junbo_xu on 2016/10/17.
 */
public class OnsConsumeMessageHookImpl implements ConsumeMessageHook {
    /**
     * 该Hook该由哪个dispatcher发送轨迹数据
     */
    private AsyncDispatcher localDispatcher;


    public OnsConsumeMessageHookImpl(AsyncDispatcher localDispatcher) {
        this.localDispatcher = localDispatcher;
    }


    @Override
    public String hookName() {
        return "OnsConsumeMessageHook";
    }


    @Override
    public void consumeMessageBefore(ConsumeMessageContext context) {
        if (context == null || context.getMsgList() == null || context.getMsgList().isEmpty()) {
            return;
        }
        OnsTraceContext onsTraceContext = new OnsTraceContext();
        context.setMqTraceContext(onsTraceContext);
        onsTraceContext.setTraceType(OnsTraceType.SubBefore);//
        onsTraceContext.setGroupName(context.getConsumerGroup());//
        List<OnsTraceBean> beans = new ArrayList<OnsTraceBean>();
        for (MessageExt msg : context.getMsgList()) {
            if (msg == null) {
                continue;
            }
            OnsTraceBean traceBean = new OnsTraceBean();
            traceBean.setTopic(msg.getTopic());//
            traceBean.setMsgId(msg.getMsgId());//
            traceBean.setTags(msg.getTags());//
            traceBean.setKeys(msg.getKeys());//
//            InetSocketAddress host;
//            // host = (InetSocketAddress) msg.getBornHost();
//            // traceBean.setClientHost(host.getHostName());//
//
//            host = (InetSocketAddress) msg.getStoreHost();
//            traceBean.setStoreHost(host.getHostName());//
            traceBean.setStoreTime(msg.getStoreTimestamp());//
            traceBean.setBodyLength(msg.getStoreSize());//
            traceBean.setRetryTimes(msg.getReconsumeTimes());//
            String regionId = msg.getProperty(MessageConst.PROPERTY_MSG_REGION);
            if (regionId == null) {
                regionId = OnsTraceConstants.default_region;
            }
            onsTraceContext.setRegionId(regionId);//
            beans.add(traceBean);
        }
        onsTraceContext.setTraceBeans(beans);
        onsTraceContext.setTimeStamp(System.currentTimeMillis());
        localDispatcher.append(onsTraceContext);
    }


    @Override
    public void consumeMessageAfter(ConsumeMessageContext context) {
        if (context == null || context.getMsgList() == null || context.getMsgList().isEmpty()) {
            return;
        }
        OnsTraceContext subBeforeContext = (OnsTraceContext) context.getMqTraceContext();
        OnsTraceContext subAfterContext = new OnsTraceContext();
        subAfterContext.setTraceType(OnsTraceType.SubAfter);//
        subAfterContext.setRegionId(subBeforeContext.getRegionId());//
        subAfterContext.setGroupName(subBeforeContext.getGroupName());//
        subAfterContext.setRequestId(subBeforeContext.getRequestId());//
        subAfterContext.setSuccess(context.isSuccess());//
        // 批量消息全部处理完毕的平均耗时
        int costTime = (int) ((System.currentTimeMillis() - subBeforeContext.getTimeStamp()) / context.getMsgList().size());
        subAfterContext.setCostTime(costTime);//
        subAfterContext.setTraceBeans(subBeforeContext.getTraceBeans());
        localDispatcher.append(subAfterContext);
    }
}
