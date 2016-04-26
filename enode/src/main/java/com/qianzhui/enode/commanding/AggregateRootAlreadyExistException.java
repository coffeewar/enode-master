package com.qianzhui.enode.commanding;

/**
 * Created by junbo_xu on 2016/3/17.
 */
public class AggregateRootAlreadyExistException extends RuntimeException {
    private final static String ExceptionMessage = "Aggregate root [type=%s,id=%s] already exist in command context, cannot be added again.";

    public AggregateRootAlreadyExistException(Object id, Class type) {
        super(String.format(ExceptionMessage, type.getName(), id));
    }

}
