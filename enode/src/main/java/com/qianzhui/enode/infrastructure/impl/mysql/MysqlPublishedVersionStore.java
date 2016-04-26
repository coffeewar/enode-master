package com.qianzhui.enode.infrastructure.impl.mysql;

import com.qianzhui.enode.ENode;
import com.qianzhui.enode.common.container.ObjectContainer;
import com.qianzhui.enode.common.io.AsyncTaskResult;
import com.qianzhui.enode.common.io.AsyncTaskStatus;
import com.qianzhui.enode.common.logging.ILogger;
import com.qianzhui.enode.common.logging.ILoggerFactory;
import com.qianzhui.enode.common.utilities.Ensure;
import com.qianzhui.enode.configurations.DefaultDBConfigurationSetting;
import com.qianzhui.enode.configurations.OptionSetting;
import com.qianzhui.enode.infrastructure.IPublishedVersionStore;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

/**
 * Created by junbo_xu on 2016/4/3.
 */
public class MysqlPublishedVersionStore implements IPublishedVersionStore {

    private final DataSource _ds;
    private final QueryRunner _queryRunner;
    private final String _tableName;
    private final String _uniqueIndexName;
    private final ILogger _logger;

    public MysqlPublishedVersionStore(DataSource ds, OptionSetting optionSetting) {
        Ensure.notNull(ds, "ds");

        if(optionSetting != null) {
            _tableName = optionSetting.getOptionValue("TableName");
            _uniqueIndexName = optionSetting.getOptionValue("UniqueIndexName");
        } else {
            DefaultDBConfigurationSetting setting = ENode.getInstance().getSetting().getDefaultDBConfigurationSetting();
            _tableName = setting.getPublishedVersionTableName();
            _uniqueIndexName = setting.getPublishedVersionUniqueIndexName();
        }

        Ensure.notNull(_tableName, "_tableName");
        Ensure.notNull(_uniqueIndexName, "_uniqueIndexName");

        _ds = ds;
        _queryRunner = new QueryRunner(ds);

        _logger = ObjectContainer.resolve(ILoggerFactory.class).create(getClass());
    }

    public CompletableFuture<AsyncTaskResult> updatePublishedVersionAsync(String processorName, String aggregateRootTypeName, String aggregateRootId, int publishedVersion) {
        if (publishedVersion == 1) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    _queryRunner.update(String.format("INSERT INTO %s(ProcessorName,AggregateRootTypeName,AggregateRootId,Version,CreatedOn) VALUES(?,?,?,?,?)", _tableName),
                            processorName,
                            aggregateRootTypeName,
                            aggregateRootId,
                            1,
                            new Timestamp(new Date().getTime()));

                    return AsyncTaskResult.Success;
                } catch (SQLException ex) {
                    if(ex.getErrorCode() == 1062 && ex.getMessage().contains(_uniqueIndexName)){
                        return AsyncTaskResult.Success;
                    }
                    _logger.error("Insert aggregate published version has sql exception.", ex);
                    return new AsyncTaskResult(AsyncTaskStatus.IOException, ex.getMessage());
                } catch (Exception ex) {
                    _logger.error("Insert aggregate published version has unknown exception.", ex);
                    return new AsyncTaskResult(AsyncTaskStatus.Failed, ex.getMessage());
                }
            });
        } else {
            return CompletableFuture.supplyAsync(() -> {
                try {

                    _queryRunner.update(String.format("UPDATE %s set Version=?,CreatedOn=? WHERE ProcessorName=? and AggregateRootId=? and Version=?", _tableName),
                            publishedVersion,
                            new Timestamp(new Date().getTime()),
                            processorName,
                            aggregateRootId,
                            publishedVersion - 1);

                    return AsyncTaskResult.Success;
                } catch (SQLException ex) {
                    _logger.error("Update aggregate published version has sql exception.", ex);
                    return new AsyncTaskResult(AsyncTaskStatus.IOException, ex.getMessage());
                } catch (Exception ex) {
                    _logger.error("Update aggregate published version has unknown exception.", ex);
                    return new AsyncTaskResult(AsyncTaskStatus.Failed, ex.getMessage());
                }
            });
        }
    }

    public CompletableFuture<AsyncTaskResult<Integer>> getPublishedVersionAsync(String processorName, String aggregateRootTypeName, String aggregateRootId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Object resultObj = _queryRunner.query(String.format("SELECT Version FROM %s WHERE ProcessorName=? AND AggregateRootId=?", _tableName),
                        new ScalarHandler<>(), processorName, aggregateRootId);

                int result = (resultObj == null ? 0 : ((Number) resultObj).intValue());

                return new AsyncTaskResult<>(AsyncTaskStatus.Success, result);
            } catch (SQLException ex) {
                _logger.error("Get aggregate published version has sql exception.", ex);
                return new AsyncTaskResult<>(AsyncTaskStatus.IOException, ex.getMessage());
            } catch (Exception ex) {
                _logger.error("Get aggregate published version has unknown exception.", ex);
                return new AsyncTaskResult<>(AsyncTaskStatus.Failed, ex.getMessage());
            }
        });
    }
}
