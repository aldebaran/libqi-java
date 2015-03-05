/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
import com.aldebaran.qi.*;

public class HelloService extends QimessagingService
{
	public String greet(String name) {
		return "Hello, " + name;
	}

}
