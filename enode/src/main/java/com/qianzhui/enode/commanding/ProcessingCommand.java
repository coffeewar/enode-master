package com.qianzhui.enode.commanding;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by junbo_xu on 2016/3/14.
 */
public class ProcessingCommand {
    private ProcessingCommandMailbox mailbox;
    private long sequence;
    private final ICommand message;
    private final ICommandExecuteContext commandExecuteContext;
    private final Map<String, String> items;
    private long enqueueTimestamp;

    public ProcessingCommand(ICommand command, ICommandExecuteContext commandExecuteContext, Map<String, String> items) {
        this.message = command;
        this.commandExecuteContext = commandExecuteContext;
        this.items = items == null ? new HashMap<>() : items;
    }

    public void complete(CommandResult commandResult) {
        commandExecuteContext.onCommandExecuted(commandResult);
    }

    public ProcessingCommandMailbox getMailbox() {
        return mailbox;
    }

    public void setMailbox(ProcessingCommandMailbox mailbox) {
        this.mailbox = mailbox;
    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public ICommand getMessage() {
        return message;
    }

    public ICommandExecuteContext getCommandExecuteContext() {
        return commandExecuteContext;
    }

    public Map<String, String> getItems() {
        return items;
    }

    public long getEnqueueTimestamp() {
        return enqueueTimestamp;
    }

    void setEnqueueTime(Date enqueueTime) {
        this.enqueueTimestamp = enqueueTime.getTime();
    }
}
