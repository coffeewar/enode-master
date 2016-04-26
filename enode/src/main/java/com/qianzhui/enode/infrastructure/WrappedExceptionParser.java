package com.qianzhui.enode.infrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by junbo_xu on 2016/3/21.
 */
public class WrappedExceptionParser<T extends Throwable> {

    public static <T extends Throwable> WrappedExceptionParser<T> create(T e) {
        return new WrappedExceptionParser(e instanceof WrappedRuntimeException ? ((WrappedRuntimeException) e).getException() : e);
    }

    private T exception;
    private List<Class<? extends Throwable>> expectExceptionTypes;
    private boolean disrupt;

    private WrappedExceptionParser(T e) {
        this.exception = e;
        expectExceptionTypes = new ArrayList<>();
    }

    public <ExpectType extends Throwable> When<ExpectType, T> when(Class<ExpectType> expectTypes) {
        if (!disrupt) {
            return new WhenImpl<>(expectTypes, this);
        }

        return UNDO;
    }

    public WrappedExceptionParser<T> elze(Consumer<T> consumer) {
        if(!this.disrupt){
            this.disrupt = true;
            consumer.accept(this.exception);
        }
        return this;
    }

    private When UNDO = new UndoWhen();

    static class WhenImpl<ExpectType extends Throwable, OrigType extends Throwable> implements When<ExpectType, OrigType> {

        private Class<ExpectType> expectExceptionType;
        private WrappedExceptionParser<OrigType> parser;

        public WhenImpl(Class<ExpectType> expectExceptionType, WrappedExceptionParser<OrigType> parser) {
            this.expectExceptionType = expectExceptionType;
        }

        @Override
        public WrappedExceptionParser<OrigType> then(Consumer<ExpectType> consumer) {
            if (parser.exception.getClass() == expectExceptionType) {
                parser.disrupt = true;
                consumer.accept((ExpectType) parser.exception);
            }

            return parser;
        }
    }

    class UndoWhen implements When {
        @Override
        public WrappedExceptionParser then(Consumer consumer) {
            return WrappedExceptionParser.this;
        }
    }
}
