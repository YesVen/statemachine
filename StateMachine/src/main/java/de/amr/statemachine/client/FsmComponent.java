package de.amr.statemachine.client;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;

import de.amr.statemachine.State;
import de.amr.statemachine.StateMachine;

/**
 * Prototypical implementation of the {@link FsmControlled} interface which can
 * be used as a delegate by an entity class.
 * <p>
 * When an entity cannot inherit directly from the {@link StateMachine} class,
 * it can implement the {@link FsmContainer} interface which delegates to an
 * instance of this class.
 * 
 * @author Armin Reichert
 *
 * @param <S> state type of the finite-state machine
 * @param <E> event type of the finite-state machine
 * 
 */
public class FsmComponent<S, E> implements FsmControlled<S, E> {

	public final StateMachine<S, E> fsm;
	public final Set<Consumer<E>> listeners;
	public Predicate<E> publishedEventIsLogged;
	public Logger logger;

	public FsmComponent(StateMachine<S, E> fsm) {
		this.fsm = fsm;
		publishedEventIsLogged = event -> true;
		listeners = new LinkedHashSet<>();
		logger = Logger.getGlobal();
	}

	@Override
	public StateMachine<S, E> fsm() {
		return fsm;
	}

	@Override
	public String name() {
		return fsm.getDescription();
	}

	@Override
	public void addEventListener(Consumer<E> listener) {
		listeners.add(listener);
	}

	@Override
	public void removeEventListener(Consumer<E> listener) {
		listeners.remove(listener);
	}

	public void publish(E event) {
		if (publishedEventIsLogged.test(event)) {
			logger.info(() -> String.format("%s published event '%s'", name(), event));
		}
		listeners.forEach(listener -> listener.accept(event));
	}

	@Override
	public S getState() {
		return fsm.getState();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean is(S... states) {
		return fsm.is(states);
	}

	@Override
	public void setState(S state) {
		fsm.setState(state);
	}

	@Override
	public State<S, E> state() {
		return fsm.state();
	}

	@Override
	public void process(E event) {
		fsm.process(event);
	}

	@Override
	public void init() {
		fsm.init();
	}

	@Override
	public void update() {
		fsm.update();
	}
}