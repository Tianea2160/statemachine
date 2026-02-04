package com.github.tianea2160.statemachine.core

/**
 * 전이 시 실행되는 동작 함수형 인터페이스.
 * @param C 컨텍스트 타입
 * @param E 이벤트 타입
 */
fun interface Action<C, E : Event> {
    /**
     * 전이 동작 실행.
     * @param context 현재 컨텍스트
     * @param event 트리거된 이벤트
     * @return 새로운 컨텍스트 (불변 객체이므로 copy 반환)
     */
    fun execute(context: C, event: E): C

    companion object {
        /** 아무 동작도 하지 않고 컨텍스트 그대로 반환 */
        fun <C, E : Event> noOp(): Action<C, E> = Action { context, _ -> context }
    }
}

/** Action 체이닝 - 순차 실행 */
infix fun <C, E : Event> Action<C, E>.then(next: Action<C, E>): Action<C, E> =
    Action { context, event -> next.execute(this.execute(context, event), event) }
