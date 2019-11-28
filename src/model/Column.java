package model;

import java.sql.Types;
import java.util.Map;
import java.util.Objects;

public class Column <T extends Comparable<T>> implements Comparable<Column<T>>
{
	public static final int TEXT = -10;
	
	private static final Map<Integer, String> TYPE_TO_STRING = Map.ofEntries
	(
			Map.entry(Types.ARRAY, "ARRAY"),
			Map.entry(Types.BIGINT, "BIGINT"),
			Map.entry(Types.BINARY, "BINARY"),
			Map.entry(Types.BIT, "BIT"),
			Map.entry(Types.BLOB, "BLOB"),
			Map.entry(Types.BOOLEAN, "BOOLEAN"),
			Map.entry(Types.CHAR, "CHAR"),
			Map.entry(Types.CLOB, "CLOB"),
			Map.entry(Types.DATALINK, "DATALINK"),
			Map.entry(Types.DATE, "DATE"),
			Map.entry(Types.DECIMAL, "DECIMAL"),
			Map.entry(Types.DISTINCT, "DISTINCT"),
			Map.entry(Types.DOUBLE, "DOUBLE"),
			Map.entry(Types.FLOAT, "FLOAT"),
			Map.entry(Types.INTEGER, "INTEGER"),
			Map.entry(Types.JAVA_OBJECT, "JAVA_OBJECT"),
			Map.entry(Types.LONGNVARCHAR, "LONGNVARCHAR"),
			Map.entry(Types.LONGVARBINARY, "LONGVARBINARY"),
			Map.entry(Types.LONGVARCHAR, "LONGVARCHAR"),
			Map.entry(Types.NCHAR, "NCHAR"),
			Map.entry(Types.NCLOB, "NCLOB"),
			Map.entry(Types.NULL, "NULL"),
			Map.entry(Types.NUMERIC, "NUMERIC"),
			Map.entry(Types.NVARCHAR, "NVARCHAR"),
			Map.entry(Types.OTHER, "OTHER"),
			Map.entry(Types.REAL, "REAL"),
			Map.entry(Types.REF, "REF"),
			Map.entry(Types.REF_CURSOR, "REF_CURSOR"),
			Map.entry(Types.ROWID, "ROWID"),
			Map.entry(Types.SMALLINT, "SMALLINT"),
			Map.entry(Types.SQLXML, "SQLXML"),
			Map.entry(Types.STRUCT, "STRUCT"),
			Map.entry(Types.TIME, "TIME"),
			Map.entry(Types.TIME_WITH_TIMEZONE, "TIME_WITH_TIMEZONE"),
			Map.entry(Types.TIMESTAMP, "TIMESTAMP"),
			Map.entry(Types.TIMESTAMP_WITH_TIMEZONE, "TIMESTAMP_WITH_TIMEZONE"),
			Map.entry(Types.TINYINT, "TINYINT"),
			Map.entry(Types.VARBINARY, "VARBINARY"),
			Map.entry(Types.VARCHAR, "VARCHAR"),
			Map.entry(TEXT, "TEXT")
	);
	
	private final int type;
	private final String name;
	private final int length;
	private final boolean primaryKey;
	private final boolean nullable;
	private final boolean autoIncrement;
	private final ForeignKey<T> foreignKey;
	private final Class<T> valueClass;
	
	private T value;
	
	/**
	 * 
	 * @param clazz
	 * @param name
	 * @param type
	 * @param length
	 * @param primaryKey
	 * @param nullable
	 * @param autoIncrement
	 * @param foreign
	 */
	public Column(Class<T> clazz, String name, int type, 
		int length, boolean primaryKey, boolean nullable, boolean autoIncrement,
		ForeignKey<T> foreign)
	{
		this.valueClass = Objects.requireNonNull(clazz);
		this.type = type;
		this.name = Objects.requireNonNull(name);
		this.length = length;
		this.primaryKey = primaryKey;
		this.nullable = nullable;
		this.autoIncrement = autoIncrement;
		this.foreignKey = foreign;
	}
	
	public void setValue(T value)
	{
		this.value = this.valueClass.cast(value);
	}
	
	public void setValue(Object value)
	{
		this.value = this.valueClass.cast(value);
	}
	
	public T getValue()
	{
		return value;
	}
	
	public int getType()
	{
		return type;
	}
	
	public String getName()
	{
		return name;
	}
	
	public int getLength()
	{
		return length;
	}
	
	public boolean isPrimaryKey()
	{
		return primaryKey;
	}
	
	public boolean isNullable()
	{
		return nullable;
	}
	
	public boolean isAutoIncrement()
	{
		return autoIncrement;
	}
	
	public ForeignKey<T> getForeignKey()
	{
		return foreignKey;
	}
	
	public boolean isForeignKey()
	{
		return foreignKey != null;
	}
	
	public Class<T> getValueClass()
	{
		return valueClass;
	}
	
	public String toString()
	{
		String result = String.format("%s %s%s", name, TYPE_TO_STRING.get(type), length > 0 ? "(" + length + ")" : "");
		if(!nullable)
		{
			result += " NOT NULL";
		}
		if(autoIncrement)
		{
			result += " AUTO_INCREMENT";
		}
		
		return result;
	}
	
	public boolean equals(Object other)
	{
		if(!(other instanceof Column))
		{
			return false;
		}
		
		var otherC = Column.class.cast(other);
		
		return this.type == otherC.type &&
			   this.name.equals(otherC.name) &&
			   this.length == otherC.length &&
			   this.primaryKey == otherC.primaryKey &&
			   this.nullable == otherC.nullable &&
			   this.autoIncrement == otherC.autoIncrement &&
			   this.value.equals(otherC.value);
	}
	
	public int compareTo(Column<T> other)
	{
		return this.value.compareTo(other.value);
	}
	
	public Object clone()
	{
		var<T> newColumn = new Column<T>(valueClass, this.name,
				this.type, this.length, this.primaryKey,
				this.nullable, this.autoIncrement, this.foreignKey);
		
		return getClass().cast(newColumn);
	}
	
	@SuppressWarnings("unchecked")
	public Column<T> typeClone()
	{
		return getClass().cast(clone());
	}
	
	public Column<T> cloneWithValue(T value)
	{
		var<T> cloned = typeClone();
		cloned.setValue(value);
		
		return cloned;
	}
	
	public Column<T> cloneWithValue(Object value)
	{
		var<T> cloned = typeClone();
		cloned.setValue(value);
		
		return cloned;
	}
}
