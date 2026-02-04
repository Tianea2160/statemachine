package org.project.hansan.statemachine.core

import org.project.hansan.statemachine.exception.InvalidTransitionException
import kotlin.reflect.KClass

/**
 * 상태 전이 결과.
 */
data class TransitionResult<S : State, C : Stateful<S, C>>(
    val previousState: S,
    val newState: S,
    val context: C,
) {
    val stateChanged: Boolean = previousState != newState
}

/**
 * 상태 머신 인터페이스.
 * @param S 상태 타입
 * @param E 이벤트 타입
 * @param C 컨텍스트 타입 (반드시 Stateful 구현 필요)
 */
interface StateMachine<S : State, E : Event, C : Stateful<S, C>> {
    /**
     * 이벤트를 처리하고 상태 전이 수행.
     * @param model 상태를 가진 모델
     * @param event 트리거 이벤트
     * @return TransitionResult (새 상태, 새 컨텍스트)
     * @throws InvalidTransitionException 유효한 전이가 없는 경우
     */
    fun fire(model: C, event: E): TransitionResult<S, C>

    /**
     * 현재 상태에서 특정 이벤트로 전이 가능한지 확인.
     */
    fun canFire(model: C, event: E): Boolean

    /**
     * 현재 상태에서 가능한 모든 이벤트 타입 반환.
     */
    fun availableEvents(model: C): Set<KClass<out E>>
}

/**
 * StateMachine 기본 구현.
 */
class DefaultStateMachine<S : State, E : Event, C : Stateful<S, C>>(
    private val transitions: List<Transition<S, E, C>>,
    private val onTransition: ((from: S, event: E, to: S) -> Unit)? = null,
) : StateMachine<S, E, C> {

    override fun fire(model: C, event: E): TransitionResult<S, C> {
        val currentState = model.state
        val transition = findTransition(currentState, event, model)
            ?: throw InvalidTransitionException(currentState, event)

        // Action 실행 후 상태 자동 업데이트
        val afterActions = transition.executeActions(model, event)
        val newContext = afterActions.withState(transition.target)

        onTransition?.invoke(currentState, event, transition.target)

        return TransitionResult(
            previousState = currentState,
            newState = transition.target,
            context = newContext,
        )
    }

    override fun canFire(model: C, event: E): Boolean =
        findTransition(model.state, event, model) != null

    override fun availableEvents(model: C): Set<KClass<out E>> =
        transitions
            .filter { it.source == model.state && it.guards.all { guard -> guard.evaluate(model) } }
            .map { it.eventType }
            .toSet()

    private fun findTransition(currentState: S, event: E, context: C): Transition<S, E, C>? =
        transitions.find { it.isApplicable(currentState, event, context) }
}
