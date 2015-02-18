import com.aldebaran.qimessaging.*;
public class SayHello {

	public static void main(String[] args) throws Exception {
		Application app = new Application(args);
		Session session = new Session();
		Future<Void> fut = session.connect("tcp://nao.local:9559");
		synchronized(fut) {
			fut.wait(1000);
		}

		com.aldebaran.qimessaging.AnyObject tts = null;
		tts = session.service("ALTextToSpeech");


		boolean ping = tts.<Boolean>call("ping").get();
		if (!ping) {
			System.out.println("Could not ping TTS");
		} else {
			System.out.println("Ping ok");
		}

		System.out.println("Calling say");
		tts.call("say", "Hello, world");

	}

}
