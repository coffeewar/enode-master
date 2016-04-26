package com.qianzhui.enode.commanding;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by junbo_xu on 2016/3/14.
 */
public class ProcessingCommand {
    public ProcessingCommandMailbox mailbox;
    public long sequence;
    public ICommand message;
    public ICommandExecuteContext commandExecuteContext;
    public Map<String, String> items;

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
}
