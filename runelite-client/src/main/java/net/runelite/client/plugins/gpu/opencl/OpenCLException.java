package net.runelite.client.plugins.gpu.opencl;

public class OpenCLException extends Exception
{

	public OpenCLException(String message)
	{
		super(message);
	}

	public OpenCLException(String message, Exception cause)
	{
		super(message, cause);
	}

}
