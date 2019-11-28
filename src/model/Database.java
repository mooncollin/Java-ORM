package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

public class Database
{
	public static void registerDriver(String className) throws ClassNotFoundException
	{
		Class.forName(className);
	}
	
	private String url;
	private String username;
	private String password;
	
	public Database(String url, String username, String password)
	{
		setURL(url);
		setUsername(username);
		setPassword(password);
	}
	
	public void setURL(String url)
	{
		this.url = Objects.requireNonNull(url);
	}
	
	public void setUsername(String username)
	{
		this.username = Objects.requireNonNull(username);
	}
	
	public void setPassword(String password)
	{
		this.password = Objects.requireNonNull(password);
	}
	
	public String getURL()
	{
		return url;
	}
	
	public String getUsername()
	{
		return username;
	}
	
	public String getPassword()
	{
		return password;
	}
	
	public Connection getConnection() throws SQLException
	{
		return DriverManager.getConnection(url, username, password);
	}
	
	public boolean canConnect()
	{
		try(var connection = getConnection())
		{
			return true;
		}
		catch(SQLException e)
		{
			return false;
		}
	}
}
