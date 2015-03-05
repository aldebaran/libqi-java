/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
import com.aldebaran.qi.Application;
import com.aldebaran.qi.CallError;
import com.aldebaran.qi.Future;
import com.aldebaran.qi.AnyObject;
import com.aldebaran.qi.Session;

public class ReactToEvents {

	private AnyObject tts;
	private AnyObject memory;
	private CallBack callback;

	public class CallBack {
		private AnyObject tts;

		public CallBack(AnyObject tts) {
			this.tts = tts;
		}

		public void onTouch(Object value) {
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
		AnyObject subscriber = (AnyObject) memory.call("subscriber",
				"FrontTactilTouched").get();
		subscriber.connect("signal::(m)", "onTouch::(m)", callback);
		application.run();

	}

	public static void main(String[] args) throws Exception {
		ReactToEvents reactor = new ReactToEvents();
		reactor.run(args);
	}
}
