package com.qianzhui.enode.common.io;

import com.qianzhui.enode.common.function.*;
import com.qianzhui.enode.common.logging.ENodeLogger;
import com.qianzhui.enode.common.utilities.Ensure;
import com.qianzhui.enode.infrastructure.WrappedRuntimeException;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * Created by junbo_xu on 2016/3/2.
 */
public class IOHelper {
    private static final Logger logger = ENodeLogger.getLog();

    public void tryIOAction(String actionName, Func<String> getContextInfo, Action action, int maxRetryTimes) {
        tryIOAction(actionName, getContextInfo, action, maxRetryTimes, false, 1000);
    }

    public void tryIOAction(String actionName, Func<String> getContextInfo, Action action, int maxRetryTimes, boolean continueRetryWhenRetryFailed, int retryInterval) {
        Ensure.notNull(actionName, "actionName");
        Ensure.notNull(getContextInfo, "getContextInfo");
        Ensure.notNull(action, "action");
        tryIOActionRecursivelyInternal(actionName, getContextInfo, (x, y, z) ->
                action.apply()
        , 0, maxRetryTimes, continueRetryWhenRetryFailed, retryInterval);
    }

    public <T> T tryIOFunc(String funcName, Func<String> getContextInfo, Func<T> func, int maxRetryTimes) {
        return tryIOFunc(funcName, getContextInfo, func, maxRetryTimes, false, 1000);
    }

    public <T> T tryIOFunc(String funcName, Func<String> getContextInfo, Func<T> func, int maxRetryTimes, boolean continueRetryWhenRetryFailed, int retryInterval) {
        Ensure.notNull(funcName, "funcName");
        Ensure.notNull(getContextInfo, "getContextInfo");
        Ensure.notNull(func, "func");
        return tryIOFuncRecursivelyInternal(funcName, getContextInfo, (x, y, z) ->func.apply(), 0, maxRetryTimes, continueRetryWhenRetryFailed, retryInterval);
    }

    /**
     * ========== TryAsyncActionRecursively =========
     */
    public <TAsyncResult extends AsyncTaskResult> void tryAsyncActionRecursively(
            String asyncActionName,
            Func<CompletableFuture<TAsyncResult>> asyncAction,
            Action1<Integer> mainAction,
            Action1<TAsyncResult> successAction,
            Func<String> getContextInfoFunc,
            Action1<String> failedAction,
            int retryTimes) {
        tryAsyncActionRecursively(asyncActionName, asyncAction, mainAction, successAction, getContextInfoFunc, failedAction, retryTimes, false, 3, 1000);
    }

    public <TAsyncResult extends AsyncTaskResult> void tryAsyncActionRecursively(
            String asyncActionName,
            Func<CompletableFuture<TAsyncResult>> asyncAction,
            Action1<Integer> mainAction,
            Action1<TAsyncResult> successAction,
            Func<String> getContextInfoFunc,
            Action1<String> failedAction,
            int retryTimes, boolean retryWhenFailed) {
        tryAsyncActionRecursively(asyncActionName, asyncAction, mainAction, successAction, getContextInfoFunc, failedAction, retryTimes, retryWhenFailed, 3, 1000);
    }

    public <TAsyncResult extends AsyncTaskResult> void tryAsyncActionRecursively(
            String asyncActionName,
            Func<CompletableFuture<TAsyncResult>> asyncAction,
            Action1<Integer> mainAction,
            Action1<TAsyncResult> successAction,
            Func<String> getContextInfoFunc,
            Action1<String> failedAction,
            int retryTimes,
            boolean retryWhenFailed,
            int maxRetryTimes,
            int retryInterval) {
        try {
            asyncAction.apply().handleAsync((result, ex) -> {
                taskContinueAction(result, ex, new TaskExecutionContext(asyncActionName, mainAction, successAction, getContextInfoFunc, failedAction, retryTimes,
                        retryWhenFailed, maxRetryTimes, retryInterval));

                return null;
            });
        } catch (IORuntimeException ex) {
            logger.error(String.format("IOException raised when executing async task '%s', contextInfo:%s, current retryTimes:%d, try to execute the async task again.", asyncActionName, getContextInfo(getContextInfoFunc), retryTimes), ex);
            executeRetryAction(asyncActionName, getContextInfoFunc, mainAction, retryTimes, maxRetryTimes, retryInterval);
        } catch (Exception ex) {
            logger.error(String.format("Unknown exception raised when executing async task '%s', contextInfo:%s, current retryTimes:%d", asyncActionName, getContextInfo(getContextInfoFunc), retryTimes), ex);
            if (retryWhenFailed) {
                executeRetryAction(asyncActionName, getContextInfoFunc, mainAction, retryTimes, maxRetryTimes, retryInterval);
            } else {
                executeFailedAction(asyncActionName, getContextInfoFunc, failedAction, ex.getMessage());
            }
        }
    }

    private String getContextInfo(Func<String> func) {
        try {
            return func.apply();
        } catch (Exception ex) {
            logger.error("Failed to execute the getContextInfoFunc.", ex);
            return null;
        }
    }

    private void executeFailedAction(String asyncActionName, Func<String> getContextInfoFunc, Action1<String> failedAction, String errorMessage) {
        try {
            if (failedAction != null) {
                failedAction.apply(errorMessage);
            }
        } catch (Exception ex) {
            logger.error(String.format("Failed to execute the failedAction of asyncAction:%s, contextInfo:%s", asyncActionName, getContextInfo(getContextInfoFunc)), ex);
        }
    }

    private void executeRetryAction(String asyncActionName, Func<String> getContextInfoFunc, Action1<Integer> mainAction, int currentRetryTimes, int maxRetryTimes, int retryInterval) {
        try {
            if (currentRetryTimes >= maxRetryTimes) {
                DelayedTask.startDelayedTask(Duration.ofMillis(retryInterval), () -> mainAction.apply(currentRetryTimes + 1));
                //Task.Factory.StartDelayedTask(retryInterval, () -> mainAction.apply(currentRetryTimes + 1));
            } else {
                mainAction.apply(currentRetryTimes + 1);
            }
        } catch (Exception ex) {
            logger.error(String.format("Failed to execute the retryAction, asyncActionName:%s, contextInfo:%s", asyncActionName, getContextInfo(getContextInfoFunc)), ex);
        }
    }

    private void processTaskException(String asyncActionName, Func<String> getContextInfoFunc, Action1<Integer> mainAction, Action1<String> failedAction, Throwable exception, int currentRetryTimes, int maxRetryTimes, int retryInterval, boolean retryWhenFailed) {
        if (exception instanceof IORuntimeException) {
            logger.error(String.format("Async task '%s' has io exception, contextInfo:%s, current retryTimes:%d, try to run the async task again.", asyncActionName, getContextInfo(getContextInfoFunc), currentRetryTimes), exception);
            executeRetryAction(asyncActionName, getContextInfoFunc, mainAction, currentRetryTimes, maxRetryTimes, retryInterval);
        } else {
            logger.error(String.format("Async task '%s' has unknown exception, contextInfo:%s, current retryTimes:%d", asyncActionName, getContextInfo(getContextInfoFunc), currentRetryTimes), exception);
            if (retryWhenFailed) {
                executeRetryAction(asyncActionName, getContextInfoFunc, mainAction, currentRetryTimes, maxRetryTimes, retryInterval);
            } else {
                executeFailedAction(asyncActionName, getContextInfoFunc, failedAction, exception.getMessage());
            }
        }
    }

    private <TAsyncResult extends AsyncTaskResult> void taskContinueAction(TAsyncResult result, Throwable ex, TaskExecutionContext context) {
        try {
            if (ex != null) {
                if(ex instanceof CancellationException) {
                    logger.error("Async task '{}' was cancelled, contextInfo:{}, current retryTimes:{}.",
                            context.getAsyncActionName(),
                            getContextInfo(context.getContextInfoFunc()),
                            context.getRetryTimes());
                    executeFailedAction(
                            context.getAsyncActionName(),
                            context.getContextInfoFunc(),
                            context.getFailedAction(),
                            String.format("Async task '%s' was cancelled.", context.getAsyncActionName()));
                    return;
                }

                processTaskException(
                        context.getAsyncActionName(),
                        context.getContextInfoFunc(),
                        context.getMainAction(),
                        context.getFailedAction(),
                        ex,
                        context.getRetryTimes(),
                        context.getMaxRetryTimes(),
                        context.getRetryInterval(),
                        context.isRetryWhenFailed());
                return;
            }

            if (result == null) {
                logger.error("Async task '{}' result is null, contextInfo:{}, current retryTimes:{}",
                        context.getAsyncActionName(),
                        getContextInfo(context.getContextInfoFunc()),
                        context.getRetryTimes());
                if (context.isRetryWhenFailed()) {
                    executeRetryAction(
                            context.getAsyncActionName(),
                            context.getContextInfoFunc(),
                            context.getMainAction(),
                            context.getRetryTimes(),
                            context.getMaxRetryTimes(),
                            context.getRetryInterval());
                } else {
                    executeFailedAction(
                            context.getAsyncActionName(),
                            context.getContextInfoFunc(),
                            context.getFailedAction(),
                            String.format("Async task '%s' result is null.", context.getAsyncActionName()));
                }
                return;
            }
            if (result.getStatus().equals(AsyncTaskStatus.Success)) {

                if (context.getSuccessAction() != null) {
                    context.getSuccessAction().apply(result);
                }
            } else if (result.getStatus().equals(AsyncTaskStatus.IOException)) {
                logger.error("Async task '{}' result status is io exception, contextInfo:{}, current retryTimes:{}, errorMsg:{}, try to run the async task again.",
                        context.getAsyncActionName(),
                        getContextInfo(context.getContextInfoFunc()),
                        context.getRetryTimes(),
                        result.getErrorMessage());
                executeRetryAction(
                        context.getAsyncActionName(),
                        context.getContextInfoFunc(),
                        context.getMainAction(),
                        context.getRetryTimes(),
                        context.getMaxRetryTimes(),
                        context.getRetryInterval());
            } else if (result.getStatus().equals(AsyncTaskStatus.Failed)) {
                logger.error("Async task '{}' failed, contextInfo:{}, current retryTimes:{}, errorMsg:{}",
                        context.getAsyncActionName(),
                        getContextInfo(context.getContextInfoFunc()),
                        context.getRetryTimes(),
                        result.getErrorMessage());
                if (context.isRetryWhenFailed()) {
                    executeRetryAction(
                            context.getAsyncActionName(),
                            context.getContextInfoFunc(),
                            context.getMainAction(),
                            context.getRetryTimes(),
                            context.getMaxRetryTimes(),
                            context.getRetryInterval());
                } else {
                    executeFailedAction(
                            context.getAsyncActionName(),
                            context.getContextInfoFunc(),
                            context.getFailedAction(),
                            result.getErrorMessage());
                }
            }
        } catch (Exception e) {
            logger.error(String.format("Failed to execute the taskContinueAction, asyncActionName:%s, contextInfo:%s",
                    context.getAsyncActionName(),
                    getContextInfo(context.getContextInfoFunc())), e);
        }
    }

    public void tryIOAction(Action action, String actionName) {
        Ensure.notNull(action, "action");
        Ensure.notNull(actionName, "actionName");
        try {
            action.apply();
        } catch (IORuntimeException e) {
            throw e;
        } catch (Exception ex) {
            throw new IORuntimeException(String.format("%s failed.", actionName), ex);
        }
    }

    public <T> CompletableFuture<T> tryIOActionAsync(Func<CompletableFuture<T>> action, String actionName) {
        Ensure.notNull(action, "action");
        Ensure.notNull(actionName, "actionName");
        try {
            return action.apply();
        } catch (IORuntimeException e) {
            throw e;
        } catch (Exception ex) {
            throw new IORuntimeException(String.format("%s failed.", actionName), ex);
        }
    }

    public <T> T tryIOFunc(Func<T> func, String funcName) {
        Ensure.notNull(func, "func");
        Ensure.notNull(funcName, "funcName");
        try {
            return func.apply();
        } catch (IORuntimeException e) {
            throw e;
        } catch (Exception ex) {
            throw new IORuntimeException(String.format("%s failed.", funcName), ex);
        }
    }

    public <T> CompletableFuture<T> tryIOFuncAsync(Func<CompletableFuture<T>> func, String funcName) {
        Ensure.notNull(func, "func");
        Ensure.notNull(funcName, "funcName");
        try {
            return func.apply();
        } catch (IORuntimeException e) {
            throw e;
        } catch (Exception ex) {
            throw new IORuntimeException(String.format("%s failed.", funcName), ex);
        }
    }

    private void tryIOActionRecursivelyInternal(String actionName, Func<String> getContextInfo, Action3<String, Func<String>, Integer> action, int retryTimes, int maxRetryTimes, boolean continueRetryWhenRetryFailed, int retryInterval) {
        try{
            action.apply(actionName,getContextInfo, retryTimes);
        } catch(Exception ex){
            String contextInfo;
            try {
                contextInfo = getContextInfo.apply();
            } catch (Exception e) {
                throw new WrappedRuntimeException(e);
            }
            if(ex instanceof IOException || (ex instanceof WrappedRuntimeException && ((WrappedRuntimeException)ex).getException() instanceof IOException)){
                String errorMessage = String.format("IOException raised when executing action '%s', currentRetryTimes:%d, maxRetryTimes:%d, contextInfo:%s", actionName, retryTimes, maxRetryTimes, contextInfo);
                logger.error(errorMessage, ex);
                if (retryTimes >= maxRetryTimes) {
                    if (!continueRetryWhenRetryFailed) {
                        throw new WrappedRuntimeException(ex);
                    } else {
                        try {
                            Thread.sleep(retryInterval);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                retryTimes++;
                tryIOActionRecursivelyInternal(actionName, getContextInfo, action, retryTimes, maxRetryTimes, continueRetryWhenRetryFailed, retryInterval);
            } else {
                String errorMessage = String.format("Unknown exception raised when executing action '%s', currentRetryTimes:%d, maxRetryTimes:%d, contextInfo:%s", actionName, retryTimes, maxRetryTimes, contextInfo);
                logger.error(errorMessage, ex);
                throw new WrappedRuntimeException(ex);
            }
        }
    }

    private <T> T tryIOFuncRecursivelyInternal(String funcName, Func<String> getContextInfo, Func3<String, Func<String>, Integer, T> func, int retryTimes, int maxRetryTimes, boolean continueRetryWhenRetryFailed, int retryInterval) {
        try{
            return func.apply(funcName,getContextInfo,retryTimes);
        } catch(Exception ex){
            String contextInfo;
            try {
                contextInfo = getContextInfo.apply();
            } catch (Exception e) {
                throw new WrappedRuntimeException(e);
            }

            if(ex instanceof IOException || (ex instanceof WrappedRuntimeException && ((WrappedRuntimeException)ex).getException() instanceof IOException)){
                String errorMessage = String.format("IOException raised when executing func '%s', currentRetryTimes:%d, maxRetryTimes:%d, contextInfo:%s", funcName, retryTimes, maxRetryTimes, contextInfo);
                logger.error(errorMessage, ex);
                if (retryTimes >= maxRetryTimes) {
                    if (!continueRetryWhenRetryFailed) {
                        throw new WrappedRuntimeException(ex);
                    } else {
                        try {
                            Thread.sleep(retryInterval);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                retryTimes++;
                return tryIOFuncRecursivelyInternal(funcName, getContextInfo, func, retryTimes, maxRetryTimes, continueRetryWhenRetryFailed, retryInterval);
            } else {
                String errorMessage = String.format("Unknown exception raised when executing func '%s', currentRetryTimes:%d, maxRetryTimes:%d, contextInfo:%s", funcName, retryTimes, maxRetryTimes, contextInfo);
                logger.error(errorMessage, ex);
                throw new WrappedRuntimeException(ex);
            }
        }
    }

    class TaskExecutionContext<TAsyncResult> {
        private String asyncActionName;
        private Action1<Integer> mainAction;
        private Action1<TAsyncResult> successAction;
        private Func<String> contextInfoFunc;
        private Action1<String> failedAction;
        private int retryTimes;
        private boolean retryWhenFailed;
        private int maxRetryTimes;
        private int retryInterval;

        public TaskExecutionContext(String asyncActionName, Action1<Integer> mainAction, Action1<TAsyncResult> successAction, Func<String> contextInfoFunc,
                                    Action1<String> failedAction, int retryTimes, boolean retryWhenFailed, int maxRetryTimes, int retryInterval) {
            super();
            this.asyncActionName = asyncActionName;
            this.mainAction = mainAction;
            this.successAction = successAction;
            this.contextInfoFunc = contextInfoFunc;
            this.failedAction = failedAction;
            this.retryTimes = retryTimes;
            this.retryWhenFailed = retryWhenFailed;
            this.maxRetryTimes = maxRetryTimes;
            this.retryInterval = retryInterval;
        }

        public String getAsyncActionName() {
            return asyncActionName;
        }

        public Action1<Integer> getMainAction() {
            return mainAction;
        }

        public Action1<TAsyncResult> getSuccessAction() {
            return successAction;
        }

        public Func<String> getContextInfoFunc() {
            return contextInfoFunc;
        }

        public Action1<String> getFailedAction() {
            return failedAction;
        }

        public int getRetryTimes() {
            return retryTimes;
        }

        public boolean isRetryWhenFailed() {
            return retryWhenFailed;
        }

        public int getMaxRetryTimes() {
            return maxRetryTimes;
        }

        public int getRetryInterval() {
            return retryInterval;
        }
    }
}
