package isabelle.eclipse.editors;

import java.util.HashMap;
import java.util.Map;

public class HashMapWithDefault<K, V> extends HashMap<K, V> {

	private V defaultValue = null;
	
	public HashMapWithDefault() {
	}

	public HashMapWithDefault(int initialCapacity) {
		super(initialCapacity);
	}

	public HashMapWithDefault(Map<? extends K, ? extends V> m) {
		super(m);
	}

	public HashMapWithDefault(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	public V getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(V defaultValue) {
		this.defaultValue = defaultValue;
	}

	@Override
	public V get(Object key) {
		V value = super.get(key);
		if (value != null) {
			return value;
		}
		
		return getDefaultValue();
	}
	
	

}
