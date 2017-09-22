package com.qianzhui.enode.infrastructure.impl;

import com.qianzhui.enode.common.io.AsyncTaskResult;
import com.qianzhui.enode.common.io.IOHelper;
import com.qianzhui.enode.common.logging.ILogger;
import com.qianzhui.enode.common.logging.ILoggerFactory;
import com.qianzhui.enode.infrastructure.*;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;

/**
 * Created by junbo_xu on 2016/4/5.
 */
public abstract class AbstractSequenceProcessingMessageHandler<X extends IProcessingMessage<X, Y> & ISequenceProcessingMessage, Y extends ISequenceMessage> implements IProcessingMessageHandler<X, Y> {
    private final IPublishedVersionStore _publishedVersionStore;
    private final IOHelper _ioHelper;
    private final ILogger _logger;

    public abstract String getName();

    @Inject
    public AbstractSequenceProcessingMessageHandler(IPublishedVersionStore publishedVersionStore, IOHelper ioHelper, ILoggerFactory loggerFactory) {
        _publishedVersionStore = publishedVersionStore;
        _ioHelper = ioHelper;
        _logger = loggerFactory.create(getClass());
    }

    protected abstract CompletableFuture<AsyncTaskResult> dispatchProcessingMessageAsync(X processingMessage);

    public void handleAsync(X processingMessage) {
        handleMessageAsync(processingMessage, 0);
    }

    private void handleMessageAsync(X processingMessage, int retryTimes) {
        Y message = processingMessage.getMessage();

        _ioHelper.tryAsyncActionRecursively("GetPublishedVersionAsync",
                () -> _publishedVersionStore.getPublishedVersionAsync(getName(), message.aggregateRootTypeName(), message.aggregateRootStringId()),
                currentRetryTimes -> handleMessageAsync(processingMessage, currentRetryTimes),
                result ->
                {
                    Integer publishedVersion = result.getData();
                    if (publishedVersion + 1 == message.version()) {
                        dispatchProcessingMessageAsync(processingMessage, 0);
                    } else if (publishedVersion + 1 < message.version()) {
                        _logger.info("The sequence message cannot be process now as the version is not the next version, it will be handle later. contextInfo [aggregateRootId=%s,lastPublishedVersion=%d,messageVersion=%d]", message.aggregateRootStringId(), publishedVersion, message.version());
                        processingMessage.addToWaitingList();
                    } else {
                        //TODO default(Z)
                        processingMessage.complete();
                    }
                },
                () -> String.format("sequence message [messageId:%s, messageType:%s, aggregateRootId:%s, aggregateRootVersion:%s]", message.id(), message.getClass().getName(), message.aggregateRootStringId(), message.version()),
                errorMessage ->

                        _logger.fatal(String.format("Get published version has unknown exception, the code should not be run to here, errorMessage: %s", errorMessage)),
                retryTimes,
                true);
    }

    private void dispatchProcessingMessageAsync(X processingMessage, int retryTimes) {
        _ioHelper.tryAsyncActionRecursively("DispatchProcessingMessageAsync",
                () -> dispatchProcessingMessageAsync(processingMessage),
                currentRetryTimes -> dispatchProcessingMessageAsync(processingMessage, currentRetryTimes),
                result -> updatePublishedVersionAsync(processingMessage, 0),
                () -> String.format("sequence message [messageId:%s, messageType:%s, aggregateRootId:%s, aggregateRootVersion:%d]", processingMessage.getMessage().id(), processingMessage.getMessage().getClass().getName(), processingMessage.getMessage().aggregateRootStringId(), processingMessage.getMessage().version()),
                errorMessage ->

                        _logger.fatal(String.format("Dispatching message has unknown exception, the code should not be run to here, errorMessage: %s", errorMessage)),
                retryTimes,
                true);
    }

    private void updatePublishedVersionAsync(X processingMessage, int retryTimes) {
        _ioHelper.tryAsyncActionRecursively("UpdatePublishedVersionAsync",
                () -> _publishedVersionStore.updatePublishedVersionAsync(getName(), processingMessage.getMessage().aggregateRootTypeName(), processingMessage.getMessage().aggregateRootStringId(), processingMessage.getMessage().version()),
                currentRetryTimes -> updatePublishedVersionAsync(processingMessage, currentRetryTimes),
                result ->
                {
                    //TODO default(Z)
                    processingMessage.complete();
                },
                () -> String.format("sequence message [messageId:%s, messageType:%s, aggregateRootId:%s, aggregateRootVersion:%d]", processingMessage.getMessage().id(), processingMessage.getMessage().getClass().getName(), processingMessage.getMessage().aggregateRootStringId(), processingMessage.getMessage().version()),
                errorMessage ->
                        _logger.fatal(String.format("Update published version has unknown exception, the code should not be run to here, errorMessage: %s", errorMessage)),
                retryTimes,
                true);
    }
}
