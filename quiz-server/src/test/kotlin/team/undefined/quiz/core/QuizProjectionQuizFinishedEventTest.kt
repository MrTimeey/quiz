package team.undefined.quiz.core

import com.google.common.eventbus.EventBus
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import reactor.core.publisher.Flux
import java.util.concurrent.atomic.AtomicReference

internal class QuizProjectionQuizFinishedEventTest {

    @Test
    fun shouldHandleQuizFinishedEventWhenQuizIsAlreadyInCache() {
        val quiz = Quiz(name = "Awesome Quiz")

        val question = Question(question = "Wofür steht die Abkürzung a.D.?")
        val participant = Participant(name = "Lena")

        val eventBus = EventBus()
        val eventRepository = mock(EventRepository::class.java)
        `when`(eventRepository.determineEvents(quiz.id))
                .thenReturn(Flux.just(
                        QuizCreatedEvent(quiz.id, quiz, 1),
                        QuestionCreatedEvent(quiz.id, question, 2),
                        ParticipantCreatedEvent(quiz.id, participant, 3),
                        QuestionAskedEvent(quiz.id, question.id, 4),
                        BuzzeredEvent(quiz.id, participant.id, 5),
                        AnsweredEvent(quiz.id, AnswerCommand.Answer.CORRECT, 6),
                        QuizFinishedEvent(quiz.id, 7)
                ))
        val quizProjection = QuizProjection(eventBus, QuizStatisticsProvider(eventRepository), eventRepository)

        val observedQuiz = AtomicReference<Quiz>()
        quizProjection.observeQuiz(quiz.id)
                .subscribe { observedQuiz.set(it) }

        eventBus.post(QuizCreatedEvent(quiz.id, quiz, 1))
        eventBus.post(QuestionCreatedEvent(quiz.id, question, 2))
        eventBus.post(ParticipantCreatedEvent(quiz.id, participant, 3))
        eventBus.post(QuestionAskedEvent(quiz.id, question.id, 4))
        eventBus.post(BuzzeredEvent(quiz.id, participant.id, 5))
        eventBus.post(AnsweredEvent(quiz.id, AnswerCommand.Answer.CORRECT, 6))
        eventBus.post(QuizFinishedEvent(quiz.id, 7))

        await until {
            observedQuiz.get().id == quiz.id
                    && observedQuiz.get().finished
                    && observedQuiz.get().quizStatistics!!.questionStatistics.size == 1
                    && observedQuiz.get().quizStatistics!!.questionStatistics[0].questionId == question.id
                    && observedQuiz.get().quizStatistics!!.questionStatistics[0].buzzerStatistics.size == 1
                    && observedQuiz.get().quizStatistics!!.questionStatistics[0].buzzerStatistics[0].duration == 1L
                    && observedQuiz.get().quizStatistics!!.questionStatistics[0].buzzerStatistics[0].participantId == participant.id
                    && observedQuiz.get().quizStatistics!!.questionStatistics[0].buzzerStatistics[0].answer == AnswerCommand.Answer.CORRECT

        }
    }

    @Test
    fun shouldHandleHandleQuizFinishedEventWhenQuizIsNotInCacheAndLastEventWasAlreadyPersisted() {
        val quiz = Quiz(name = "Awesome Quiz")

        val question = Question(question = "Wofür steht die Abkürzung a.D.?")
        val participant = Participant(name = "Lena")
        val finishedEvent = QuizFinishedEvent(quiz.id, 7)

        val eventRepository = mock(EventRepository::class.java)
        `when`(eventRepository.determineEvents(quiz.id))
                .thenReturn(Flux.just(
                        QuizCreatedEvent(quiz.id, quiz, 1),
                        QuestionCreatedEvent(quiz.id, question, 2),
                        ParticipantCreatedEvent(quiz.id, participant, 3),
                        QuestionAskedEvent(quiz.id, question.id, 4),
                        BuzzeredEvent(quiz.id, participant.id, 5),
                        AnsweredEvent(quiz.id, AnswerCommand.Answer.CORRECT, 6),
                        finishedEvent
                ))

        val eventBus = EventBus()
        val quizProjection = QuizProjection(eventBus, QuizStatisticsProvider(eventRepository), eventRepository)

        val observedQuiz = AtomicReference<Quiz>()
        quizProjection.observeQuiz(quiz.id)
                .subscribe { observedQuiz.set(it) }

        eventBus.post(finishedEvent)

        await until {
            observedQuiz.get().id == quiz.id
                    && observedQuiz.get().finished
                    && observedQuiz.get().quizStatistics!!.questionStatistics.size == 1
                    && observedQuiz.get().quizStatistics!!.questionStatistics[0].questionId == question.id
                    && observedQuiz.get().quizStatistics!!.questionStatistics[0].buzzerStatistics.size == 1
                    && observedQuiz.get().quizStatistics!!.questionStatistics[0].buzzerStatistics[0].duration == 1L
                    && observedQuiz.get().quizStatistics!!.questionStatistics[0].buzzerStatistics[0].participantId == participant.id
                    && observedQuiz.get().quizStatistics!!.questionStatistics[0].buzzerStatistics[0].answer == AnswerCommand.Answer.CORRECT
        }
    }

    @Test
    fun shouldHandleHandleQuizFinishedEventCreationWhenQuizIsNotInCacheAndLastEventWasNotYetPersisted() {
        val quiz = Quiz(name = "Awesome Quiz")
        val question = Question(question = "Wofür steht die Abkürzung a.D.?")
        val participant = Participant(name = "Lena")

        val eventRepository = mock(EventRepository::class.java)
        `when`(eventRepository.determineEvents(quiz.id))
                .thenReturn(Flux.just(
                        QuizCreatedEvent(quiz.id, quiz, 1),
                        QuestionCreatedEvent(quiz.id, question, 2),
                        ParticipantCreatedEvent(quiz.id, participant, 3),
                        QuestionAskedEvent(quiz.id, question.id, 4),
                        BuzzeredEvent(quiz.id, participant.id, 5),
                        AnsweredEvent(quiz.id, AnswerCommand.Answer.CORRECT, 6)
                ))

        val eventBus = EventBus()
        val quizProjection = QuizProjection(eventBus, QuizStatisticsProvider(eventRepository), eventRepository)

        val observedQuiz = AtomicReference<Quiz>()
        quizProjection.observeQuiz(quiz.id)
                .subscribe { observedQuiz.set(it) }

        eventBus.post(QuizFinishedEvent(quiz.id, 7))

        await until {
            observedQuiz.get().id == quiz.id
                    && observedQuiz.get().finished
                    && observedQuiz.get().quizStatistics!!.questionStatistics.size == 1
                    && observedQuiz.get().quizStatistics!!.questionStatistics[0].questionId == question.id
                    && observedQuiz.get().quizStatistics!!.questionStatistics[0].buzzerStatistics.size == 1
                    && observedQuiz.get().quizStatistics!!.questionStatistics[0].buzzerStatistics[0].duration == 1L
                    && observedQuiz.get().quizStatistics!!.questionStatistics[0].buzzerStatistics[0].participantId == participant.id
                    && observedQuiz.get().quizStatistics!!.questionStatistics[0].buzzerStatistics[0].answer == AnswerCommand.Answer.CORRECT
        }
    }

}