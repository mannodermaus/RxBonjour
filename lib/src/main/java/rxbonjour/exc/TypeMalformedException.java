package rxbonjour.exc;

/**
 * @author marcel
 */
public class TypeMalformedException extends RuntimeException {

	public TypeMalformedException(String type) {
		super("The following is not a valid Bonjour type: " + type);
	}
}
