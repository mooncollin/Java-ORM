package model;

import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import model.Filter.FilterType;
import model.Filter.RelationType;

public class Query
{
	private final Table model;
	private final Column<?>[] modelColumns;
	private List<Filter> filters;
	private Map<String, Join> joins;
	private final Database database;
	
	public static <T extends Table> Query query(Database database, Class<T> tableClass)
	{
		try
		{
			return new Query(database, tableClass.getConstructor().newInstance());
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e)
		{
			throw new RuntimeException(e.getMessage());
		}
	}
	
	public Query(Database database, Table model)
	{
		filters 		= new LinkedList<Filter>();
		joins			= new HashMap<String, Join>();
		this.model 		= Objects.requireNonNull(model);
		this.database 	= Objects.requireNonNull(database);
		modelColumns 	= model.getColumns().stream().toArray(Column<?>[]::new);
	}
	
	public <F extends Comparable<F>> Query filter(Column<F> column, F value)
	{
		return filter(column.cloneWithValue(value));
	}
	
	public <F extends Comparable<F>> Query filter(Column<F> column, F value, FilterType type)
	{
		return filter(column.cloneWithValue(value), type);
	}
	
	public Query filter(Column<?> column)
	{
		return filter(column, FilterType.EQUAL);
	}
	
	public Query filter(Column<?> column, FilterType type)
	{
		return filter(new Filter().filterColumn(column, type));
	}
	
	public Query filter(RelationType type, List<Column<?>> columns, List<FilterType> types)
	{
		if(columns.size() < 1 || types.size() < 1)
		{
			return this;
		}
		if(columns.size() != types.size())
		{
			throw new IllegalArgumentException("Columns and types must be the same size");
		}
		
		var filter = new Filter();
		var columnsIt = columns.iterator();
		var typesIt = types.iterator();
		
		filter.filterColumn(columnsIt.next(), typesIt.next());
		
		while(columnsIt.hasNext() && typesIt.hasNext())
		{
			filter.filterColumn(type, columnsIt.next(), typesIt.next());
		}
		
		return filter(filter);
	}
	
	public Query filter(Filter filter)
	{
		filters.add(Objects.requireNonNull(filter));
		return this;
	}
	
	public Query join(Join join)
	{
		var key = Objects.requireNonNull(join).getTable().getName();
		var value =  joins.putIfAbsent(key, join);
		if(value != null)
		{
			value.merge(join);
		}
		return this;
	}
	
	
	public <T extends Comparable<T>> Query join(Table table, Column<T> column, FilterType type)
	{
		Join j = new Join(table);
		j.joinOnValue(new Filter().filterColumn(column, type));
		return join(j);
	}
	
	public <T extends Comparable<T>> Query join(Table table, Column<T> column, FilterType type, Table otherTable, Column<?> otherColumn)
	{
		Join j = new Join(table);
		j.joinOnColumn(new Filter().filterColumn(column, type), otherTable, otherColumn);
		return join(j);
	}
	
	public String toString()
	{
		var sqlString = String.format("SELECT * from %s", model.getName());
		for(var key : joins.keySet())
		{
			sqlString += String.format("\nJOIN %s", joins.get(key));
		}
		
		if(!filters.isEmpty())
		{
			sqlString += "\nWHERE ";
			
			sqlString += String.join(" AND ", 
					filters.stream().map(Filter::toString).toArray(String[]::new));
		}
		
		return sqlString;
	}
	
	public Optional<Table> first() throws SQLException
	{
		var sqlString = toString();
		
		try(var connection = database.getConnection())
		{
			var statement = connection.prepareStatement(sqlString);
			statement.setMaxRows(1);
			fillStatement(statement);
			
			var resultSet = statement.executeQuery();
			if(!resultSet.next())
			{
				return Optional.empty();
			}
			return Optional.of(generateRow(resultSet));
		}
	}
	
	public List<Table> all() throws SQLException
	{
		var results = new LinkedList<Table>();
		
		var sqlString = toString();
		
		try(var connection = database.getConnection())
		{
			var statement = connection.prepareStatement(sqlString);
			fillStatement(statement);
			
			var resultSet = statement.executeQuery();
			while(resultSet.next())
			{
				results.add(generateRow(resultSet));
			}
		}
		
		
		return results;
	}
	
	private void fillStatement(PreparedStatement s) throws SQLException
	{
		var index = 1;
		
		for(var entry : joins.entrySet())
		{
			for(var filter : entry.getValue().getValueFilters())
			{
				for(var column : filter.getColumns())
				{
					s.setObject(index++, column.getValue());
				}
			}
		}

		for(var filter : filters)
		{
			for(var column : filter.getColumns())
			{
				s.setObject(index++, column.getValue());
			}
		}
	}
	
	private Table generateRow(ResultSet results) throws SQLException
	{
		var columns = new Column<?>[modelColumns.length];
		for(var i = 0; i < columns.length; i++)
		{
			columns[i] = modelColumns[i].cloneWithValue(results.getObject(i+1));
		}
		
		var row = new Table(model.getName(), columns);
		row.setInDatabase(true);

		return row;
	}
}