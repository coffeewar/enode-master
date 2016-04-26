package com.qianzhui.enode.rocketmq;

/**
 * Created by junbo_xu on 2016/3/13.
 */
public enum CommandReplyType {
    Other((short)0),//保持与C#状态一致
    CommandExecuted((short)1),
    DomainEventHandled((short)2);

    short value;

    CommandReplyType(short value){
        this.value=value;
    }

    public short getValue(){
        return value;
    }
}
