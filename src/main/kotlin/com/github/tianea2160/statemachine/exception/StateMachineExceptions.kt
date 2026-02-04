package com.github.tianea2160.statemachine.exception

import com.github.tianea2160.statemachine.core.Event
import com.github.tianea2160.statemachine.core.State

sealed class StateMachineException(message: String) : RuntimeException(message)

class InvalidTransitionException(
    val currentState: State,
    val event: Event,
) : StateMachineException(
    "No valid transition from state '$currentState' with event '${event::class.simpleName}'",
)
