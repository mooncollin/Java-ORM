package model;

import java.math.BigInteger;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class Table
{
	private static final int TABLE_STRING = 0;
	private static final int CREATE_TABLE_STRING = 1;
	private static final int DROP_TABLE_STRING = 2;
	private static final Map<Integer, String[]> tableStrings = new HashMap<Integer, String[]>();
	private static final ReentrantReadWriteLock tableStringsLocks = new ReentrantReadWriteLock();
	
	private final String tableName;
	private boolean existsCache;
	
	private final Object[] oldValues;
	private final Column<?>[] currentColumns;
	private final Column<?>[] primaryKeys;
	private final Map<String, List<ForeignKey<?>>> foreignKeys;
	private final Map<String, Column<?>> namesToColumns;
	
	private boolean inDatabase;
	
	public Table(String tableName, Column<?>... columns)
	{
		this.tableName = tableName;
		currentColumns = columns;
		
		oldValues = new Object[currentColumns.length];
		updateOldValues();
		
		primaryKeys = Arrays.stream(currentColumns)
									.filter(c -> c.isPrimaryKey())
									.toArray(Column[]::new);
		
		namesToColumns = Arrays.stream(currentColumns)
									   .collect(Collectors.toMap(Column::getName, k -> k));
		
		foreignKeys = new HashMap<String, List<ForeignKey<?>>>();
		
		Arrays.stream(currentColumns)
					  .filter(column -> column.isForeignKey())
					  .map(entry -> entry.getForeignKey())
					  .forEach(key -> {
						 var value = foreignKeys.putIfAbsent(key.getTableReference().getName(), new LinkedList<ForeignKey<?>>());
						 if(value == null)
						 {
							 value = foreignKeys.get(key.getTableReference().getName());
						 }
						 value.add(key);
					  });
		
		tableStringsLocks.readLock().lock();
		if(!tableStrings.containsKey(hashCode()))
		{
			tableStringsLocks.readLock().unlock();
			var tableString = generateTableString();
			tableStringsLocks.writeLock().lock();
			try
			{
				tableStrings.put(hashCode(), new String[] {tableString, 
						String.format("CREATE TABLE IF NOT EXISTS %s", tableString),
						String.format("DROP TABLE IF EXISTS\n%s\nCASCADE", tableName)});
			}
			finally
			{
				tableStringsLocks.writeLock().unlock();
			}
		}
		else
		{
			tableStringsLocks.readLock().unlock();
		}
	}
	
	@Override
	public int hashCode()
	{
		return tableName.hashCode();
	}
	
	public boolean inDatabase()
	{
		return inDatabase;
	}
	
	public void setInDatabase(boolean b)
	{
		inDatabase = b;
	}
	
	public Column<?>[] getColumns()
	{
		return currentColumns;
	}
	
	public Column<?> getColumn(String name)
	{
		return namesToColumns.get(name);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Comparable<T>> Column<T> getColumn(Column<T> column)
	{
		return (Column<T>) getColumn(column.getName());
	}
	
	public Object getColumnValue(String name)
	{
		return getColumn(name).getValue();
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getColumnValue(Column<T> column)
	{
		return (T) getColumnValue(column.getName());
	}
	
	public void setColumnValue(String name, Object value)
	{
		getColumn(name).setValue(value);
	}
	
	public <T> void setColumnValue(Column<T> column, T value)
	{
		setColumnValue(column.getName(), value);
	}
	
	public String getName()
	{
		return tableName;
	}
	
	public String getCreateSQL()
	{
		String sql;
		tableStringsLocks.readLock().lock();
		try
		{
			sql = tableStrings.get(hashCode())[CREATE_TABLE_STRING];
		}
		finally
		{
			tableStringsLocks.readLock().unlock();
		}
		return sql;
	}
	
	public String getDropSQL()
	{
		String sql;
		tableStringsLocks.readLock().lock();
		try
		{
			sql = tableStrings.get(hashCode())[DROP_TABLE_STRING];
		}
		finally
		{
			tableStringsLocks.readLock().unlock();
		}
		return sql;
	}
	
	public String getCommitSQL()
	{
		return generateUpdateString(getChangedColumns());
	}
	
	public List<Column<?>> getChangedColumns()
	{
		var different = new LinkedList<Column<?>>();
		for(int i = 0; i < currentColumns.length; i++)
		{
			var oldValue = oldValues[i];
			var currentValue = currentColumns[i];
			if(!currentValue.isAutoIncrement() &&
					   ((oldValue != null && currentValue.getValue() == null)
					|| (oldValue == null && currentValue.getValue() != null)
					|| (oldValue != null && currentValue.getValue() != null && !oldValue.equals(currentValue.getValue()))))
				{
					different.add(currentValue);
				}
		}
		
		return different;
	}
	
	public void commit(Database db) throws SQLException
	{
		if(!existsCache)
		{
			createTable(db);
		}
		
		var different = getChangedColumns();
		
		if(different.isEmpty())
		{
			return;
		}
		
		String sql = generateUpdateString(different);
		
		try(var connection = db.getConnection())
		{
			var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			var difIt = different.iterator();
			var count = 1;
			
			while(difIt.hasNext())
			{
				statement.setObject(count++, difIt.next().getValue());
			}
			
			if(!needsAutoGenerated()
			 && inDatabase)
			{
				for(var key : primaryKeys)
				{
					statement.setObject(count++, key.getValue());
				}
			}
			
			statement.executeUpdate();
			
			if(needsAutoGenerated())
			{
				var results = statement.getGeneratedKeys();
				for(var key : primaryKeys)
				{
					results.next();
					Object value = results.getObject(1);
					if(value instanceof BigInteger)
					{
						key.setValue(BigInteger.class.cast(value).intValue());
					}
					else
					{
						key.setValue(value);
					}
				}
			}
		}
		
		updateOldValues();
		inDatabase = true;
	}
	
	public boolean update(Database db) throws SQLException
	{
		if(primaryKeys.length == 0
		 || Arrays.stream(primaryKeys).anyMatch(k -> k == null))
		{
			throw new IllegalStateException("Cannot update with no primary keys");
		}
		
		try(var connection = db.getConnection())
		{
			var selectWithIDQuery = query(db);
			for(var primaryKey : primaryKeys)
			{
				selectWithIDQuery.filter(primaryKey);
			}
			
			var option = selectWithIDQuery.first();
			Table results;
			
			if(option.isEmpty())
			{
				return false;
			}
			else
			{
				results = option.get();
			}
			
			Arrays.parallelSetAll(this.currentColumns, (int i) -> results.getColumns()[i]);

			var hasChanged = !getChangedColumns().isEmpty();
			updateOldValues();
			
			return hasChanged;
		}
	}
	
	public void delete(Database db) throws SQLException
	{
		var deleteSQL = String.format("DELETE FROM %s WHERE %s", getName(),
			generateIDFilterString());
		
		try(var connection = db.getConnection())
		{
			var statement = connection.prepareStatement(deleteSQL);
			var index = 1;
			for(var primaryKey : primaryKeys)
			{
				statement.setObject(index++, primaryKey.getValue());
			}
			
			statement.executeUpdate();
			
			for(var column : currentColumns)
			{
				column.setValue(null);
			}
			
			updateOldValues();
		}
	}
	
	public Query query(Database db) throws SQLException
	{
		if(!existsCache)
		{
			createTable(db);
		}
		return new Query(db, this);
	}
	
	public void createTable(Database db) throws SQLException
	{
		try(var connection = db.getConnection())
		{
			var statement = connection.prepareCall(getCreateSQL());
			statement.executeUpdate();
			existsCache = true;
		}
	}
	
	public void drop(Database db) throws SQLException
	{
		if(!existsCache)
		{
			createTable(db);
		}
		try(var connection = db.getConnection())
		{
			var statement = connection.prepareCall(getDropSQL());
			statement.executeUpdate();
			existsCache = false;
		}
	}
	
	public boolean exists(Database db) throws SQLException
	{
		try(var connection = db.getConnection())
		{
			var metaData = connection.getMetaData();
			var tableInfo = metaData.getTables(null, null, tableName, null);
			return tableInfo.next();
		}
	}
	
	public String toString()
	{
		String sql;
		tableStringsLocks.readLock().lock();
		try
		{
			sql = tableStrings.get(hashCode())[TABLE_STRING];
		}
		finally
		{
			tableStringsLocks.readLock().unlock();
		}
		return sql;
	}
	
	private String generateUpdateString(List<Column<?>> changingColumns)
	{	
		if(needsAutoGenerated()
			|| !inDatabase)
		{
			return String.format("INSERT INTO %s (%s)\n"
					+ "VALUES (%s)", tableName,
					String.join(", ", changingColumns.stream().map(k -> k.getName()).toArray(String[]::new)),
					String.join(", ", changingColumns.stream().map(c -> "?").toArray(String[]::new)));
		}
						
		return String.format("UPDATE %s\n"
				+ "SET %s\n"
				+ "WHERE %s;", tableName,
				String.join(", ", 
					changingColumns.stream().map(c -> String.format("%s = ?", c.getName())).toArray(String[]::new)),
				generateIDFilterString());
	}
	
	private boolean needsAutoGenerated()
	{
		return Arrays.stream(primaryKeys)
				.anyMatch(p -> p.getValue() == null);
	}
	
	private String generateIDFilterString()
	{
		return String.join(" AND ", Arrays.stream(primaryKeys).map(k -> String.format("%s = ?", k.getName())).toArray(String[]::new));
	}
	
	private String generateTableString()
	{
		var tableString = String.format("%s (\n", tableName);
		
		tableString += String.join(",\n", 
				Arrays.stream(currentColumns).map(Column::toString).toArray(String[]::new));
		
		if(primaryKeys.length != 0)
		{
			tableString += String.format(",\nPRIMARY KEY (%s)", 
				String.join(", ", Arrays.stream(primaryKeys).map(Column::getName).toArray(String[]::new)));
		}
		
		if(!foreignKeys.isEmpty())
		{
			for(var entry : foreignKeys.entrySet())
			{
				String columnNames = String.join(", ", entry.getValue().stream().map(ForeignKey::getName).toArray(String[]::new));
				String foreignColumnNames = String.join(", ", entry.getValue().stream().map(f -> f.getColumnReference().getName()).toArray(String[]::new));
				
				tableString += String.format(",\nFOREIGN KEY (%s)\n"
						+ "REFERENCES %s(%s) ON DELETE CASCADE",
						columnNames,
						entry.getKey(),
						foreignColumnNames);
			}
		}
		
		tableString += "\n);";
		
		return tableString;
	}
	
	private void updateOldValues()
	{
		Arrays.parallelSetAll(oldValues, (int i) -> (Object) currentColumns[i].getValue());
	}
}
