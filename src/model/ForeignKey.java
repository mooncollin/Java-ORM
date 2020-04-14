package model;

import java.util.Objects;

public class ForeignKey <T>
{
	private final String thisColumnName;
	private final Table tableReference;
	private final Column<T> columnReference;
	
	public ForeignKey(String thisColumnName, Table table, Column<T> otherColumn)
	{
		this.thisColumnName = Objects.requireNonNull(thisColumnName);
		this.tableReference = Objects.requireNonNull(table);
		this.columnReference = Objects.requireNonNull(otherColumn);
	}
	
	public String getName()
	{
		return thisColumnName;
	}
	
	public Table getTableReference()
	{
		return tableReference;
	}
	
	public Column<T> getColumnReference()
	{
		return columnReference;
	}
}
