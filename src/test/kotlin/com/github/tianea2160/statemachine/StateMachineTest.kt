package com.github.tianea2160.statemachine

import com.github.tianea2160.statemachine.core.Action
import com.github.tianea2160.statemachine.core.DefaultStateMachine
import com.github.tianea2160.statemachine.core.Event
import com.github.tianea2160.statemachine.core.Guard
import com.github.tianea2160.statemachine.core.State
import com.github.tianea2160.statemachine.core.Stateful
import com.github.tianea2160.statemachine.core.Transition
import com.github.tianea2160.statemachine.core.and
import com.github.tianea2160.statemachine.core.not
import com.github.tianea2160.statemachine.core.or
import com.github.tianea2160.statemachine.core.then
import com.github.tianea2160.statemachine.dsl.stateMachine
import com.github.tianea2160.statemachine.exception.InvalidTransitionException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * StateMachine 테스트.
 */
class StateMachineTest {

    // 테스트용 상태
    enum class DocumentStatus : State {
        DRAFT,
        PUBLISHED,
        ARCHIVED,
    }

    // 테스트용 이벤트
    sealed interface DocumentEvent : Event {
        data object Publish : DocumentEvent
        data object Archive : DocumentEvent
        data object Restore : DocumentEvent
    }

    // 테스트용 컨텍스트 (Stateful 구현)
    data class Document(
        override val state: DocumentStatus,
        val content: String,
        val publishedAt: Long? = null,
        val archivedAt: Long? = null,
    ) : Stateful<DocumentStatus, Document> {
        override fun withState(newState: DocumentStatus): Document =
            copy(state = newState)
    }

    @Test
    fun `fire - 유효한 전이를 수행한다`() {
        // given
        val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
            from(DocumentStatus.DRAFT) {
                on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED
            }
        }
        val doc = Document(DocumentStatus.DRAFT, "Hello World")

        // when
        val result = machine.fire(doc, DocumentEvent.Publish)

        // then
        assertEquals(DocumentStatus.DRAFT, result.previousState)
        assertEquals(DocumentStatus.PUBLISHED, result.newState)
        assertTrue(result.stateChanged)
    }

    @Test
    fun `fire - 정의되지 않은 전이는 예외 발생`() {
        // given
        val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
            from(DocumentStatus.DRAFT) {
                on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED
            }
        }
        val doc = Document(DocumentStatus.ARCHIVED, "content")

        // when & then
        assertFailsWith<InvalidTransitionException> {
            machine.fire(doc, DocumentEvent.Publish)
        }
    }

    @Test
    fun `fire - Guard 실패 시 예외 발생`() {
        // given
        val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
            from(DocumentStatus.DRAFT) {
                on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED guardedBy {
                    it.content.isNotBlank()
                }
            }
        }
        val doc = Document(DocumentStatus.DRAFT, "")  // 빈 content

        // when & then
        assertFailsWith<InvalidTransitionException> {
            machine.fire(doc, DocumentEvent.Publish)
        }
    }

    @Test
    fun `fire - Action이 컨텍스트를 변환한다`() {
        // given
        val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
            from(DocumentStatus.DRAFT) {
                on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED action { doc, _ ->
                    doc.copy(publishedAt = 12345L)  // 상태는 자동 업데이트됨
                }
            }
        }
        val doc = Document(DocumentStatus.DRAFT, "Hello")

        // when
        val result = machine.fire(doc, DocumentEvent.Publish)

        // then
        assertEquals(DocumentStatus.PUBLISHED, result.context.state)  // 자동 업데이트됨
        assertEquals(12345L, result.context.publishedAt)
    }

    @Test
    fun `fire - Guard와 Action을 함께 사용`() {
        // given
        val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
            from(DocumentStatus.DRAFT) {
                on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED guardedBy {
                    it.content.length >= 10
                } action { doc, _ ->
                    doc.copy(publishedAt = 99999L)  // 상태는 자동 업데이트됨
                }
            }
        }
        val doc = Document(DocumentStatus.DRAFT, "Hello World!")

        // when
        val result = machine.fire(doc, DocumentEvent.Publish)

        // then
        assertEquals(DocumentStatus.PUBLISHED, result.newState)
        assertEquals(DocumentStatus.PUBLISHED, result.context.state)
        assertEquals(99999L, result.context.publishedAt)
    }

    @Test
    fun `canFire - 전이 가능하면 true 반환`() {
        // given
        val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
            from(DocumentStatus.DRAFT) {
                on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED
            }
        }
        val doc = Document(DocumentStatus.DRAFT, "Hello")

        // when & then
        assertTrue(machine.canFire(doc, DocumentEvent.Publish))
    }

    @Test
    fun `canFire - 정의되지 않은 전이는 false 반환`() {
        // given
        val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
            from(DocumentStatus.DRAFT) {
                on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED
            }
        }
        val doc = Document(DocumentStatus.PUBLISHED, "Hello")

        // when & then
        assertFalse(machine.canFire(doc, DocumentEvent.Publish))
    }

    @Test
    fun `canFire - Guard 실패 시 false 반환`() {
        // given
        val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
            from(DocumentStatus.DRAFT) {
                on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED guardedBy {
                    it.content.isNotBlank()
                }
            }
        }
        val doc = Document(DocumentStatus.DRAFT, "")

        // when & then
        assertFalse(machine.canFire(doc, DocumentEvent.Publish))
    }

    @Test
    fun `availableEvents - 현재 상태에서 가능한 이벤트 반환`() {
        // given
        val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
            from(DocumentStatus.DRAFT) {
                on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED
                on<DocumentEvent.Archive>() goto DocumentStatus.ARCHIVED
            }
            from(DocumentStatus.PUBLISHED) {
                on<DocumentEvent.Archive>() goto DocumentStatus.ARCHIVED
            }
        }
        val doc = Document(DocumentStatus.DRAFT, "Hello")

        // when
        val events = machine.availableEvents(doc)

        // then
        assertEquals(2, events.size)
        assertTrue(events.any { it == DocumentEvent.Publish::class })
        assertTrue(events.any { it == DocumentEvent.Archive::class })
    }

    @Test
    fun `availableEvents - Guard 실패한 이벤트는 제외`() {
        // given
        val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
            from(DocumentStatus.DRAFT) {
                on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED guardedBy {
                    it.content.isNotBlank()
                }
                on<DocumentEvent.Archive>() goto DocumentStatus.ARCHIVED
            }
        }
        val doc = Document(DocumentStatus.DRAFT, "")

        // when
        val events = machine.availableEvents(doc)

        // then
        assertEquals(1, events.size)
        assertTrue(events.any { it == DocumentEvent.Archive::class })
    }

    @Test
    fun `onTransition - 전이 후 콜백이 호출된다`() {
        // given
        var callbackInvoked = false
        var capturedFrom: DocumentStatus? = null
        var capturedTo: DocumentStatus? = null

        val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
            from(DocumentStatus.DRAFT) {
                on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED
            }
            onTransition { from, _, to ->
                callbackInvoked = true
                capturedFrom = from
                capturedTo = to
            }
        }
        val doc = Document(DocumentStatus.DRAFT, "Hello")

        // when
        machine.fire(doc, DocumentEvent.Publish)

        // then
        assertTrue(callbackInvoked)
        assertEquals(DocumentStatus.DRAFT, capturedFrom)
        assertEquals(DocumentStatus.PUBLISHED, capturedTo)
    }

    @Test
    fun `여러 상태에서 전이 정의`() {
        // given
        val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
            from(DocumentStatus.DRAFT) {
                on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED
            }
            from(DocumentStatus.PUBLISHED) {
                on<DocumentEvent.Archive>() goto DocumentStatus.ARCHIVED
            }
            from(DocumentStatus.ARCHIVED) {
                on<DocumentEvent.Restore>() goto DocumentStatus.DRAFT
            }
        }
        val doc = Document(DocumentStatus.DRAFT, "Hello")

        // when - 순차 전이 (상태는 자동 업데이트됨)
        val r1 = machine.fire(doc, DocumentEvent.Publish)
        val r2 = machine.fire(r1.context, DocumentEvent.Archive)
        val r3 = machine.fire(r2.context, DocumentEvent.Restore)

        // then
        assertEquals(DocumentStatus.PUBLISHED, r1.newState)
        assertEquals(DocumentStatus.PUBLISHED, r1.context.state)  // 상태 자동 업데이트 확인
        assertEquals(DocumentStatus.ARCHIVED, r2.newState)
        assertEquals(DocumentStatus.ARCHIVED, r2.context.state)
        assertEquals(DocumentStatus.DRAFT, r3.newState)
        assertEquals(DocumentStatus.DRAFT, r3.context.state)
    }

    @Test
    fun `Guard 조합 - and 연산자`() {
        // given
        val notBlank: Guard<Document> = Guard { it.content.isNotBlank() }
        val longEnough: Guard<Document> = Guard { it.content.length >= 5 }
        val combined = notBlank and longEnough

        // when & then
        assertTrue(combined.evaluate(Document(DocumentStatus.DRAFT, "Hello World")))
        assertFalse(combined.evaluate(Document(DocumentStatus.DRAFT, "Hi")))
        assertFalse(combined.evaluate(Document(DocumentStatus.DRAFT, "")))
    }

    @Test
    fun `Guard 조합 - or 연산자`() {
        // given
        val isPublished: Guard<Document> = Guard { it.state == DocumentStatus.PUBLISHED }
        val isDraft: Guard<Document> = Guard { it.state == DocumentStatus.DRAFT }
        val combined = isPublished or isDraft

        // when & then
        assertTrue(combined.evaluate(Document(DocumentStatus.DRAFT, "content")))
        assertTrue(combined.evaluate(Document(DocumentStatus.PUBLISHED, "content")))
        assertFalse(combined.evaluate(Document(DocumentStatus.ARCHIVED, "content")))
    }

    @Test
    fun `Guard 조합 - not 연산자`() {
        // given
        val isArchived: Guard<Document> = Guard { it.state == DocumentStatus.ARCHIVED }
        val notArchived = !isArchived

        // when & then
        assertTrue(notArchived.evaluate(Document(DocumentStatus.DRAFT, "content")))
        assertFalse(notArchived.evaluate(Document(DocumentStatus.ARCHIVED, "content")))
    }

    @Test
    fun `Action 체이닝 - then 연산자`() {
        // given
        val setPublished: Action<Document, DocumentEvent> = Action { doc, _ ->
            doc.copy(state = DocumentStatus.PUBLISHED)
        }
        val setTimestamp: Action<Document, DocumentEvent> = Action { doc, _ ->
            doc.copy(publishedAt = 12345L)
        }
        val combined = setPublished then setTimestamp

        // when
        val result = combined.execute(Document(DocumentStatus.DRAFT, "Hello"), DocumentEvent.Publish)

        // then
        assertEquals(DocumentStatus.PUBLISHED, result.state)
        assertEquals(12345L, result.publishedAt)
    }

    @Test
    fun `DefaultStateMachine 직접 생성`() {
        // given
        val transitions = listOf(
            Transition<DocumentStatus, DocumentEvent, Document>(
                source = DocumentStatus.DRAFT,
                eventType = DocumentEvent.Publish::class,
                target = DocumentStatus.PUBLISHED,
            ),
        )
        val machine = DefaultStateMachine(transitions)
        val doc = Document(DocumentStatus.DRAFT, "Hello")

        // when
        val result = machine.fire(doc, DocumentEvent.Publish)

        // then
        assertEquals(DocumentStatus.PUBLISHED, result.newState)
    }

    // ===== 다중 Guard/Action 테스트 =====

    @Test
    fun `여러 Guard를 순차적으로 평가 - 모두 통과`() {
        // given
        val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
            from(DocumentStatus.DRAFT) {
                on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED guards {
                    guard { it.content.isNotBlank() }
                    guard { it.content.length >= 5 }
                    guard { !it.content.contains("forbidden") }
                }
            }
        }
        val doc = Document(DocumentStatus.DRAFT, "Hello World")

        // when
        val result = machine.fire(doc, DocumentEvent.Publish)

        // then
        assertEquals(DocumentStatus.PUBLISHED, result.newState)
    }

    @Test
    fun `여러 Guard 중 하나라도 실패하면 전이 불가`() {
        // given
        val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
            from(DocumentStatus.DRAFT) {
                on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED guards {
                    guard { it.content.isNotBlank() }
                    guard { it.content.length >= 20 }  // 이 조건 실패
                }
            }
        }
        val doc = Document(DocumentStatus.DRAFT, "Short")

        // when & then
        assertFalse(machine.canFire(doc, DocumentEvent.Publish))
        assertFailsWith<InvalidTransitionException> {
            machine.fire(doc, DocumentEvent.Publish)
        }
    }

    @Test
    fun `여러 Action을 순차적으로 실행`() {
        // given
        val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
            from(DocumentStatus.DRAFT) {
                on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED actions {
                    action { doc, _ -> doc.copy(publishedAt = 11111L) }
                    action { doc, _ -> doc.copy(content = doc.content.uppercase()) }
                }
            }
        }
        val doc = Document(DocumentStatus.DRAFT, "Hello")

        // when
        val result = machine.fire(doc, DocumentEvent.Publish)

        // then
        assertEquals(DocumentStatus.PUBLISHED, result.context.state)  // 자동 업데이트
        assertEquals(11111L, result.context.publishedAt)
        assertEquals("HELLO", result.context.content)
    }

    @Test
    fun `여러 Guard와 여러 Action을 함께 사용`() {
        // given
        val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
            from(DocumentStatus.DRAFT) {
                on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED guards {
                    guard { it.content.isNotBlank() }
                    guard { it.content.length >= 3 }
                } actions {
                    action { doc, _ -> doc.copy(publishedAt = 22222L) }
                }
            }
        }
        val doc = Document(DocumentStatus.DRAFT, "Valid Content")

        // when
        val result = machine.fire(doc, DocumentEvent.Publish)

        // then
        assertEquals(DocumentStatus.PUBLISHED, result.newState)
        assertEquals(DocumentStatus.PUBLISHED, result.context.state)  // 자동 업데이트
        assertEquals(22222L, result.context.publishedAt)
    }

    @Test
    fun `Guard 객체를 unaryPlus로 추가`() {
        // given
        val contentNotBlank: Guard<Document> = Guard { it.content.isNotBlank() }
        val contentLongEnough: Guard<Document> = Guard { it.content.length >= 5 }

        val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
            from(DocumentStatus.DRAFT) {
                on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED guards {
                    +contentNotBlank
                    +contentLongEnough
                }
            }
        }
        val doc = Document(DocumentStatus.DRAFT, "Hello World")

        // when & then
        assertTrue(machine.canFire(doc, DocumentEvent.Publish))
    }

    @Test
    fun `Action 객체를 unaryPlus로 추가`() {
        // given
        val setTimestamp: Action<Document, DocumentEvent> = Action { doc, _ ->
            doc.copy(publishedAt = 33333L)
        }
        val uppercaseContent: Action<Document, DocumentEvent> = Action { doc, _ ->
            doc.copy(content = doc.content.uppercase())
        }

        val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
            from(DocumentStatus.DRAFT) {
                on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED actions {
                    +setTimestamp
                    +uppercaseContent
                }
            }
        }
        val doc = Document(DocumentStatus.DRAFT, "Hello")

        // when
        val result = machine.fire(doc, DocumentEvent.Publish)

        // then
        assertEquals(DocumentStatus.PUBLISHED, result.context.state)  // 자동 업데이트
        assertEquals(33333L, result.context.publishedAt)
        assertEquals("HELLO", result.context.content)
    }

    @Test
    fun `기존 guardedBy와 action 체이닝 API 호환성`() {
        // given - 기존 API 사용
        val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
            from(DocumentStatus.DRAFT) {
                on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED guardedBy {
                    it.content.isNotBlank()
                } guardedBy {
                    it.content.length >= 3
                } action { doc, _ ->
                    doc.copy(publishedAt = 44444L)
                } action { doc, _ ->
                    doc.copy(content = doc.content.uppercase())
                }
            }
        }
        val doc = Document(DocumentStatus.DRAFT, "Hello")

        // when
        val result = machine.fire(doc, DocumentEvent.Publish)

        // then
        assertEquals(DocumentStatus.PUBLISHED, result.newState)
        assertEquals(DocumentStatus.PUBLISHED, result.context.state)  // 자동 업데이트
        assertEquals(44444L, result.context.publishedAt)
        assertEquals("HELLO", result.context.content)
    }

    // ===== Stateful 인터페이스 테스트 =====

    @Test
    fun `Stateful - withState로 상태 변경`() {
        // given
        val doc = Document(DocumentStatus.DRAFT, "Hello")

        // when
        val published = doc.withState(DocumentStatus.PUBLISHED)

        // then
        assertEquals(DocumentStatus.PUBLISHED, published.state)
        assertEquals("Hello", published.content)
    }

    @Test
    fun `Stateful - state 필드로 현재 상태 조회`() {
        // given
        val doc = Document(DocumentStatus.DRAFT, "Hello")

        // then
        assertEquals(DocumentStatus.DRAFT, doc.state)
    }
}
