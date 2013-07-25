import com.aldebaran.qimessaging.*;
import com.aldebaran.qimessaging.Object;

public class App
{
	public static void main(String[] args) throws Exception {

		Application app = new Application(args);
		String address = "tcp://127.0.0.1:9559";
		Session session = new Session();
		QimessagingService service = new HelloService();
		DynamicObjectBuilder objectBuilder = new DynamicObjectBuilder();
		objectBuilder.advertiseMethod("greet::s(s)", service, "Greet the caller");
		Object object = objectBuilder.object();
		service.init(object);

		System.out.println("Connecting to: " + address);
		Future<Void> fut = session.connect(address);
		synchronized(fut) {
			fut.wait(1000);
		}

		System.out.println("Registering hello service");
		session.registerService("hello", objectBuilder.object());

		while(true) {
			Thread.sleep(1);
		}
	}
}
