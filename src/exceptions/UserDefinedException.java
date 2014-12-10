package exceptions;

public class UserDefinedException extends Exception {

	private String desc;
	
	public UserDefinedException(String desc)
	{
		this.desc = desc;
	}
	
	public String getDesc()
	{
		return this.desc;
	}
}
