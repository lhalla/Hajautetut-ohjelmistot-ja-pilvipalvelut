
public class Tuple<T, U>
{
	private T data1;
	private U data2;
	
	public Tuple(T data1, U data2)
	{
		this.data1 = data1;
		this.data2 = data2;
	}
	
	public T first()
	{
		return data1;
	}
	
	public U last()
	{
		return data2;
	}
}
