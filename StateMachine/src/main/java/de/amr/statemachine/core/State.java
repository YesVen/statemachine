package de.amr.statemachine.core;

import java.util.Objects;
import java.util.function.IntSupplier;

/**
 * Implementation of a state in a finite state machine.
 * 
 * @param <S>
 *          state type, normally some enum type
 * @param <E>
 *          event type, e.g. event classes with different attributes
 * 
 * @author Armin Reichert
 */
public class State<S, E> {

	/** Constant for defining an unlimited duration. */
	public static final int ENDLESS = Integer.MAX_VALUE;

	/** The label used to identify this state. */
	S id;

	/** The state machine this state belongs to. */
	StateMachine<S, E> machine;

	/** The client code executed when entering this state. */
	Runnable entryAction;

	/** The client code executed when a tick occurs for this state. */
	Runnable tickAction;

	/** The client code executed when leaving this state. */
	Runnable exitAction;

	/** Function providing the timer duration for this state. */
	IntSupplier fnTimer;

	/** The number of ticks this state will be active. */
	int duration;

	/** Ticks remaining until time-out */
	int ticksRemaining;

	private void runAction(Runnable actionOrNull) {
		if (actionOrNull != null) {
			actionOrNull.run();
		}
	}

	protected State() {
		fnTimer = () -> ENDLESS;
		resetTimer();
	}

	@Override
	public String toString() {
		String s = String.format("(%s:%s", machine.getDescription(), id);
		if (entryAction != null)
			s += " entry";
		if (tickAction != null)
			s += " tick";
		if (exitAction != null)
			s += " exit";
		s += ")";
		return s;
	}

	public S id() {
		return id;
	}

	public void setOnEntry(Runnable action) {
		Objects.requireNonNull(action);
		entryAction = action;
	}

	public void onEntry() {
		runAction(entryAction);
	}

	public void setOnExit(Runnable action) {
		Objects.requireNonNull(action);
		exitAction = action;
	}

	public void onExit() {
		runAction(exitAction);
	}

	public void setOnTick(Runnable action) {
		Objects.requireNonNull(action);
		tickAction = action;
	}

	public void onTick() {
		runAction(tickAction);
	}

	/** Tells if this state has timed out. */
	public boolean isTerminated() {
		return ticksRemaining == 0;
	}

	/** Resets the timer to the complete state duration. */
	public void resetTimer() {
		if (fnTimer == null) {
			throw new IllegalStateException(String.format("Timer function is NULL in state '%s'", id));
		}
		ticksRemaining = duration = fnTimer.getAsInt();
	}

	boolean updateTimer() {
		if (duration != ENDLESS && ticksRemaining > 0) {
			--ticksRemaining;
			if (ticksRemaining == 0) {
				return true; // timeout
			}
		}
		return false;
	}

	/**
	 * Sets a timer function for this state and resets the timer.
	 * 
	 * @param fnTimer
	 *                  function providing the time for this state
	 */
	public void setTimerFunction(IntSupplier fnTimer) {
		Objects.requireNonNull(fnTimer);
		this.fnTimer = fnTimer;
		resetTimer();
	}

	/**
	 * Sets a constant timer function for this state and resets the timer.
	 * 
	 * @param fixedTime
	 *                    constant time for this state
	 */
	public void setConstantTimer(int fixedTime) {
		setTimerFunction(() -> fixedTime);
	}

	/**
	 * Returns the duration of this state (in ticks).
	 * 
	 * @return the state duration (number of updates until this state times out)
	 */
	public int getDuration() {
		return duration;
	}

	/**
	 * Returns the number of updates until this state will time out.
	 * 
	 * @return the number of updates until timeout occurs
	 */
	public int getTicksRemaining() {
		return ticksRemaining;
	}

	/**
	 * The number of updates since the (optional) timer for this state was started or reset.
	 * 
	 * @return number of updates since timer started or reset
	 */
	public int getTicksConsumed() {
		return duration == ENDLESS ? 0 : duration - ticksRemaining;
	}
}