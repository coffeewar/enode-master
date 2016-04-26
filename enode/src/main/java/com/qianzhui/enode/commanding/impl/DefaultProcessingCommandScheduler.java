package com.qianzhui.enode.commanding.impl;

import com.qianzhui.enode.commanding.IProcessingCommandScheduler;
import com.qianzhui.enode.commanding.ProcessingCommandMailbox;

import java.util.concurrent.CompletableFuture;

/**
 * Created by junbo_xu on 2016/4/23.
 */
public class DefaultProcessingCommandScheduler implements IProcessingCommandScheduler {
    public void scheduleMailbox(ProcessingCommandMailbox mailbox) {
        if (mailbox.enterHandlingMessage()) {
            CompletableFuture.runAsync(mailbox::run);
        }
    }
}
