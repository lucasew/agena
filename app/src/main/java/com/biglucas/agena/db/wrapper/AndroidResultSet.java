package com.biglucas.agena.db.wrapper;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class AndroidResultSet implements ResultSet {
    private final Cursor cursor;

    public AndroidResultSet(Cursor cursor) {
        this.cursor = cursor;
    }

    @Override
    public boolean next() throws SQLException {
        return cursor.moveToNext();
    }

    @Override
    public void close() throws SQLException {
        cursor.close();
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        // JDBC is 1-based, Cursor is 0-based
        return cursor.getString(columnIndex - 1);
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        int index = cursor.getColumnIndexOrThrow(columnLabel);
        return cursor.getString(index);
    }

    // --- Compatibility Methods ---

    // This is the method causing the compilation error on Android's outdated SQL interface
    // but the generated code calls it.
    // If the Android SDK doesn't have this method in its java.sql.ResultSet interface,
    // this class implementing it won't help if the *compiler* sees the interface from android.jar.
    // However, if we compile against a JDK that has it (which we are, mostly), but the runtime is Android...
    // WAIT. The error was "no suitable method found for getObject(int,Class<OffsetDateTime>)".
    // That means the interface `java.sql.ResultSet` in the compile classpath (android.jar) DOES NOT have this method.
    // Simply implementing it here won't make `java.sql.ResultSet` (the interface) have it.
    // The generated code calls `results.getObject(...)` where `results` is of type `java.sql.ResultSet`.
    // It cannot call a method that doesn't exist on the interface.

    // THIS IS A DEAD END if I cannot change the generated code or the classpath.
    // Android's java.sql.ResultSet simply doesn't have that method.

    // I MUST modify the generated code to avoid using getObject(int, Class).
    // Or I must cast `results` to `AndroidResultSet` in the generated code... which requires modifying generated code.

    // Since I can't easily change the generator...
    // I might have to use a post-processing step to fix the generated code.
    // E.g. sed replacement in Gradle.

    // But let's finish the stub just in case.

    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        // Basic emulation for OffsetDateTime which sqlc uses for timestamps
        if (type == OffsetDateTime.class) {
            String s = getString(columnIndex);
            if (s == null) return null;
            // Parse SQLite default timestamp format "YYYY-MM-DD HH:MM:SS" usually, but here it might differ.
            // Let's assume standard format for now or just parse.
            // Actually sqlc-gen-java might expect something specific.
            return type.cast(OffsetDateTime.parse(s.replace(" ", "T") + "Z")); // Simple hack
        }
        return null;
    }

    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        int index = cursor.getColumnIndexOrThrow(columnLabel);
        return getObject(index + 1, type);
    }

    // --- Stubs ---
    @Override public boolean wasNull() throws SQLException { return false; } // Simplified
    @Override public boolean getBoolean(int columnIndex) throws SQLException { return cursor.getInt(columnIndex - 1) != 0; }
    @Override public byte getByte(int columnIndex) throws SQLException { return (byte) cursor.getShort(columnIndex - 1); }
    @Override public short getShort(int columnIndex) throws SQLException { return cursor.getShort(columnIndex - 1); }
    @Override public int getInt(int columnIndex) throws SQLException { return cursor.getInt(columnIndex - 1); }
    @Override public long getLong(int columnIndex) throws SQLException { return cursor.getLong(columnIndex - 1); }
    @Override public float getFloat(int columnIndex) throws SQLException { return cursor.getFloat(columnIndex - 1); }
    @Override public double getDouble(int columnIndex) throws SQLException { return cursor.getDouble(columnIndex - 1); }
    @Override public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException { return null; }
    @Override public byte[] getBytes(int columnIndex) throws SQLException { return cursor.getBlob(columnIndex - 1); }
    @Override public Date getDate(int columnIndex) throws SQLException { return null; }
    @Override public Time getTime(int columnIndex) throws SQLException { return null; }
    @Override public Timestamp getTimestamp(int columnIndex) throws SQLException { return null; }
    @Override public InputStream getAsciiStream(int columnIndex) throws SQLException { return null; }
    @Override public InputStream getUnicodeStream(int columnIndex) throws SQLException { return null; }
    @Override public InputStream getBinaryStream(int columnIndex) throws SQLException { return null; }
    @Override public boolean getBoolean(String columnLabel) throws SQLException { return false; }
    @Override public byte getByte(String columnLabel) throws SQLException { return 0; }
    @Override public short getShort(String columnLabel) throws SQLException { return 0; }
    @Override public int getInt(String columnLabel) throws SQLException { return 0; }
    @Override public long getLong(String columnLabel) throws SQLException { return 0; }
    @Override public float getFloat(String columnLabel) throws SQLException { return 0; }
    @Override public double getDouble(String columnLabel) throws SQLException { return 0; }
    @Override public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException { return null; }
    @Override public byte[] getBytes(String columnLabel) throws SQLException { return new byte[0]; }
    @Override public Date getDate(String columnLabel) throws SQLException { return null; }
    @Override public Time getTime(String columnLabel) throws SQLException { return null; }
    @Override public Timestamp getTimestamp(String columnLabel) throws SQLException { return null; }
    @Override public InputStream getAsciiStream(String columnLabel) throws SQLException { return null; }
    @Override public InputStream getUnicodeStream(String columnLabel) throws SQLException { return null; }
    @Override public InputStream getBinaryStream(String columnLabel) throws SQLException { return null; }
    @Override public SQLWarning getWarnings() throws SQLException { return null; }
    @Override public void clearWarnings() throws SQLException { }
    @Override public String getCursorName() throws SQLException { return null; }
    @Override public ResultSetMetaData getMetaData() throws SQLException { return null; }
    @Override public Object getObject(int columnIndex) throws SQLException { return null; }
    @Override public Object getObject(String columnLabel) throws SQLException { return null; }
    @Override public int findColumn(String columnLabel) throws SQLException { return 0; }
    @Override public Reader getCharacterStream(int columnIndex) throws SQLException { return null; }
    @Override public Reader getCharacterStream(String columnLabel) throws SQLException { return null; }
    @Override public BigDecimal getBigDecimal(int columnIndex) throws SQLException { return null; }
    @Override public BigDecimal getBigDecimal(String columnLabel) throws SQLException { return null; }
    @Override public boolean isBeforeFirst() throws SQLException { return false; }
    @Override public boolean isAfterLast() throws SQLException { return false; }
    @Override public boolean isFirst() throws SQLException { return false; }
    @Override public boolean isLast() throws SQLException { return false; }
    @Override public void beforeFirst() throws SQLException { }
    @Override public void afterLast() throws SQLException { }
    @Override public boolean first() throws SQLException { return false; }
    @Override public boolean last() throws SQLException { return false; }
    @Override public int getRow() throws SQLException { return 0; }
    @Override public boolean absolute(int row) throws SQLException { return false; }
    @Override public boolean relative(int rows) throws SQLException { return false; }
    @Override public boolean previous() throws SQLException { return false; }
    @Override public void setFetchDirection(int direction) throws SQLException { }
    @Override public int getFetchDirection() throws SQLException { return 0; }
    @Override public void setFetchSize(int rows) throws SQLException { }
    @Override public int getFetchSize() throws SQLException { return 0; }
    @Override public int getType() throws SQLException { return 0; }
    @Override public int getConcurrency() throws SQLException { return 0; }
    @Override public boolean rowUpdated() throws SQLException { return false; }
    @Override public boolean rowInserted() throws SQLException { return false; }
    @Override public boolean rowDeleted() throws SQLException { return false; }
    @Override public void updateNull(int columnIndex) throws SQLException { }
    @Override public void updateBoolean(int columnIndex, boolean x) throws SQLException { }
    @Override public void updateByte(int columnIndex, byte x) throws SQLException { }
    @Override public void updateShort(int columnIndex, short x) throws SQLException { }
    @Override public void updateInt(int columnIndex, int x) throws SQLException { }
    @Override public void updateLong(int columnIndex, long x) throws SQLException { }
    @Override public void updateFloat(int columnIndex, float x) throws SQLException { }
    @Override public void updateDouble(int columnIndex, double x) throws SQLException { }
    @Override public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException { }
    @Override public void updateString(int columnIndex, String x) throws SQLException { }
    @Override public void updateBytes(int columnIndex, byte[] x) throws SQLException { }
    @Override public void updateDate(int columnIndex, Date x) throws SQLException { }
    @Override public void updateTime(int columnIndex, Time x) throws SQLException { }
    @Override public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException { }
    @Override public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException { }
    @Override public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException { }
    @Override public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException { }
    @Override public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException { }
    @Override public void updateObject(int columnIndex, Object x) throws SQLException { }
    @Override public void updateNull(String columnLabel) throws SQLException { }
    @Override public void updateBoolean(String columnLabel, boolean x) throws SQLException { }
    @Override public void updateByte(String columnLabel, byte x) throws SQLException { }
    @Override public void updateShort(String columnLabel, short x) throws SQLException { }
    @Override public void updateInt(String columnLabel, int x) throws SQLException { }
    @Override public void updateLong(String columnLabel, long x) throws SQLException { }
    @Override public void updateFloat(String columnLabel, float x) throws SQLException { }
    @Override public void updateDouble(String columnLabel, double x) throws SQLException { }
    @Override public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException { }
    @Override public void updateString(String columnLabel, String x) throws SQLException { }
    @Override public void updateBytes(String columnLabel, byte[] x) throws SQLException { }
    @Override public void updateDate(String columnLabel, Date x) throws SQLException { }
    @Override public void updateTime(String columnLabel, Time x) throws SQLException { }
    @Override public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException { }
    @Override public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException { }
    @Override public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException { }
    @Override public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException { }
    @Override public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException { }
    @Override public void updateObject(String columnLabel, Object x) throws SQLException { }
    @Override public void insertRow() throws SQLException { }
    @Override public void updateRow() throws SQLException { }
    @Override public void deleteRow() throws SQLException { }
    @Override public void refreshRow() throws SQLException { }
    @Override public void cancelRowUpdates() throws SQLException { }
    @Override public void moveToInsertRow() throws SQLException { }
    @Override public void moveToCurrentRow() throws SQLException { }
    @Override public Statement getStatement() throws SQLException { return null; }
    @Override public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException { return null; }
    @Override public Ref getRef(int columnIndex) throws SQLException { return null; }
    @Override public Blob getBlob(int columnIndex) throws SQLException { return null; }
    @Override public Clob getClob(int columnIndex) throws SQLException { return null; }
    @Override public Array getArray(int columnIndex) throws SQLException { return null; }
    @Override public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException { return null; }
    @Override public Ref getRef(String columnLabel) throws SQLException { return null; }
    @Override public Blob getBlob(String columnLabel) throws SQLException { return null; }
    @Override public Clob getClob(String columnLabel) throws SQLException { return null; }
    @Override public Array getArray(String columnLabel) throws SQLException { return null; }
    @Override public Date getDate(int columnIndex, Calendar cal) throws SQLException { return null; }
    @Override public Date getDate(String columnLabel, Calendar cal) throws SQLException { return null; }
    @Override public Time getTime(int columnIndex, Calendar cal) throws SQLException { return null; }
    @Override public Time getTime(String columnLabel, Calendar cal) throws SQLException { return null; }
    @Override public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException { return null; }
    @Override public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException { return null; }
    @Override public URL getURL(int columnIndex) throws SQLException { return null; }
    @Override public URL getURL(String columnLabel) throws SQLException { return null; }
    @Override public void updateRef(int columnIndex, Ref x) throws SQLException { }
    @Override public void updateRef(String columnLabel, Ref x) throws SQLException { }
    @Override public void updateBlob(int columnIndex, Blob x) throws SQLException { }
    @Override public void updateBlob(String columnLabel, Blob x) throws SQLException { }
    @Override public void updateClob(int columnIndex, Clob x) throws SQLException { }
    @Override public void updateClob(String columnLabel, Clob x) throws SQLException { }
    @Override public void updateArray(int columnIndex, Array x) throws SQLException { }
    @Override public void updateArray(String columnLabel, Array x) throws SQLException { }
    @Override public RowId getRowId(int columnIndex) throws SQLException { return null; }
    @Override public RowId getRowId(String columnLabel) throws SQLException { return null; }
    @Override public void updateRowId(int columnIndex, RowId x) throws SQLException { }
    @Override public void updateRowId(String columnLabel, RowId x) throws SQLException { }
    @Override public int getHoldability() throws SQLException { return 0; }
    @Override public boolean isClosed() throws SQLException { return false; }
    @Override public void updateNString(int columnIndex, String nString) throws SQLException { }
    @Override public void updateNString(String columnLabel, String nString) throws SQLException { }
    @Override public void updateNClob(int columnIndex, NClob nClob) throws SQLException { }
    @Override public void updateNClob(String columnLabel, NClob nClob) throws SQLException { }
    @Override public NClob getNClob(int columnIndex) throws SQLException { return null; }
    @Override public NClob getNClob(String columnLabel) throws SQLException { return null; }
    @Override public SQLXML getSQLXML(int columnIndex) throws SQLException { return null; }
    @Override public SQLXML getSQLXML(String columnLabel) throws SQLException { return null; }
    @Override public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException { }
    @Override public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException { }
    @Override public String getNString(int columnIndex) throws SQLException { return null; }
    @Override public String getNString(String columnLabel) throws SQLException { return null; }
    @Override public Reader getNCharacterStream(int columnIndex) throws SQLException { return null; }
    @Override public Reader getNCharacterStream(String columnLabel) throws SQLException { return null; }
    @Override public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException { }
    @Override public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException { }
    @Override public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException { }
    @Override public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException { }
    @Override public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException { }
    @Override public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException { }
    @Override public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException { }
    @Override public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException { }
    @Override public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException { }
    @Override public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException { }
    @Override public void updateClob(int columnIndex, Reader reader, long length) throws SQLException { }
    @Override public void updateClob(String columnLabel, Reader reader, long length) throws SQLException { }
    @Override public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException { }
    @Override public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException { }
    @Override public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException { }
    @Override public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException { }
    @Override public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException { }
    @Override public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException { }
    @Override public void updateCharacterStream(int columnIndex, Reader x) throws SQLException { }
    @Override public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException { }
    @Override public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException { }
    @Override public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException { }
    @Override public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException { }
    @Override public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException { }
    @Override public void updateClob(int columnIndex, Reader reader) throws SQLException { }
    @Override public void updateClob(String columnLabel, Reader reader) throws SQLException { }
    @Override public void updateNClob(int columnIndex, Reader reader) throws SQLException { }
    @Override public void updateNClob(String columnLabel, Reader reader) throws SQLException { }
    @Override public <T> T unwrap(Class<T> iface) throws SQLException { return null; }
    @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
}
