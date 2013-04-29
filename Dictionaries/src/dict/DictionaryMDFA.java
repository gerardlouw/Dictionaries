package dict;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;

public class DictionaryMDFA extends DictionaryDFA {
	
	public DictionaryMDFA(Type t) {
		super(t);
	}
	
	public DictionaryMDFA(Type t, Collection<String> c) {
		super(t, c);
	}
	
	@Override
	public boolean add(String e) {
		boolean add = super.add(e);
		hopcroftMinimize();
		return add;
	}
	
	@Override
	public boolean addAll(final Collection<? extends String> c) {
		boolean any = false;
		for (final String s : c) {
			if (super.add(s)) {
				any = true;
			}
		}
		hopcroftMinimize();
		return any;
	}

	private final void hopcroftMinimize() {
		final Set<State> acceptStates = new HashSet<State>();
		final Set<State> nonAcceptStates = new HashSet<State>();
		final Iterator<State> it = stateIterator();
		while (it.hasNext()) {
			State s = it.next();
			if (s.accept) {
				acceptStates.add(s);
			} else {
				nonAcceptStates.add(s);
			}
		}

		final LinkedList<Set<State>> partition = new LinkedList<Set<State>>();
		final LinkedList<Set<State>> untreated = new LinkedList<Set<State>>();
		if (acceptStates.size() < nonAcceptStates.size()) {
			partition.addLast(nonAcceptStates);
			partition.addLast(acceptStates);
			untreated.addLast(acceptStates);
		} else {
			partition.addLast(acceptStates);
			partition.addLast(nonAcceptStates);
			untreated.addLast(nonAcceptStates);
		}

		while (!untreated.isEmpty()) {
			Set<State> S = untreated.removeFirst();
			for (char c = 'a'; c <= 'z'; c++) {
				ListIterator<Set<State>> lit = partition.listIterator();
				while (lit.hasNext()) {
					final Set<State> P = lit.next();
					final Set<State> statesToS = new HashSet<State>();
					final Set<State> statesNotToS = new HashSet<State>();
					for (State s : P) {
						final Map<Character, State> m = transitionMap.get(s);
						if (m == null) {
							statesNotToS.add(s);
							continue;
						}
						State t = m.get(c);
						if (t == null) {
							statesNotToS.add(s);
							continue;
						}
						(S.contains(t) ? statesToS : statesNotToS).add(s);							
					}
					lit.remove();
					if (!statesToS.isEmpty())
						lit.add(statesToS);
					if (!statesNotToS.isEmpty())
						lit.add(statesNotToS);
					if (statesToS.size() < statesNotToS.size()) {
						if (statesToS.size() != 0)
							untreated.addLast(statesToS);
					} else {
						if (statesNotToS.size() != 0)
							untreated.addLast(statesNotToS);
					}
					
				}
			}
		}
		
		final Map<State, State> representitives = new HashMap<State, State>();
		for (final Set<State> p : partition) {
			final State representitive = p.iterator().next();
			for (final State s : p) {
				representitives.put(s, representitive);
			}
		}
		for (final Set<State> p : partition) {
			for (final State s : p) {
				if (s != representitives.get(s)) {
					transitionMap.remove(s);
					numStates--;
				}
				final Map<Character, State> m = transitionMap.get(s);
				if (m != null) {
					for (Entry<Character, State> e : m.entrySet()) {
						final State u = representitives.get(e.getValue());
						if (u != null && e.getValue() != u) {
							m.put(e.getKey(), u);
						}
					}
				}
			}
		}
	}

	@SuppressWarnings("unused")
	private final void mooreMinimize() {
		final ArrayList<State> states = new ArrayList<State>();
		final Iterator<State> it = stateIterator();
		while(it.hasNext()) {
			State s = it.next();
			states.add(s);
		}
		final Set<Pair<State, State>> marked = new HashSet<Pair<State, State>>();
		final Set<Pair<State, State>> unmarked = new HashSet<Pair<State, State>>();
		for (int i = 0; i < states.size(); i++)
			for (int j = i + 1; j < states.size(); j++) {
				Pair<State, State> p = new Pair<State, State>(states.get(i),
						states.get(j));
				(p.getFirst().accept ^ p.getSecond().accept ? marked : unmarked)
						.add(p);
			}
		final Set<Pair<State, State>> changes = new HashSet<Pair<State, State>>();
		do {
			changes.clear();
			for (final Pair<State, State> p : unmarked) {
				final Map<Character, State> firstMap = transitionMap.get(p
						.getFirst());
				final Map<Character, State> secondMap = transitionMap.get(p
						.getSecond());
				if (firstMap != null && secondMap != null) {
					for (final Entry<Character, State> e : firstMap.entrySet()) {
						final State s = e.getValue(), t = secondMap.get(e
								.getKey());
						if (t == null
								|| marked
										.contains(new Pair<State, State>(s, t))
								|| marked
										.contains(new Pair<State, State>(t, s))) {
							changes.add(p);
							marked.add(p);
						}
					}
				} else if (firstMap != null || secondMap != null) {
					changes.add(p);
					marked.add(p);
				}
			}
			unmarked.removeAll(changes);
		} while (!changes.isEmpty());
		final Map<State, State> representitives = new HashMap<State, State>();
		final Map<State, Set<State>> equivalences = new HashMap<State, Set<State>>();
		for (Pair<State, State> p : unmarked) {
			final State s = p.getFirst(), t = p.getSecond();
			State S = representitives.get(s), T = representitives.get(t);
			if (T == null) {
				if (S == null) {
					representitives.put(s, S = s);
					final Set<State> equivalence = new HashSet<State>();
					equivalence.add(s);
					equivalences.put(S, equivalence);
				}
				representitives.put(t, S);
				equivalences.get(S).add(t);
			} else if (S == null) {
				representitives.put(s, T);
				equivalences.get(T).add(s);
			} else if (S != T) {
				final Set<State> equivalence = equivalences.remove(T);
				for (final State u : equivalence) {
					representitives.put(u, S);
				}
				equivalences.get(S).addAll(equivalence);
			}
		}
		final Set<State> C = new HashSet<State> ();
		final Set<State> S = new HashSet<State> (representitives.values());
		for (State s : S) {
			final Set<State> P = new HashSet<State>();
			for (Pair<State, State> p : unmarked) {
				if (p.getFirst() == s) P.add(p.getSecond());
				else if (p.getSecond() == s) P.add(p.getFirst()); 
			}
			final Set<State> M = new HashSet<State>();
			for (Entry<State, State> e : representitives.entrySet()) {
				if (e.getValue() == s) M.add(e.getKey());
			}
			M.remove(s);
			if (!P.equals(M))
				for (Entry<State, State> e : representitives.entrySet()) {
					if (e.getValue() == s) C.add(e.getKey());
				}
		}
		for (final State s : C) {
			representitives.remove(s);
		}
		
		for (final State s : states) {
			final State t = representitives.get(s);
			if (t != null && s != t) {
				transitionMap.remove(s);
				numStates--;
			}
			final Map<Character, State> m = transitionMap.get(s);
			if (m != null) {
				for (Entry<Character, State> e : m.entrySet()) {
					final State u = representitives.get(e.getValue());
					if (u != null && e.getValue() != u) {
						m.put(e.getKey(), u);
					}
				}
			}
		}
	}

	private final Iterator<State> stateIterator() {
		return new Iterator<State>() {
			private final Stack<State> states;

			{
				states = new Stack<State>();
				states.push(start);
			}

			@Override
			public boolean hasNext() {
				return !states.isEmpty();
			}

			@Override
			public State next() {
				State s = states.pop();
				final Map<Character, State> m = transitionMap.get(s);
				if (m != null) {
					for (Entry<Character, State> e : m.entrySet()) {
						states.push(e.getValue());
					}
				}
				return s;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
	public static void main(String[] args) {
		Type t = Type.HASH;
		if (args.length != 0) {
			if (args.length == 1 && args[1].equals("tree")) {
				t = Type.TREE;
			} else if (args.length != 1 || !args[1].equals("hash")) {
				System.out.println("Usage: java DictionaryMDFA [tree|hash]");
			}
		}
		LinkedList<String> l = new LinkedList<String>();
		Scanner s = new Scanner(System.in);
		while (s.hasNextLine()) {
			l.addLast(s.nextLine());
		}
		s.close();
		DictionaryMDFA dfa = new DictionaryMDFA(t, l);
		System.out.println(dfa);
	}
}
