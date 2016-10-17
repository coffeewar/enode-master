package com.qianzhui.enode.rocketmq.trace.core.common;

import com.alibaba.rocketmq.common.message.MessageClientIDSetter;

import java.util.Iterator;
import java.util.List;

/**
 * Created by junbo_xu on 2016/10/17.
 */
public class OnsTraceContext implements Comparable<OnsTraceContext> {
    private OnsTraceType traceType;
    private long timeStamp = System.currentTimeMillis();
    private String regionId = "";
    private String groupName = "";
    private int costTime = 0;
    private boolean isSuccess = true;
    private String requestId = MessageClientIDSetter.createUniqID();
    private List<OnsTraceBean> traceBeans;

    public OnsTraceContext() {
    }

    public List<OnsTraceBean> getTraceBeans() {
        return this.traceBeans;
    }

    public void setTraceBeans(List<OnsTraceBean> traceBeans) {
        this.traceBeans = traceBeans;
    }

    public String getRegionId() {
        return this.regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public OnsTraceType getTraceType() {
        return this.traceType;
    }

    public void setTraceType(OnsTraceType traceType) {
        this.traceType = traceType;
    }

    public long getTimeStamp() {
        return this.timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getGroupName() {
        return this.groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public int getCostTime() {
        return this.costTime;
    }

    public void setCostTime(int costTime) {
        this.costTime = costTime;
    }

    public boolean isSuccess() {
        return this.isSuccess;
    }

    public void setSuccess(boolean success) {
        this.isSuccess = success;
    }

    public String getRequestId() {
        return this.requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public int compareTo(OnsTraceContext o) {
        return (int)(this.timeStamp - o.getTimeStamp());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(1024);
        if(this.traceBeans != null && this.traceBeans.size() > 0) {
            Iterator i$ = this.traceBeans.iterator();

            while(i$.hasNext()) {
                OnsTraceBean bean = (OnsTraceBean)i$.next();
                sb.append(bean.getMsgId() + "_");
            }
        }

        sb.append(this.traceType);
        return "OnsTraceContext{traceBeans=" + sb.toString() + '}';
    }
}