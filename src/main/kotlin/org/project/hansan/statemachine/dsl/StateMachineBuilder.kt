package org.project.hansan.statemachine.dsl

import org.project.hansan.statemachine.core.Action
import org.project.hansan.statemachine.core.DefaultStateMachine
import org.project.hansan.statemachine.core.Event
import org.project.hansan.statemachine.core.Guard
import org.project.hansan.statemachine.core.State
import org.project.hansan.statemachine.core.StateMachine
import org.project.hansan.statemachine.core.Stateful
import org.project.hansan.statemachine.core.Transition
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * 상태 머신을 선언적으로 정의하기 위한 DSL 빌더.
 *
 * 사용 예시:
 * ```kotlin
 * val machine = stateMachine<Status, MyEvent, Context> {
 *     from(Status.DRAFT) {
 *         on<PublishEvent>() goto Status.PUBLISHED
 *         on<ArchiveEvent>() goto Status.ARCHIVED guardedBy { it.canArchive }
 *     }
 *     from(Status.PUBLISHED) {
 *         on<ArchiveEvent>() goto Status.ARCHIVED action { ctx, _ ->
 *             ctx.copy(archivedAt = Instant.now())
 *         }
 *     }
 * }
 * ```
 */
@DslMarker
annotation class StateMachineDsl

@StateMachineDsl
class StateMachineBuilder<S : State, E : Event, C : Stateful<S, C>> {
    private val transitions = mutableListOf<Transition<S, E, C>>()
    private var onTransition: ((S, E, S) -> Unit)? = null

    /**
     * 특정 소스 상태에서의 전이 정의.
     */
    fun from(state: S, block: StateBuilder<S, E, C>.() -> Unit) {
        val stateBuilder = StateBuilder<S, E, C>(state)
        stateBuilder.block()
        transitions.addAll(stateBuilder.build())
    }

    /**
     * 전이 후 콜백 등록 (로깅, 이벤트 발행 등).
     */
    fun onTransition(callback: (from: S, event: E, to: S) -> Unit) {
        onTransition = callback
    }

    fun build(): StateMachine<S, E, C> = DefaultStateMachine(
        transitions = transitions.toList(),
        onTransition = onTransition,
    )
}

@StateMachineDsl
class StateBuilder<S : State, E : Event, C : Stateful<S, C>>(
    @PublishedApi internal val source: S,
) {
    @PublishedApi
    internal val transitionBuilders = mutableListOf<TransitionConfig<S, E, C, out E>>()

    /**
     * 특정 이벤트에 대한 전이 정의 시작.
     */
    inline fun <reified T : E> on(): TransitionStart<S, E, C, T> =
        TransitionStart(source, T::class) { transitionBuilders.add(it) }

    fun build(): List<Transition<S, E, C>> = transitionBuilders.map { it.build() }
}

@StateMachineDsl
class TransitionStart<S : State, E : Event, C : Stateful<S, C>, T : E>(
    private val source: S,
    private val eventClass: KClass<T>,
    private val onComplete: (TransitionConfig<S, E, C, T>) -> Unit,
) {
    /**
     * 타겟 상태 지정.
     */
    infix fun goto(target: S): TransitionConfig<S, E, C, T> {
        val config = TransitionConfig<S, E, C, T>(source, eventClass, target)
        onComplete(config)
        return config
    }
}

@StateMachineDsl
class TransitionConfig<S : State, E : Event, C : Stateful<S, C>, T : E>(
    private val source: S,
    private val eventClass: KClass<T>,
    private val target: S,
) {
    private val guards = mutableListOf<Guard<C>>()
    private val actions = mutableListOf<Action<C, E>>()

    /**
     * 단일 Guard 조건 추가 (기존 API 호환).
     */
    infix fun guardedBy(predicate: (C) -> Boolean): TransitionConfig<S, E, C, T> {
        guards.add(Guard(predicate))
        return this
    }

    /**
     * 여러 Guard 조건을 순차적으로 추가.
     */
    infix fun guards(block: GuardsBuilder<C>.() -> Unit): TransitionConfig<S, E, C, T> {
        val builder = GuardsBuilder<C>()
        builder.block()
        guards.addAll(builder.build())
        return this
    }

    /**
     * 단일 전이 동작 추가 (기존 API 호환).
     * 이벤트 타입은 on<T>()에서 이미 검증되었으므로 캐스팅이 안전함.
     */
    infix fun action(block: (C, T) -> C): TransitionConfig<S, E, C, T> {
        actions.add(Action { context, event -> block(context, eventClass.cast(event)) })
        return this
    }

    /**
     * 여러 전이 동작을 순차적으로 추가.
     */
    infix fun actions(block: ActionsBuilder<C, E, T>.() -> Unit): TransitionConfig<S, E, C, T> {
        val builder = ActionsBuilder<C, E, T>(eventClass)
        builder.block()
        actions.addAll(builder.build())
        return this
    }

    fun build(): Transition<S, E, C> = Transition(
        source = source,
        eventType = eventClass,
        target = target,
        guards = guards.toList(),
        actions = actions.toList(),
    )
}

@StateMachineDsl
class GuardsBuilder<C> {
    private val guards = mutableListOf<Guard<C>>()

    /**
     * Guard 추가.
     */
    fun guard(predicate: (C) -> Boolean) {
        guards.add(Guard(predicate))
    }

    /**
     * Guard 객체 직접 추가.
     */
    operator fun Guard<C>.unaryPlus() {
        guards.add(this)
    }

    fun build(): List<Guard<C>> = guards.toList()
}

@StateMachineDsl
class ActionsBuilder<C, E : Event, T : E>(
    private val eventClass: KClass<T>,
) {
    private val actions = mutableListOf<Action<C, E>>()

    /**
     * Action 추가.
     * 이벤트 타입은 on<T>()에서 이미 검증되었으므로 캐스팅이 안전함.
     */
    fun action(block: (C, T) -> C) {
        actions.add(Action { context, event -> block(context, eventClass.cast(event)) })
    }

    /**
     * Action 객체 직접 추가.
     */
    operator fun Action<C, E>.unaryPlus() {
        actions.add(this)
    }

    fun build(): List<Action<C, E>> = actions.toList()
}

/**
 * DSL 진입점 함수.
 */
inline fun <reified S : State, reified E : Event, C : Stateful<S, C>> stateMachine(
    block: StateMachineBuilder<S, E, C>.() -> Unit,
): StateMachine<S, E, C> {
    val builder = StateMachineBuilder<S, E, C>()
    builder.block()
    return builder.build()
}
