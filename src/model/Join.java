package model;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import model.Filter.RelationType;


public class Join
{
	private Table table;
	private List<Filter> valueFilters;
	private List<Filter> columnFilters;
	private List<Table> otherTables;
	private List<Column<?>> otherColumns;
	private List<RelationType> relations;
	
	public Join(Table table1)
	{
		this.table = Objects.requireNonNull(table1);
		valueFilters = new LinkedList<Filter>();
		columnFilters = new LinkedList<Filter>();
		otherTables = new LinkedList<Table>();
		otherColumns = new LinkedList<Column<?>>();
		relations = new LinkedList<RelationType>();
	}
	
	public Join joinOnValue(Filter filter)
	{
		if(containsJoin())
		{
			return joinOnValue(RelationType.AND, filter);
		}
		
		valueFilters.add(Objects.requireNonNull(filter));	
		
		return this;
	}
	
	public Join joinOnValue(RelationType relation, Filter filter)
	{
		if(!containsJoin())
		{
			throw new IllegalStateException("Must have at least one join column before adding a relation");			
		}
		
		relations.add(Objects.requireNonNull(relation));
		valueFilters.add(Objects.requireNonNull(filter));
		
		return this;
	}
	
	public Join joinOnColumn(Filter filter, Table otherTable, Column<?> otherColumn)
	{
		if(containsJoin())
		{
			return joinOnColumn(RelationType.AND, filter, otherTable, otherColumn);
		}
		
		columnFilters.add(Objects.requireNonNull(filter));
		otherTables.add(Objects.requireNonNull(otherTable));
		otherColumns.add(Objects.requireNonNull(otherColumn));
		
		return this;
	}
	
	public Join joinOnColumn(RelationType relation, Filter filter, Table otherTable, Column<?> otherColumn)
	{
		if(!containsJoin())
		{
			throw new IllegalStateException("Must have at least one join column before adding a relation");			
		}
		
		relations.add(Objects.requireNonNull(relation));
		columnFilters.add(Objects.requireNonNull(filter));
		otherTables.add(Objects.requireNonNull(otherTable));
		otherColumns.add(Objects.requireNonNull(otherColumn));
		
		return this;
	}
	
	public Table getTable()
	{
		return table;
	}
	
	public Join merge(Join otherJoin)
	{
		this.valueFilters.addAll(otherJoin.valueFilters);
		this.columnFilters.addAll(otherJoin.columnFilters);
		this.otherTables.addAll(otherJoin.otherTables);
		this.otherColumns.addAll(otherJoin.otherColumns);
		this.relations.addAll(otherJoin.relations);
		
		if(!valueFilters.isEmpty() && !otherJoin.valueFilters.isEmpty())
		{
			this.relations.add(RelationType.AND);
		}
		if(!columnFilters.isEmpty() && !otherJoin.columnFilters.isEmpty())
		{
			this.relations.add(RelationType.AND);
		}
		
		return this;
	}
	
	public List<Filter> getValueFilters()
	{
		return valueFilters;
	}
	
	public String toString()
	{
		String result = table.getName();
		var relationIt = relations.iterator();
		
		if(!valueFilters.isEmpty() || !columnFilters.isEmpty())
		{
			result += "\nON";
		}
		
		for(var valueFilter : valueFilters)
		{
			result += String.format("\n%s", normalizeFilter(valueFilter));
			if(relationIt.hasNext())
			{
				result += String.format("\n%s", relationIt.next());
			}
		}
		
		var colFilterIt = columnFilters.iterator();
		var columnIt = otherColumns.iterator();
		var tableIt = otherTables.iterator();
		
		while(colFilterIt.hasNext())
		{
			var filter = colFilterIt.next();
			var otherTable = tableIt.next();
			var otherColumn = columnIt.next();
			var filterString = normalizeFilter(filter);
			filterString = filterString.replace("?", String.format("%s.%s", otherTable.getName(), otherColumn.getName()));
			result += String.format("\n%s", filterString);
			if(relationIt.hasNext())
			{
				result += String.format("\n%s", relationIt.next());
			}
		}
		
		return result;
	}
	
	private String normalizeFilter(Filter filter)
	{
		String filterString = filter.toString();
		for(var column : filter.getColumns())
		{
			filterString = filterString.replace(column.getName(), String.format("%s.%s", table.getName(), column.getName()));
		}
		
		return filterString;
	}
	
	private boolean containsJoin()
	{
		return !valueFilters.isEmpty() || !columnFilters.isEmpty();
	}
}
