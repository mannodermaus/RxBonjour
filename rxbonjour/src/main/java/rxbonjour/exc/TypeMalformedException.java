package rxbonjour.exc;

/**
 * Thrown when a requested type doesn't conform to the DNS-SD format
 */
public class TypeMalformedException extends RuntimeException {

	public TypeMalformedException(String type) {
		super("The following is not a valid Bonjour type: " + type);
	}
}
