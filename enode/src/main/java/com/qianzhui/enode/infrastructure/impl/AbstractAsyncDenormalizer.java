package com.qianzhui.enode.infrastructure.impl;

import com.qianzhui.enode.common.io.AsyncTaskResult;
import com.qianzhui.enode.common.io.IORuntimeException;
import org.apache.commons.dbutils.QueryRunner;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Created by junbo_xu on 2016/9/22.
 */
public abstract class AbstractAsyncDenormalizer {
    protected final DataSource ds;
    protected final QueryRunner _queryRunner;

    public AbstractAsyncDenormalizer(DataSource ds) {
        this.ds = ds;
        this._queryRunner = new QueryRunner(ds);
    }

    public CompletableFuture<AsyncTaskResult> tryExecuteAsync(Function<QueryRunner, AsyncTaskResult> executer) {
        return CompletableFuture.supplyAsync(() ->
                executer.apply(_queryRunner)
        );
    }

    public CompletableFuture<AsyncTaskResult> tryInsertRecordAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this._queryRunner.update(sql, params);
                return AsyncTaskResult.Success;
            } catch (SQLException ex) {
                if (ex.getErrorCode() == 1062) { //主键冲突，忽略即可；出现这种情况，是因为同一个消息的重复处理
                    return AsyncTaskResult.Success;
                }
                throw new IORuntimeException("Insert record failed.", ex);
            }
        });
    }

    public CompletableFuture<AsyncTaskResult> tryUpdateRecordAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this._queryRunner.update(sql, params);
                return AsyncTaskResult.Success;
            } catch (SQLException ex) {
                throw new IORuntimeException("Update record failed.", ex);
            }
        });
    }

    public CompletableFuture<AsyncTaskResult> tryTransactionAsync(QueryRunnerExecuter... executers) {
        return CompletableFuture.supplyAsync(() -> {

            try (Connection connection = ds.getConnection()) {
                connection.setAutoCommit(false);

                try {
                    for (QueryRunnerExecuter executer : executers) {
                        executer.update(_queryRunner, connection);
                    }

                    connection.commit();

                    return AsyncTaskResult.Success;
                } catch (SQLException ex) {
                    connection.rollback();
                    throw new IORuntimeException(ex);
                }
            } catch (SQLException ex) {
                throw new IORuntimeException(ex);
            }
        });
    }

    protected QueryRunnerExecuter insertStatement(String sql, Object... params) {
        return new InsertExecuter(sql, params);
    }

    protected QueryRunnerExecuter updateStatement(String sql, Object... params) {
        return new UpdateExecuter(sql, params);
    }

    protected QueryRunnerExecuter batchStatement(String sql, Object[][] params) {
        return new BatchExecuter(sql, params);
    }

    protected interface QueryRunnerExecuter {
        int update(QueryRunner queryRunner, Connection connection) throws SQLException;
    }

    static abstract class AbstractQueryRunnerExecuter implements QueryRunnerExecuter {
        private String sql;
        private Object[] params;

        public AbstractQueryRunnerExecuter(String sql, Object[] params) {
            this.sql = sql;
            this.params = params;
        }

        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

        public Object[] getParams() {
            return params;
        }

        public void setParams(Object[] params) {
            this.params = params;
        }

        @Override
        public int update(QueryRunner queryRunner, Connection connection) throws SQLException {
            return queryRunner.update(connection, sql, params);
        }
    }

    static class UpdateExecuter extends AbstractQueryRunnerExecuter {

        public UpdateExecuter(String sql, Object[] params) {
            super(sql, params);
        }
    }

    static class InsertExecuter extends AbstractQueryRunnerExecuter {
        public InsertExecuter(String sql, Object[] params) {
            super(sql, params);
        }

        @Override
        public int update(QueryRunner queryRunner, Connection connection) throws SQLException {
            try {
                return queryRunner.update(connection, getSql(), getParams());
            } catch (SQLException ex) {
                if (ex.getErrorCode() == 1062) { //主键冲突，忽略即可；出现这种情况，是因为同一个消息的重复处理
                    //ignore
                    return 1;
                }
                throw new IORuntimeException("Insert record failed.", ex);
            }
        }
    }

    static class BatchExecuter implements QueryRunnerExecuter {

        private String sql;
        private Object[][] params;

        public BatchExecuter(String sql, Object[][] params) {
            this.sql = sql;
            this.params = params;
        }

        @Override
        public int update(QueryRunner queryRunner, Connection connection) throws SQLException {
            queryRunner.batch(sql, params);
            return 0;
        }
    }
}
