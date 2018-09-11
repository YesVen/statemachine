package de.amr.statemachine;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Builder for state machine instances.
 * 
 * @author Armin Reichert
 *
 * @param <S>
 *          Type for identifying states
 * @param <E>
 *          Type of events
 */
public class StateMachineBuilder<S, E> {

	private StateMachine<S, E> sm;
	private String description;
	private S initialState;

	public StateMachineBuilder(Class<S> stateLabelType, Match matchStrategy) {
		sm = new StateMachine<>(stateLabelType, matchStrategy);
	}

	public StateMachineBuilder(Class<S> stateLabelType) {
		sm = new StateMachine<>(stateLabelType);
	}
	
	public StateMachineBuilder(StateMachine<S,E> sm) {
		this.sm = sm;
	}

	public StateMachineBuilder<S, E> description(String description) {
		this.description = description != null ? description : getClass().getSimpleName();
		return this;
	}

	public StateMachineBuilder<S, E> initialState(S initialState) {
		this.initialState = initialState;
		return this;
	}

	public StateBuilder states() {
		return new StateBuilder();
	}

	public class StateBuilder {

		private S state;
		private Runnable entry;
		private Runnable exit;
		private Runnable update;
		private IntSupplier fnDuration;

		private void clear() {
			state = null;
			entry = null;
			exit = null;
			update = null;
			fnDuration = null;
		}

		public StateBuilder state(S state) {
			// commit previous build if any
			if (this.state != null) {
				commit();
			}
			clear();
			// start new build
			this.state = state;
			return this;
		}

		public <C extends State<S, E>> StateBuilder impl(C customStateObject) {
			if (customStateObject == null) {
				throw new IllegalArgumentException("Custom state object cannot be NULL");
			}
			sm.realizeState(state, customStateObject);
			return this;
		}

		public StateBuilder timeoutAfter(IntSupplier fnDuration) {
			if (fnDuration == null) {
				throw new IllegalStateException("Timer function cannot be null for state " + state);
			}
			this.fnDuration = fnDuration;
			return this;
		}

		public StateBuilder onEntry(Runnable entry) {
			this.entry = entry;
			return this;
		}

		public StateBuilder onExit(Runnable exit) {
			this.exit = exit;
			return this;
		}

		public StateBuilder onTick(Runnable update) {
			this.update = update;
			return this;
		}

		private StateBuilder commit() {
			State<S, E> stateObject = sm.state(state);
			stateObject.entry = entry;
			stateObject.exit = exit;
			stateObject.update = update;
			// fnDuration could have alreday been set via API if this is a subclassed state!
			if (stateObject.fnDuration == null) {
				stateObject.fnDuration = fnDuration != null ? fnDuration : () -> State.ENDLESS;
			}
			return this;
		}

		public TransitionBuilder transitions() {
			// commit previous build if any
			if (this.state != null) {
				commit();
			}
			return new TransitionBuilder();
		}
	}

	public class TransitionBuilder {
		
		private boolean started;
		private S from;
		private S to;
		private BooleanSupplier guard;
		private boolean timeout;
		private E event;
		private Class<? extends E> eventType;
		private Consumer<E> action;

		private void clear() {
			started = false;
			from = null;
			to = null;
			guard = null;
			timeout = false;
			event = null;
			eventType = null;
			action = null;
		}

		public TransitionBuilder stay(S state) {
			return when(state);
		}

		public TransitionBuilder when(S from) {
			if (from == null) {
				throw new IllegalArgumentException("Transition source state must not be NULL");
			}
			if (this.from != null) {
				commit();
			}
			clear();
			started = true;
			this.from = this.to = from;
			return this;
		}

		public TransitionBuilder then(S to) {
			if (to == null) {
				throw new IllegalArgumentException("Transition target state must not be NULL");
			}
			if (!started) {
				throw new IllegalArgumentException("Transition building must be started with when(...) or stay(...)");
			}
			this.to = to;
			return this;
		}

		public TransitionBuilder condition(BooleanSupplier guard) {
			if (guard == null) {
				throw new IllegalArgumentException("Transition guard must not be NULL");
			}
			if (!started) {
				throw new IllegalArgumentException("Transition building must be started with when(...) or stay(...)");
			}
			this.guard = guard;
			return this;
		}

		public TransitionBuilder onTimeout() {
			if (timeout) {
				throw new IllegalArgumentException("Transition timeout must only be set once");
			}
			if (!started) {
				throw new IllegalArgumentException("Transition building must be started with when(...) or stay(...)");
			}
			this.timeout = true;
			return this;
		}

		public TransitionBuilder on(Class<? extends E> eventType) {
			if (eventType == null) {
				throw new IllegalArgumentException("Event type of transition must not be NULL");
			}
			if (!started) {
				throw new IllegalArgumentException("Transition building must be started with when(...) or stay(...)");
			}
			this.eventType = eventType;
			return this;
		}

		public TransitionBuilder on(E eventObject) {
			if (eventObject == null) {
				throw new IllegalArgumentException("Event object of transition must not be NULL");
			}
			if (!started) {
				throw new IllegalArgumentException("Transition building must be started with when(...) or stay(...)");
			}
			this.event = eventObject;
			return this;
		}

		public TransitionBuilder act(Consumer<E> action) {
			if (action == null) {
				throw new IllegalArgumentException("Transition action must not be NULL");
			}
			if (!started) {
				throw new IllegalArgumentException("Transition building must be started with when(...) or stay(...)");
			}
			this.action = action;
			return this;
		}

		public TransitionBuilder act(Runnable action) {
			if (action == null) {
				throw new IllegalArgumentException("Transition action must not be NULL");
			}
			if (!started) {
				throw new IllegalArgumentException("Transition building must be started with when(...) or stay(...)");
			}
			this.action = e -> action.run();
			return this;
		}

		private TransitionBuilder commit() {
			if (timeout && event != null) {
				throw new IllegalStateException(
						"Cannot specify both timeout and event object for the same transition");
			}
			if (timeout && eventType != null) {
				throw new IllegalStateException("Cannot specify both timeout and event type for the same transition");
			}
			if (timeout) {
				sm.addTransitionOnTimeout(from, to, guard, action);
			} else  {
				sm.addTransition(from, to, guard, action, event, eventType, false);
			}
			clear();
			return this;
		}

		public StateMachine<S, E> endStateMachine() {
			if (from != null) {
				commit();
			}
			sm.setDescription(description == null ? sm.getClass().getSimpleName() : description);
			sm.setInitialState(initialState);
			return sm;
		}
	}
}