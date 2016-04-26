package com.qianzhui.enode.rocketmq.command;

/**
 * Created by junbo_xu on 2016/3/9.
 */
public class CommandMessage {
    private String commandData;
    private String replyAddress;

    public CommandMessage(String commandData, String replyAddress) {
        this.commandData = commandData;
        this.replyAddress = replyAddress;
    }

    public String getCommandData() {
        return commandData;
    }

    public void setCommandData(String commandData) {
        this.commandData = commandData;
    }

    public String getReplyAddress() {
        return replyAddress;
    }

    public void setReplyAddress(String replyAddress) {
        this.replyAddress = replyAddress;
    }
}
