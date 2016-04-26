package com.qianzhui.enode.common.remoting;

import java.util.concurrent.CompletableFuture;

/**
 * Created by junbo_xu on 2016/3/11.
 */
public class ResponseFuture {
    private CompletableFuture<RemotingResponse> taskSource;

    private final long beginTimestamp = System.currentTimeMillis();
    private long timeoutMillis;
    private RemotingRequest request;

    public ResponseFuture(RemotingRequest request, long timeoutMillis, CompletableFuture<RemotingResponse> taskSource) {
        this.request = request;
        this.timeoutMillis = timeoutMillis;
        this.taskSource = taskSource;
    }

    public boolean isTimeout() {

        return (System.currentTimeMillis() - beginTimestamp) > timeoutMillis;
    }

    boolean setResponse(RemotingResponse response) {
        return taskSource.complete(response);
    }

    public CompletableFuture<RemotingResponse> getTaskSource() {
        return taskSource;
    }

    private void setTaskSource(CompletableFuture<RemotingResponse> taskSource) {
        this.taskSource = taskSource;
    }

    public long getBeginTimestamp() {
        return beginTimestamp;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    private void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public RemotingRequest getRequest() {
        return request;
    }

    private void setRequest(RemotingRequest request) {
        this.request = request;
    }
}
