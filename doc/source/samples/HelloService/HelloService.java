import com.aldebaran.qimessaging.*;

public class HelloService extends QimessagingService
{
	public String greet(String name) {
		return "Hello, " + name;
	}

}
