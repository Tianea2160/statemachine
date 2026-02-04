package org.project.hansan.statemachine.core

import kotlin.reflect.KClass

/**
 * 단일 상태 전이 정의.
 * @param S 상태 타입
 * @param E 이벤트 타입
 * @param C 컨텍스트 타입 (반드시 Stateful 구현 필요)
 */
data class Transition<S : State, E : Event, C : Stateful<S, C>>(
    val source: S,
    val eventType: KClass<out E>,
    val target: S,
    val guards: List<Guard<C>> = emptyList(),
    val actions: List<Action<C, E>> = emptyList(),
) {
    /**
     * 이 전이가 적용 가능한지 검사.
     * 모든 Guard가 순차적으로 평가되며, 하나라도 실패하면 전이 불가.
     */
    fun isApplicable(currentState: S, triggeredEvent: E, context: C): Boolean =
        currentState == source &&
            eventType.isInstance(triggeredEvent) &&
            guards.all { it.evaluate(context) }

    /**
     * 모든 Action을 순차적으로 실행하고 최종 컨텍스트 반환.
     */
    fun executeActions(context: C, event: E): C =
        actions.fold(context) { acc, action -> action.execute(acc, event) }
}
