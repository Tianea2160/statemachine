```
   _____ _        _         __  __            _     _
  / ____| |      | |       |  \/  |          | |   (_)
 | (___ | |_ __ _| |_ ___  | \  / | __ _  ___| |__  _ _ __   ___
  \___ \| __/ _` | __/ _ \ | |\/| |/ _` |/ __| '_ \| | '_ \ / _ \
  ____) | || (_| | ||  __/ | |  | | (_| | (__| | | | | | | |  __/
 |_____/ \__\__,_|\__\___| |_|  |_|\__,_|\___|_| |_|_|_| |_|\___|
```

[![Build](https://github.com/Tianea2160/statemachine/actions/workflows/build.yml/badge.svg)](https://github.com/Tianea2160/statemachine/actions/workflows/build.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white)
![JVM](https://img.shields.io/badge/JVM-24-007396?logo=openjdk&logoColor=white)
![License](https://img.shields.io/badge/License-Apache%202.0-blue)

---

## Welcome to State Machine

State Machine is a type-safe, declarative state machine library for Kotlin. It provides a fluent DSL that makes defining state transitions intuitive and readable, while maintaining full compile-time type safety.

The library embraces Kotlin's language features to deliver an expressive API where state machines become self-documenting code. Guards, actions, and transitions are composed naturally using infix functions and lambda expressions.

---

## Features

- **Type-safe DSL** - Define state machines with compile-time type checking for states, events, and context
- **Guard conditions** - Control transitions with composable boolean predicates (`and`, `or`, `not`)
- **Actions** - Execute side effects or transform context during transitions with chainable actions (`then`)
- **Stateful interface** - Automatic state management through the `Stateful` interface
- **Transition callbacks** - Hook into state changes for logging, event publishing, or auditing
- **Immutable design** - All transitions produce new context instances, ensuring thread safety

---

## Quick Start

### Gradle

```kotlin
dependencies {
    implementation("org.project.hansan:statemachine:1.0.0")
}
```

### Basic Usage

```kotlin
import org.project.hansan.statemachine.core.*
import org.project.hansan.statemachine.dsl.stateMachine

// Define states
enum class DocumentStatus : State {
    DRAFT, PUBLISHED, ARCHIVED
}

// Define events
sealed interface DocumentEvent : Event {
    data object Publish : DocumentEvent
    data object Archive : DocumentEvent
}

// Define context implementing Stateful
data class Document(
    override val state: DocumentStatus,
    val content: String
) : Stateful<DocumentStatus, Document> {
    override fun withState(newState: DocumentStatus) = copy(state = newState)
}

// Build state machine
val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
    from(DocumentStatus.DRAFT) {
        on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED
        on<DocumentEvent.Archive>() goto DocumentStatus.ARCHIVED
    }
    from(DocumentStatus.PUBLISHED) {
        on<DocumentEvent.Archive>() goto DocumentStatus.ARCHIVED
    }
}

// Use it
val doc = Document(DocumentStatus.DRAFT, "Hello World")
val result = machine.fire(doc, DocumentEvent.Publish)
// result.newState == DocumentStatus.PUBLISHED
```

---

## Examples

### Guards and Actions

```kotlin
val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
    from(DocumentStatus.DRAFT) {
        on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED guardedBy {
            it.content.isNotBlank()
        } action { doc, _ ->
            doc.copy(publishedAt = Instant.now())
        }
    }
}
```

### Multiple Guards and Actions

```kotlin
val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
    from(DocumentStatus.DRAFT) {
        on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED guards {
            guard { it.content.isNotBlank() }
            guard { it.content.length >= 10 }
            guard { !it.content.contains("forbidden") }
        } actions {
            action { doc, _ -> doc.copy(publishedAt = Instant.now()) }
            action { doc, _ -> doc.copy(content = doc.content.trim()) }
        }
    }
}
```

### Composable Guard Operators

```kotlin
val notBlank: Guard<Document> = Guard { it.content.isNotBlank() }
val longEnough: Guard<Document> = Guard { it.content.length >= 10 }

// Combine with operators
val publishable = notBlank and longEnough
val archivable = notBlank or Guard { it.state == DocumentStatus.PUBLISHED }
val notArchived = !Guard<Document> { it.state == DocumentStatus.ARCHIVED }
```

### Chainable Actions

```kotlin
val setTimestamp: Action<Document, DocumentEvent> = Action { doc, _ ->
    doc.copy(publishedAt = Instant.now())
}
val normalizeContent: Action<Document, DocumentEvent> = Action { doc, _ ->
    doc.copy(content = doc.content.trim().lowercase())
}

// Chain with 'then'
val publishAction = setTimestamp then normalizeContent
```

### Transition Callbacks

```kotlin
val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
    from(DocumentStatus.DRAFT) {
        on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED
    }
    onTransition { from, event, to ->
        logger.info("Transition: $from -> $to via $event")
    }
}
```

---

## API Reference

| Component | Description |
|-----------|-------------|
| `State` | Marker interface for state types (typically enums) |
| `Event` | Marker interface for event types |
| `Stateful<S, C>` | Interface for context objects that hold state |
| `Guard<C>` | Predicate that controls whether a transition can occur |
| `Action<C, E>` | Function executed during transition, returns new context |
| `Transition<S, E, C>` | Represents a single state transition definition |
| `StateMachine<S, E, C>` | The state machine interface |
| `stateMachine { }` | DSL entry point for building state machines |

### StateMachine Methods

| Method | Description |
|--------|-------------|
| `fire(model, event)` | Execute transition, returns `TransitionResult` |
| `canFire(model, event)` | Check if transition is possible |
| `availableEvents(model)` | Get all possible events for current state |

---

<details>
<summary><strong>Korean</strong></summary>

## State Machine 라이브러리

State Machine은 Kotlin을 위한 타입 안전한 선언적 상태 머신 라이브러리입니다. 직관적이고 가독성 높은 Fluent DSL을 제공하며, 컴파일 타임에 완전한 타입 안전성을 보장합니다.

### 주요 기능

- **타입 안전한 DSL** - 상태, 이벤트, 컨텍스트에 대한 컴파일 타임 타입 검사
- **Guard 조건** - 조합 가능한 불리언 조건자로 전이 제어 (`and`, `or`, `not`)
- **Action** - 전이 중 부수 효과 실행 또는 컨텍스트 변환, 체이닝 지원 (`then`)
- **Stateful 인터페이스** - `Stateful` 인터페이스를 통한 자동 상태 관리
- **전이 콜백** - 로깅, 이벤트 발행, 감사를 위한 상태 변경 훅
- **불변 설계** - 모든 전이가 새로운 컨텍스트 인스턴스를 생성하여 스레드 안전성 보장

### 사용 예시

```kotlin
val machine = stateMachine<DocumentStatus, DocumentEvent, Document> {
    from(DocumentStatus.DRAFT) {
        on<DocumentEvent.Publish>() goto DocumentStatus.PUBLISHED guardedBy {
            it.content.isNotBlank()
        } action { doc, _ ->
            doc.copy(publishedAt = Instant.now())
        }
    }
}

val doc = Document(DocumentStatus.DRAFT, "Hello World")
val result = machine.fire(doc, DocumentEvent.Publish)
```

</details>

---

## Contributing

Contributions are welcome. Please feel free to submit issues and pull requests.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## License

```
Copyright 2025 State Machine Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
