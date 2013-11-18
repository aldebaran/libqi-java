import com.aldebaran.qimessaging.Application;
import com.aldebaran.qimessaging.CallError;
import com.aldebaran.qimessaging.Future;
import com.aldebaran.qimessaging.Object;
import com.aldebaran.qimessaging.Session;

public class ReactToEvents {

	private Object tts;
	private Object memory;
	private CallBack callback;

	public class CallBack {
		private Object tts;

		public CallBack(Object tts) {
			this.tts = tts;
		}

		public void onTouch(java.lang.Object value) {
			float data = (Float) value;
			if (data == 1.0) {
				try {
					tts.call("say", "ouch");
				} catch (CallError error) {
					System.err.println(error.getMessage());
				}
			}
		}

	}

	public void run(String[] args) throws Exception {
		String url = "tcp://nao.local:9559";
		if (args.length == 1) {
			url = args[0];
		}
		Application application = new Application(args);
		Session session = new Session();
		Future<Void> fut = session.connect(url);
		synchronized(fut) {
			fut.wait(1000);
		}
		tts = session.service("ALTextToSpeech");
		callback = new CallBack(tts);
		memory = session.service("ALMemory");
		Object subscriber = (Object) memory.call("subscriber",
				"FrontTactilTouched").get();
		subscriber.connect("signal::(m)", "onTouch::(m)", callback);
		application.run();

	}

	public static void main(String[] args) throws Exception {
		ReactToEvents reactor = new ReactToEvents();
		reactor.run(args);
	}
}
