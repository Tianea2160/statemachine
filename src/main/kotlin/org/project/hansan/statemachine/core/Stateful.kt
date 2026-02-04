package org.project.hansan.statemachine.core

/**
 * 상태를 가진 모델을 위한 추상화 인터페이스.
 * 도메인 모델이 이 인터페이스를 구현하면 상태 머신과 통합 가능.
 *
 * @param S 상태 타입 (State를 구현한 enum 등)
 * @param Self 자기 자신 타입 (Self-type 패턴)
 *
 * 사용 예시:
 * ```kotlin
 * data class Document(
 *     override val state: DocumentStatus,
 *     val content: String,
 * ) : Stateful<DocumentStatus, Document> {
 *     override fun withState(newState: DocumentStatus): Document =
 *         copy(state = newState)
 * }
 * ```
 */
interface Stateful<S : State, Self : Stateful<S, Self>> {
    /**
     * 현재 상태.
     */
    val state: S

    /**
     * 새로운 상태로 복사본 생성.
     * @param newState 새 상태
     * @return 상태가 변경된 새 인스턴스
     */
    fun withState(newState: S): Self
}
