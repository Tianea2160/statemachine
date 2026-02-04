package org.project.hansan.statemachine.core

/**
 * 전이 조건을 검사하는 함수형 인터페이스.
 * @param C 컨텍스트(도메인 객체) 타입
 */
fun interface Guard<C> {
    /**
     * 전이가 가능한지 검사.
     * @param context 현재 컨텍스트
     * @return true면 전이 허용, false면 전이 불가
     */
    fun evaluate(context: C): Boolean

    companion object {
        /** 항상 통과하는 Guard */
        fun <C> always(): Guard<C> = Guard { true }

        /** 항상 실패하는 Guard */
        fun <C> never(): Guard<C> = Guard { false }
    }
}

/** Guard AND 조합 */
infix fun <C> Guard<C>.and(other: Guard<C>): Guard<C> =
    Guard { context -> this.evaluate(context) && other.evaluate(context) }

/** Guard OR 조합 */
infix fun <C> Guard<C>.or(other: Guard<C>): Guard<C> =
    Guard { context -> this.evaluate(context) || other.evaluate(context) }

/** Guard NOT */
operator fun <C> Guard<C>.not(): Guard<C> =
    Guard { context -> !this.evaluate(context) }
