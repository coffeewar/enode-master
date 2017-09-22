package com.qianzhui.enodesamples.quickstart.eventhandlers;

import com.qianzhui.enode.common.io.AsyncTaskResult;
import com.qianzhui.enode.common.logging.ILogger;
import com.qianzhui.enode.common.logging.ILoggerFactory;
import com.qianzhui.enode.infrastructure.IMessageHandler;
import com.qianzhui.enodesamples.notesample.domain.NoteCreated;
import com.qianzhui.enodesamples.notesample.domain.NoteTitleChanged;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;

/**
 * Created by junbo_xu on 2016/4/15.
 */
//@Component(life = LifeStyle.Transient)
public class NoteEventHandler implements IMessageHandler {
    private ILogger _logger;

    @Inject
    public NoteEventHandler(ILoggerFactory loggerFactory) {
        _logger = loggerFactory.create(getClass());
    }

    public CompletableFuture<AsyncTaskResult> handleAsync(NoteCreated evnt) {
        _logger.info("Note denormalizered, title：%s, Version: %d", evnt.getTitle(), evnt.version());
        return CompletableFuture.completedFuture(AsyncTaskResult.Success);
    }

    public CompletableFuture<AsyncTaskResult> handleAsync(NoteTitleChanged evnt) {
        _logger.info("Note denormalizered, title：%s, Version: %d", evnt.getTitle(), evnt.version());
        return CompletableFuture.completedFuture(AsyncTaskResult.Success);
    }
}
