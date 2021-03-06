package team.undefined.quiz.core

import com.google.common.eventbus.EventBus
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import reactor.core.publisher.Flux
import java.util.*
import java.util.concurrent.atomic.AtomicReference

internal class QuizProjectionQuestionEditedEventTest {

    @Test
    fun shouldHandleQuestionEditWhenQuizIsAlreadyInCache() {
        val quiz = Quiz(name = "Awesome Quiz")
        val questionId = UUID.randomUUID()

        val eventBus = EventBus()
        val quizProjection = DefaultQuizProjection(
            eventBus,
            mock(EventRepository::class.java),
            UndoneEventsCache(),
            QuizProjectionConfiguration(25, 1)
        )

        val observedQuiz = AtomicReference<Quiz>()
        quizProjection.observeQuiz(quiz.id)
                .subscribe { observedQuiz.set(it) }

        val changedQuestion = Question(questionId, question = "Wer ist das?", imageUrl = "urlToImage", visibility = Question.QuestionVisibility.PUBLIC)
        eventBus.post(QuizCreatedEvent(quiz.id, quiz, sequenceNumber = 1))
        eventBus.post(QuestionCreatedEvent(
            quiz.id,
            Question(questionId, question = "Wer ist das", visibility = Question.QuestionVisibility.PRIVATE),
            sequenceNumber = 2
        ))
        eventBus.post(QuestionEditedEvent(quiz.id, changedQuestion, sequenceNumber = 3))

        await untilAsserted {
            val q = observedQuiz.get()
            assertThat(q.id).isEqualTo(quiz.id)
            assertThat(q.participants).isEmpty()
            assertThat(q.questions).containsExactly(changedQuestion)
            assertThat(q.undoPossible).isTrue()
            assertThat(q.finished).isFalse()
        }
    }

    @Test
    fun shouldHandleQuestionEditWhenQuizIsNotInCacheAndLastEventWasAlreadyPersisted() {
        val quiz = Quiz(name = "Awesome Quiz")
        val questionId = UUID.randomUUID()

        val changedQuestion = Question(questionId, question = "Wofür steht die Abkürzung a.D.?")
        val questionEditedEvent = QuestionEditedEvent(quiz.id, changedQuestion, sequenceNumber = 3)

        val eventRepository = mock(EventRepository::class.java)
        `when`(eventRepository.determineEvents(quiz.id))
                .thenReturn(Flux.just(QuizCreatedEvent(quiz.id, quiz, sequenceNumber = 1), QuestionCreatedEvent(
                    quiz.id,
                    Question(questionId, question = "Wofür steht die Abkürzung a.D.?"),
                    sequenceNumber = 2
                ), questionEditedEvent))

        val eventBus = EventBus()
        val quizProjection = DefaultQuizProjection(
            eventBus,
            eventRepository,
            UndoneEventsCache(),
            QuizProjectionConfiguration(25, 1)
        )

        val observedQuiz = AtomicReference<Quiz>()
        quizProjection.observeQuiz(quiz.id)
                .subscribe { observedQuiz.set(it) }

        eventBus.post(questionEditedEvent)

        await untilAsserted {
            val q = observedQuiz.get()
            assertThat(q.id).isEqualTo(quiz.id)
            assertThat(q.participants).isEmpty()
            assertThat(q.questions).containsExactly(changedQuestion)
            assertThat(q.undoPossible).isTrue()
            assertThat(q.finished).isFalse()
        }
    }

    @Test
    fun shouldHandleQuestionEditWhenQuizIsNotInCacheAndLastEventWasNotYetPersisted() {
        val quiz = Quiz(name = "Awesome Quiz")
        val questionId = UUID.randomUUID()

        val changedQuestion = Question(questionId, question = "Wofür steht die Abkürzung a.D.?")
        val questionEditedEvent = QuestionEditedEvent(quiz.id, changedQuestion, sequenceNumber = 3)

        val eventRepository = mock(EventRepository::class.java)
        `when`(eventRepository.determineEvents(quiz.id))
                .thenReturn(Flux.just(QuizCreatedEvent(quiz.id, quiz, sequenceNumber = 1), QuestionCreatedEvent(
                    quiz.id,
                    Question(questionId, question = "Wofür steht die Abkürzung a.D.?"),
                    sequenceNumber = 2
                )))

        val eventBus = EventBus()
        val quizProjection = DefaultQuizProjection(
            eventBus,
            eventRepository,
            UndoneEventsCache(),
            QuizProjectionConfiguration(25, 1)
        )

        val observedQuiz = AtomicReference<Quiz>()
        quizProjection.observeQuiz(quiz.id)
                .subscribe { observedQuiz.set(it) }

        eventBus.post(questionEditedEvent)

        await untilAsserted {
            val q = observedQuiz.get()
            assertThat(q.id).isEqualTo(quiz.id)
            assertThat(q.participants).isEmpty()
            assertThat(q.questions).containsExactly(changedQuestion)
            assertThat(q.undoPossible).isTrue()
            assertThat(q.finished).isFalse()
        }
    }

}