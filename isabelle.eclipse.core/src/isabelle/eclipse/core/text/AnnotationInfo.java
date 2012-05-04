package isabelle.eclipse.core.text;

import isabelle.Text.Range;

/**
 * Abstract representation of an annotation, carrying its type, position (range) and message.
 * 
 * @author Andrius Velykis
 */
public class AnnotationInfo {
	
	private final IsabelleAnnotation type;
	private final Range range;
	private final String message;
	
	public AnnotationInfo(IsabelleAnnotation type, Range range) {
		this(type, range, null);
	}
	
	public AnnotationInfo(IsabelleAnnotation type, Range range, String message) {
		this.type = type;
		this.range = range;
		this.message = message;
	}

	public IsabelleAnnotation getType() {
		return type;
	}

	public Range getRange() {
		return range;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + ((range == null) ? 0 : range.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AnnotationInfo other = (AnnotationInfo) obj;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (range == null) {
			if (other.range != null)
				return false;
		} else if (!range.equals(other.range))
			return false;
		if (type != other.type)
			return false;
		return true;
	}
}