package rxbonjour.internal;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import rxbonjour.base.BaseTest;

import static junit.framework.Assert.assertEquals;

public class BacklogTest extends BaseTest {

	@Test public void testQueue() throws Exception {
		final AtomicInteger counter = new AtomicInteger(0);
		Backlog<String> backlog = new Backlog<String>() {
			@Override public void onNext(Backlog<String> backlog, String item) {
				counter.incrementAndGet();
				backlog.proceed();
			}
		};

		backlog.add("1");
		backlog.add("2");
		backlog.add("3");
		Thread.sleep(500);
		backlog.quit();

		assertEquals(3, counter.get());
	}
}
