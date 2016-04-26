package com.qianzhui.enode.commanding.impl;

import com.qianzhui.enode.ENode;
import com.qianzhui.enode.commanding.CommandAddResult;
import com.qianzhui.enode.commanding.HandledCommand;
import com.qianzhui.enode.commanding.ICommandStore;
import com.qianzhui.enode.common.container.ObjectContainer;
import com.qianzhui.enode.common.io.AsyncTaskResult;
import com.qianzhui.enode.common.io.AsyncTaskStatus;
import com.qianzhui.enode.common.logging.ILogger;
import com.qianzhui.enode.common.logging.ILoggerFactory;
import com.qianzhui.enode.common.serializing.IJsonSerializer;
import com.qianzhui.enode.common.utilities.Ensure;
import com.qianzhui.enode.configurations.DefaultDBConfigurationSetting;
import com.qianzhui.enode.configurations.OptionSetting;
import com.qianzhui.enode.infrastructure.IApplicationMessage;
import com.qianzhui.enode.infrastructure.ITypeNameProvider;
import com.qianzhui.enode.infrastructure.WrappedRuntimeException;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

/**
 * Created by junbo_xu on 2016/3/20.
 */
public class MysqlCommandStore implements ICommandStore {

    private String _tableName;
    private String _commandIndexName;
    private IJsonSerializer _jsonSerializer;
    private ITypeNameProvider _typeNameProvider;
    private QueryRunner _queryRunner;
    private ILogger _logger;

    public MysqlCommandStore(DataSource ds, OptionSetting optionSetting) {
        Ensure.notNull(ds, "ds");

        if (optionSetting != null) {
            _tableName = optionSetting.getOptionValue("TableName");
            _commandIndexName = optionSetting.getOptionValue("CommandIndexName");
        } else {
            DefaultDBConfigurationSetting defaultDBConfigurationSetting = ENode.getInstance().getSetting().getDefaultDBConfigurationSetting();
            _tableName = defaultDBConfigurationSetting.getCommandTableName();
            _commandIndexName = defaultDBConfigurationSetting.getCommandTableCommandIdUniqueIndexName();
        }

        Ensure.notNull(_tableName, "_tableName");
        Ensure.notNull(_commandIndexName, "_commandIndexName");

        _jsonSerializer = ObjectContainer.resolve(IJsonSerializer.class);
        _typeNameProvider = ObjectContainer.resolve(ITypeNameProvider.class);
        _queryRunner = new QueryRunner(ds);
        _logger = ObjectContainer.resolve(ILoggerFactory.class).create(getClass());
    }

    public CompletableFuture<AsyncTaskResult<CommandAddResult>> addAsync(HandledCommand handledCommand) {
        CommandRecord record = convertTo(handledCommand);

        return CompletableFuture.supplyAsync(() -> {
            try {
                _queryRunner.update("INSERT INTO Command(CommandId,AggregateRootId,MessagePayload,MessageTypeName,CreatedOn) VALUES(?,?,?,?,?)",
                        record.getCommandId(),
                        record.getAggregateRootId(),
                        record.getMessagePayload(),
                        record.getMessageTypeName(),
                        new Timestamp(record.getCreatedOn().getTime()));

                return new AsyncTaskResult<>(AsyncTaskStatus.Success, null, CommandAddResult.Success);
            } catch (SQLException ex) {
                if (ex.getErrorCode() == 1062 && ex.getMessage().contains(_commandIndexName)) {
                    return new AsyncTaskResult<>(AsyncTaskStatus.Success, null, CommandAddResult.DuplicateCommand);
                }
                _logger.error(String.format("Add handled command has sql exception, handledCommand: %s", handledCommand), ex);
                throw new WrappedRuntimeException(ex);
            } catch (Exception ex) {
                _logger.error(String.format("Add handled command has unkown exception, handledCommand: %s", handledCommand), ex);
                throw new WrappedRuntimeException(ex);
            }
        });
    }

    public CompletableFuture<AsyncTaskResult<HandledCommand>> getAsync(String commandId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                CommandRecord commandRecord = _queryRunner.query(String.format("select * from %s where CommandId=?", _tableName), new BeanHandler<>(CommandRecord.class), commandId);

                HandledCommand handledCommand = commandRecord == null ? null : convertFrom(commandRecord);

                return new AsyncTaskResult<>(AsyncTaskStatus.Success, handledCommand);
            } catch (SQLException ex) {
                _logger.error(String.format("Get handled command has sql exception, commandId: %s", commandId), ex);
                return new AsyncTaskResult<HandledCommand>(AsyncTaskStatus.IOException, ex.getMessage(), null);
            } catch (Exception ex) {
                _logger.error(String.format("Get handled command has unkown exception, commandId: %s", commandId), ex);
                return new AsyncTaskResult<HandledCommand>(AsyncTaskStatus.Failed, ex.getMessage(), null);
            }
        });
    }

    private CommandRecord convertTo(HandledCommand handledCommand) {
        return new CommandRecord(handledCommand.getCommandId(),
                handledCommand.getAggregateRootId(),
                handledCommand.getMessage() != null ? _jsonSerializer.serialize(handledCommand.getMessage()) : null,
                handledCommand.getMessage() != null ? _typeNameProvider.getTypeName(handledCommand.getMessage().getClass()) : null,
                new Date());
    }

    private HandledCommand convertFrom(CommandRecord record) {
        IApplicationMessage message = null;

        if (record.getMessageTypeName() == null || record.getMessageTypeName().trim().equals("")) {
            Class messageType = _typeNameProvider.getType(record.getMessageTypeName());
            message = (IApplicationMessage) _jsonSerializer.deserialize(record.getMessagePayload(), messageType);
        }

        return new HandledCommand(record.getCommandId(), record.getAggregateRootId(), message);
    }
}
