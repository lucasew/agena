package com.biglucas.agena.db.wrapper;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AndroidPreparedStatement implements PreparedStatement {
    private final SQLiteDatabase db;
    private final String sql;
    private final List<Object> bindArgs = new ArrayList<>();

    public AndroidPreparedStatement(SQLiteDatabase db, String sql) {
        this.db = db;
        this.sql = sql;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        String[] args = new String[bindArgs.size()];
        for (int i = 0; i < bindArgs.size(); i++) {
            Object arg = bindArgs.get(i);
            args[i] = arg == null ? null : String.valueOf(arg);
        }
        Cursor cursor = db.rawQuery(sql, args);
        return new AndroidResultSet(cursor);
    }

    @Override
    public boolean execute() throws SQLException {
        // For inserts/updates/deletes in the generated code
        // The generated code uses execute() for things that might return results or not,
        // but for "exec" queries it usually expects update counts or just success.
        // We'll emulate simple execution here.

        // If it's a SELECT, we should use rawQuery, but execute() is generic.
        // If it starts with SELECT, assume query.
        String trimmed = sql.trim().toUpperCase();
        if (trimmed.startsWith("SELECT")) {
             executeQuery(); // But execute() returns boolean
             return true; // Result set available
        } else {
             executeUpdate();
             return false; // No result set
        }
    }

    @Override
    public int executeUpdate() throws SQLException {
        Object[] args = bindArgs.toArray();
        db.execSQL(sql, args);
        return 1; // Simplified
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        db.execSQL(sql);
        return 1; // Simplified
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        Cursor cursor = db.rawQuery(sql, null);
        return new AndroidResultSet(cursor);
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        String trimmed = sql.trim().toUpperCase();
        if (trimmed.startsWith("SELECT")) {
             db.rawQuery(sql, null); // Just execute, result ignored for now as return is boolean
             return true;
        } else {
             db.execSQL(sql);
             return false;
        }
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        ensureCapacity(parameterIndex);
        bindArgs.set(parameterIndex - 1, x);
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        ensureCapacity(parameterIndex);
        bindArgs.set(parameterIndex - 1, x);
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        ensureCapacity(parameterIndex);
        bindArgs.set(parameterIndex - 1, x);
    }

    private void ensureCapacity(int index) {
        while (bindArgs.size() < index) {
            bindArgs.add(null);
        }
    }

    // --- Stubs ---

    @Override public void setNull(int parameterIndex, int sqlType) throws SQLException { ensureCapacity(parameterIndex); bindArgs.set(parameterIndex - 1, null); }
    @Override public void setBoolean(int parameterIndex, boolean x) throws SQLException { ensureCapacity(parameterIndex); bindArgs.set(parameterIndex - 1, x ? 1 : 0); }
    @Override public void setByte(int parameterIndex, byte x) throws SQLException { ensureCapacity(parameterIndex); bindArgs.set(parameterIndex - 1, x); }
    @Override public void setShort(int parameterIndex, short x) throws SQLException { ensureCapacity(parameterIndex); bindArgs.set(parameterIndex - 1, x); }
    @Override public void setFloat(int parameterIndex, float x) throws SQLException { ensureCapacity(parameterIndex); bindArgs.set(parameterIndex - 1, x); }
    @Override public void setDouble(int parameterIndex, double x) throws SQLException { ensureCapacity(parameterIndex); bindArgs.set(parameterIndex - 1, x); }
    @Override public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException { ensureCapacity(parameterIndex); bindArgs.set(parameterIndex - 1, x); }
    @Override public void setBytes(int parameterIndex, byte[] x) throws SQLException { ensureCapacity(parameterIndex); bindArgs.set(parameterIndex - 1, x); }
    @Override public void setDate(int parameterIndex, Date x) throws SQLException { ensureCapacity(parameterIndex); bindArgs.set(parameterIndex - 1, x); }
    @Override public void setTime(int parameterIndex, Time x) throws SQLException { ensureCapacity(parameterIndex); bindArgs.set(parameterIndex - 1, x); }
    @Override public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException { ensureCapacity(parameterIndex); bindArgs.set(parameterIndex - 1, x); }
    @Override public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException { }
    @Override public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException { }
    @Override public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException { }
    @Override public void clearParameters() throws SQLException { bindArgs.clear(); }
    @Override public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException { ensureCapacity(parameterIndex); bindArgs.set(parameterIndex - 1, x); }
    @Override public void setObject(int parameterIndex, Object x) throws SQLException { ensureCapacity(parameterIndex); bindArgs.set(parameterIndex - 1, x); }
    @Override public void addBatch() throws SQLException { }
    @Override public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException { }
    @Override public void setRef(int parameterIndex, Ref x) throws SQLException { }
    @Override public void setBlob(int parameterIndex, Blob x) throws SQLException { }
    @Override public void setClob(int parameterIndex, Clob x) throws SQLException { }
    @Override public void setArray(int parameterIndex, Array x) throws SQLException { }
    @Override public ResultSetMetaData getMetaData() throws SQLException { return null; }
    @Override public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException { }
    @Override public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException { }
    @Override public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException { }
    @Override public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException { }
    @Override public void setURL(int parameterIndex, URL x) throws SQLException { }
    @Override public ParameterMetaData getParameterMetaData() throws SQLException { return null; }
    @Override public void setRowId(int parameterIndex, RowId x) throws SQLException { }
    @Override public void setNString(int parameterIndex, String value) throws SQLException { }
    @Override public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException { }
    @Override public void setNClob(int parameterIndex, NClob value) throws SQLException { }
    @Override public void setClob(int parameterIndex, Reader reader, long length) throws SQLException { }
    @Override public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException { }
    @Override public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException { }
    @Override public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException { }
    @Override public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException { }
    @Override public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException { }
    @Override public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException { }
    @Override public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException { }
    @Override public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException { }
    @Override public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException { }
    @Override public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException { }
    @Override public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException { }
    @Override public void setClob(int parameterIndex, Reader reader) throws SQLException { }
    @Override public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException { }
    @Override public void setNClob(int parameterIndex, Reader reader) throws SQLException { }

    @Override public void close() throws SQLException { }
    @Override public int getMaxFieldSize() throws SQLException { return 0; }
    @Override public void setMaxFieldSize(int max) throws SQLException { }
    @Override public int getMaxRows() throws SQLException { return 0; }
    @Override public void setMaxRows(int max) throws SQLException { }
    @Override public void setEscapeProcessing(boolean enable) throws SQLException { }
    @Override public int getQueryTimeout() throws SQLException { return 0; }
    @Override public void setQueryTimeout(int seconds) throws SQLException { }
    @Override public void cancel() throws SQLException { }
    @Override public SQLWarning getWarnings() throws SQLException { return null; }
    @Override public void clearWarnings() throws SQLException { }
    @Override public void setCursorName(String name) throws SQLException { }
    @Override public ResultSet getResultSet() throws SQLException { return null; }
    @Override public int getUpdateCount() throws SQLException { return -1; }
    @Override public boolean getMoreResults() throws SQLException { return false; }
    @Override public void setFetchDirection(int direction) throws SQLException { }
    @Override public int getFetchDirection() throws SQLException { return 0; }
    @Override public void setFetchSize(int rows) throws SQLException { }
    @Override public int getFetchSize() throws SQLException { return 0; }
    @Override public int getResultSetConcurrency() throws SQLException { return 0; }
    @Override public int getResultSetType() throws SQLException { return 0; }
    @Override public void addBatch(String sql) throws SQLException { }
    @Override public void clearBatch() throws SQLException { }
    @Override public int[] executeBatch() throws SQLException { return new int[0]; }
    @Override public Connection getConnection() throws SQLException { return null; }
    @Override public boolean getMoreResults(int current) throws SQLException { return false; }
    @Override public ResultSet getGeneratedKeys() throws SQLException { return null; }
    @Override public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException { return 0; }
    @Override public int executeUpdate(String sql, int[] columnIndexes) throws SQLException { return 0; }
    @Override public int executeUpdate(String sql, String[] columnNames) throws SQLException { return 0; }
    @Override public boolean execute(String sql, int autoGeneratedKeys) throws SQLException { return false; }
    @Override public boolean execute(String sql, int[] columnIndexes) throws SQLException { return false; }
    @Override public boolean execute(String sql, String[] columnNames) throws SQLException { return false; }
    @Override public int getResultSetHoldability() throws SQLException { return 0; }
    @Override public boolean isClosed() throws SQLException { return false; }
    @Override public void setPoolable(boolean poolable) throws SQLException { }
    @Override public boolean isPoolable() throws SQLException { return false; }
    @Override public <T> T unwrap(Class<T> iface) throws SQLException { return null; }
    @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
}
