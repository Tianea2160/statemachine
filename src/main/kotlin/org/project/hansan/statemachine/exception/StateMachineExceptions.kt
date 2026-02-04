package org.project.hansan.statemachine.exception

import org.project.hansan.statemachine.core.Event
import org.project.hansan.statemachine.core.State

sealed class StateMachineException(message: String) : RuntimeException(message)

class InvalidTransitionException(
    val currentState: State,
    val event: Event,
) : StateMachineException(
    "No valid transition from state '$currentState' with event '${event::class.simpleName}'",
)
