package rxbonjour.exc;

/**
 * @author marcel
 */
public class StaleContextException extends RuntimeException {

	public StaleContextException() {
		super("The observable's Context reference is stale");
	}
}
