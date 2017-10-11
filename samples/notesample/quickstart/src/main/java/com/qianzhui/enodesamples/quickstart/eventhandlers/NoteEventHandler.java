package com.qianzhui.enodesamples.quickstart.eventhandlers;

import com.qianzhui.enode.common.io.AsyncTaskResult;
import com.qianzhui.enode.infrastructure.IMessageHandler;
import com.qianzhui.enodesamples.notesample.domain.NoteCreated;
import com.qianzhui.enodesamples.notesample.domain.NoteTitleChanged;

import java.util.concurrent.CompletableFuture;

/**
 * Created by junbo_xu on 2016/4/15.
 */
//@Component(life = LifeStyle.Transient)
public class NoteEventHandler implements IMessageHandler {

    public NoteEventHandler() {
    }

    public CompletableFuture<AsyncTaskResult> handleAsync(NoteCreated evnt) {
        System.out.println(String.format("Note denormalizered, title：%s, Version: %d", evnt.getTitle(), evnt.version()));
        return CompletableFuture.completedFuture(AsyncTaskResult.Success);
    }

    public CompletableFuture<AsyncTaskResult> handleAsync(NoteTitleChanged evnt) {
        System.out.println(String.format("Note denormalizered, title：%s, Version: %d", evnt.getTitle(), evnt.version()));
        return CompletableFuture.completedFuture(AsyncTaskResult.Success);
    }
}
