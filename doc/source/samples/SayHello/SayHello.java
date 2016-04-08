/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
import com.aldebaran.qi.*;
import java.util.concurrent.TimeUnit;
public class SayHello {

	public static void main(String[] args) throws Exception {
		Session session = new Session();
		Future<Void> fut = session.connect("tcp://nao.local:9559");
		fut.get(1, TimeUnit.SECONDS);

		com.aldebaran.qi.AnyObject tts = null;
		tts = session.service("ALTextToSpeech").get();

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
