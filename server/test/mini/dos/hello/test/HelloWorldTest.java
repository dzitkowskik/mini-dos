package mini.dos.hello.test;

import static org.junit.Assert.*;
import org.junit.Test;
import mini.dos.hello.HelloWorld;

public class HelloWorldTest {
	@Test
	public void testHellowWorld(){
		HelloWorld hello = new HelloWorld();
		assertEquals("A test for HelloWorld", "HelloWorld!", hello.sayHello());
	}
}