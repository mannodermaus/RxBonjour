package rxbonjour.internal;

import org.junit.Test;

import rx.observers.TestSubscriber;
import rxbonjour.base.BaseTest;
import rxbonjour.exc.StaleContextException;
import rxbonjour.model.BonjourEvent;

public class JBBonjourDiscoveryTest extends BaseTest {

	@Test public void testStaleContext() throws Exception {
		BonjourDiscovery discovery = new JBBonjourDiscovery();
		TestSubscriber<BonjourEvent> subscriber = new TestSubscriber<>();

		discovery.start(null, "_http._tcp").subscribe(subscriber);

		subscriber.assertError(StaleContextException.class);
	}

	// TODO Fill with more tests
}
