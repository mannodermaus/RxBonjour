package rxbonjour.exc;

/**
 * Thrown when a Context reference becomes stale
 */
public class StaleContextException extends RuntimeException {

	public StaleContextException() {
		super("The observable's Context reference is stale");
	}
}
