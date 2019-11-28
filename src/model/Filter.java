package model;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Filter
{
	private List<Column<?>> columns;
	private List<FilterType> types;
	private List<RelationType> relations;
	
	public Filter()
	{
		columns = new LinkedList<Column<?>>();
		types = new LinkedList<FilterType>();
		relations = new LinkedList<RelationType>();
	}
	
	public Filter filterColumn(Column<?> column, FilterType type)
	{
		if(!columns.isEmpty())
		{
			filterColumn(RelationType.AND, column, type);
		}
		else
		{
			columns.add(Objects.requireNonNull(column));
			types.add(Objects.requireNonNull(type));
		}
		
		return this;
	}
	
	public Filter filterColumn(RelationType relation, Column<?> column, FilterType type)
	{
		if(columns.isEmpty())
		{
			throw new IllegalStateException("Must have at least one filtering column before adding a relation");
		}
		
		relations.add(Objects.requireNonNull(relation));
		columns.add(Objects.requireNonNull(column));
		types.add(Objects.requireNonNull(type));
		
		return this;
	}
	
	public List<Column<?>> getColumns()
	{
		return columns;
	}
	
	public List<FilterType> getTypes()
	{
		return types;
	}
	
	public List<RelationType> getRelations()
	{
		return relations;
	}
	
	public String toString()
	{
		String result = "(";
		var columnsIt = columns.iterator();
		var typesIt = types.iterator();
		var relationsIt = relations.iterator();
		
		var first = true;
		
		while(columnsIt.hasNext())
		{
			if(!first)
			{
				result += String.format(" %s ", relationsIt.next().toString());
			}
			
			var column = columnsIt.next();
			var type = typesIt.next();
			result += String.format("%s %s ?", column.getName(), type.symbol());
			
			first = false;
		}
		
		return result + ")";
	}
	
	public enum FilterType
	{
		EQUAL, NOT_EQUAL, GREATER_THAN, LESS_THAN, GREATER_THAN_EQUAL,
		LESS_THAN_EQUAL;
		
		public String symbol()
		{
			String sym = null;
			switch(this)
			{
				case EQUAL:
					sym = "=";
					break;
				case NOT_EQUAL:
					sym = "!=";
					break;
				case GREATER_THAN:
					sym = ">";
					break;
				case LESS_THAN:
					sym = "<";
					break;
				case GREATER_THAN_EQUAL:
					sym = ">=";
					break;
				case LESS_THAN_EQUAL:
					sym = "<=";
					break;
			}
			
			return sym;
		}
	}
	
	public enum RelationType
	{
		AND, OR
	}
}
