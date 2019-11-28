package model;

public class Pair<T1, T2>
{
	private T1 first;
	private T2 second;
	
	public Pair(T1 first, T2 second)
	{
		setFirst(first);
		setSecond(second);
	}
	
	public void setFirst(T1 first)
	{
		this.first = first;
	}
	
	public void setSecond(T2 second)
	{
		this.second = second;
	}
	
	public T1 getFirst()
	{
		return first;
	}
	
	public T2 getSecond()
	{
		return second;
	}
	
	public static <F, S> Pair<F, S> of(F first, S second)
	{
		return new Pair<F, S>(first, second);
	}
}
