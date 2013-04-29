package dict;

public final class Pair<F, S> {
	final private F first;
	final private S second;

	public Pair(F first, S second) {
		super();
		this.first = first;
		this.second = second;
	}

	public final F getFirst() {
		return first;
	}

	public final S getSecond() {
		return second;
	}

	@Override
	public final String toString() {
		return String.format("(%s, %s)", first, second);
	}

	@Override
	public int hashCode() {
		return (first == null ? 0 : first.hashCode()) ^ (second == null ? 0 : second.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Pair))
			return false;
		final Pair<?, ?> other = (Pair<?, ?>) obj;
		if (first == null) {
			if (other.first != null)
				return false;
		} else if (!first.equals(other.first))
			return false;
		if (second == null) {
			if (other.second != null)
				return false;
		} else if (!second.equals(other.second))
			return false;
		return true;
	}
}
