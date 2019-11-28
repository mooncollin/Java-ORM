package model;

import java.util.Objects;

public class ColumnBuilder<F extends Comparable<F>>
{
	private final int type;
	private String name;
	private int length;
	private boolean primaryKey;
	private boolean nullable;
	private boolean autoIncrement;
	private ForeignKey<F> foreignKey;
	private ForeignKeyBuilder<F> foreignKeyBuilder;
	private final Class<F> valueClass;
	
	public static <J extends Comparable<J>> ColumnBuilder<J> start(Class<J> clazz, int type)
	{
		return new ColumnBuilder<J>(clazz, type);
	}
	
	public ColumnBuilder(Class<F> clazz, int type)
	{
		valueClass = clazz;
		this.type = type;
	}
	
	public ColumnBuilder<F> setName(String name)
	{
		this.name = Objects.requireNonNull(name);
		return this;
	}
	
	public ColumnBuilder<F> setLength(int length)
	{
		this.length = length;
		return this;
	}
	
	public ColumnBuilder<F> isPrimaryKey(boolean b)
	{
		this.primaryKey = b;
		return this;
	}
	
	public ColumnBuilder<F> isNullable(boolean b)
	{
		this.nullable = b;
		return this;
	}
	
	public ColumnBuilder<F> isAutoIncremented(boolean b)
	{
		this.autoIncrement = b;
		return this;
	}
	
	public ColumnBuilder<F> setForeignKey(ForeignKeyBuilder<F> keyBuilder)
	{
		this.foreignKey = null;
		this.foreignKeyBuilder = keyBuilder;
		return this;
	}
	
	public ColumnBuilder<F> setForeignKey(ForeignKey<F> key)
	{
		this.foreignKeyBuilder = null;
		this.foreignKey = key;
		return this;
	}
	
	public Column<F> build()
	{
		if(this.foreignKeyBuilder != null)
		{
			this.foreignKey = this.foreignKeyBuilder.setColumnName(this.name)
													.build();
		}
		
		return new Column<F>(this.valueClass, this.name, this.type,
			this.length, this.primaryKey, this.nullable, this.autoIncrement,
			this.foreignKey);
	}
}
