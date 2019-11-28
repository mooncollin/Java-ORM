package model;

import java.util.Objects;

public class ForeignKeyBuilder <T extends Comparable<T>>
{
	private String thisColumnName;
	private Table tableReference;
	private final Column<T> columnReference;
	
	public static <J extends Comparable<J>> ForeignKeyBuilder<J> start(Column<J> columnReference)
	{
		return new ForeignKeyBuilder<J>(columnReference);
	}
	
	public ForeignKeyBuilder(Column<T> columnReference)
	{
		this.columnReference = Objects.requireNonNull(columnReference);
	}
	
	public ForeignKeyBuilder<T> setTableReference(Table ref)
	{
		this.tableReference = Objects.requireNonNull(ref);
		return this;
	}
	
	public ForeignKeyBuilder<T> setColumnName(String thisColumnName)
	{
		this.thisColumnName = Objects.requireNonNull(thisColumnName);
		
		return this;
	}
	
	public ForeignKey<T> build()
	{
		return new ForeignKey<T>(thisColumnName, tableReference, columnReference);
	}
}
