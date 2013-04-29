package dict;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Stack;
import java.util.TreeMap;

public class DictionaryDFA implements Collection<String> {

	public static enum Type {
		HASH, TREE;
	}
	
	final private Type type;
	final Map<State, Map<Character, State>> transitionMap;
	final State start;
	private int size;
	int numStates; //XXX for experiments
	
	public DictionaryDFA(Type type) {
		switch(this.type = type) {
		case HASH:
			transitionMap = new HashMap<State, Map<Character, State>>();
			break;
		case TREE:
			transitionMap = new TreeMap<State, Map<Character, State>>();
			break;
		default:
			transitionMap = null;
		}
		start = new State();
		size = 0;
		numStates = 1;
	}

	public DictionaryDFA(Type t, Collection<String> c) {
		this(t);
		addAll(c);
	}

	public final int size() {
		return size;
	}

	@Override
	public final boolean isEmpty() {
		return size == 0;
	}

	@Override
	public final void clear() {
		transitionMap.clear();
		size = 0;
	}
	
	@Override
	public boolean add(final String e) {
		State s = start;
		for (final Character c : e.toCharArray()) {
			Map<Character, State> m = transitionMap.get(s);
			if (m == null) {
				switch (type) {
				case HASH:
					m = new HashMap<Character, State>();
					break;
				case TREE:
					m = new TreeMap<Character, State>();
					break;
				}
				transitionMap.put(s, m);
			}
			s = m.get(c);
			if (s == null) {
				m.put(c, s = new State());
				numStates++;
			}
		}
		if (!s.accept) {
			s.accept = true;
			size++;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean addAll(final Collection<? extends String> c) {
		boolean any = false;
		for (final String s : c) {
			if (add(s)) {
				any = true;
			}
		}
		return any;
	}

	@Override
	public final boolean remove(final Object o) {
		if (!(o instanceof String)) {
			return false;
		}
		final String word = (String) o;
		final Stack<State> states = new Stack<State>();
		final Stack<Character> symbols = new Stack<Character>();
		State s = start;
		states.push(s);
		for (Character c : word.toCharArray()) {
			final Map<Character, State> m = transitionMap.get(s);
			if (m == null) {
				return false;
			}
			s = m.get(c);
			if (s == null) {
				return false;
			}
			states.push(s);
			symbols.push(c);
		}
		if (s.accept) {
			s.accept = false;
			State t = states.pop();
			Map<Character, State> m = transitionMap.get(t);
			while (!states.isEmpty() && !t.accept && m == null) {
				final State u = states.pop();
				final Map<Character, State> n = transitionMap.get(u);
				n.remove(symbols.pop());
				numStates--;
				if (n.isEmpty()) {
					transitionMap.remove(u);
					m = null;
				} else {
					m = n;
				}
				t = u;
			}
			size--;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public final boolean removeAll(Collection<?> c) {
		boolean any = false;
		for (final Object o : c) {
			if (remove(o)) {
				any = true;
			}
		}
		return any;
	}

	@Override
	public final boolean contains(final Object o) {
		if (!(o instanceof String)) {
			return false;
		}
		final String word = (String) o;
		State s = start;
		for (final Character c : word.toCharArray()) {
			final Map<Character, State> m = transitionMap.get(s);
			if (m == null) {
				return false;
			}
			s = m.get(c);
			if (s == null) {
				return false;
			}
		}
		return s.accept;
	}

	@Override
	public final boolean containsAll(Collection<?> c) {
		for (final Object o : c) {
			if (!contains(o)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public final boolean retainAll(Collection<?> c) {
		DictionaryDFA d = new DictionaryDFA(type);
		for (Object o : c)
			if (contains(o))
				d.add((String) o);
		boolean any = size != d.size;
		clear();
		for (String s : d) {
			add(s);
		}
		return any;
	}

	@Override
	public final Iterator<String> iterator() {
		return new Iterator<String>() {
			private final Stack<State> states;
			private final Stack<String> words;
			private String last;

			{
				states = new Stack<State>();
				words = new Stack<String>();
				last = null;
				states.push(start);
				words.push("");
			}

			@Override
			public boolean hasNext() {
				return !states.isEmpty();
			}

			@Override
			public String next() {
				State s;
				String w;
				do {
					s = states.pop();
					w = words.pop();
					final Map<Character, State> m = transitionMap.get(s);
					if (m != null) {
						for (Entry<Character, State> e : m.entrySet()) {
							states.push(e.getValue());
							words.push(w + e.getKey());
						}
					}
				} while (!s.accept);
				return last = w;
			}

			@Override
			public void remove() {
				DictionaryDFA.this.remove(last);
			}
		};
	}

	@Override
	public final String toString() {
		if (size == 0)
			return "{\n}";
		final StringBuilder sb = new StringBuilder();
		sb.append('{');
		for (String s : this) {
			sb.append("\n    \"").append(s).append("\",");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append('\n');
		sb.append('}');
		return sb.toString();
	}

	@Override
	public final Object[] toArray() {
		Object[] a = new Object[size()];
		int i = 0;
		for (String s : this) {
			a[i++] = s;
		}
		return a;
	}

	@SuppressWarnings("unchecked")
	@Override
	public final <T> T[] toArray(T[] a) {
		a = (a.length >= size() ? a : Arrays.copyOf(a, size()));
		int i = 0;
		for (String s : this) {
			a[i++] = (T) s;
		}
		return a;
	}

	final static class State implements Comparable<State> {
		boolean accept;

		@Override
		public int hashCode() {
			return System.identityHashCode(this);
		}

		@Override
		public boolean equals(Object obj) {
			return obj == this;
		}

		@Override
		public String toString() {
			return String.format("%d", hashCode());
		}

		@Override
		public int compareTo(State o) {
			return Integer.compare(System.identityHashCode(this),
					System.identityHashCode(o));
		}
	}

	public static void main(String[] args) {
		Type t = Type.HASH;
		if (args.length != 0) {
			if (args.length == 1 && args[1].equals("tree")) {
				t = Type.TREE;
			} else if (args.length != 1 || !args[1].equals("hash")) {
				System.out.println("Usage: java DictionaryDFA [tree|hash]");
			}
		}
		LinkedList<String> l = new LinkedList<String>();
		Scanner s = new Scanner(System.in);
		while (s.hasNextLine()) {
			l.addLast(s.nextLine());
		}
		s.close();
		DictionaryDFA dfa = new DictionaryDFA(t, l);
		System.out.println(dfa);
	}
}