package team.undefined.quiz.core

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import team.undefined.quiz.core.QuizStatisticsAssert.assertThat

internal class QuizStatisticsProviderTest {

    @Test
    fun testBuzzerWithoutAskedQuestion() {
        val quiz = Quiz(name = "Awesome Quiz")

        val buzzerQuestion = Question(question = "Wofür steht die Abkürzung a.D.?")
        val freetextQuestion = Question(question = "Wer schrieb Peter und der Wolf?", estimates = HashMap(), initialTimeToAnswer = 45)
        val participant1 = Participant(name = "Lena")
        val participant2 = Participant(name = "Erik")

        val eventRepository = Mockito.mock(EventRepository::class.java)
        Mockito.`when`(eventRepository.determineEvents(quiz.id))
                .thenReturn(Flux.just(
                        QuizCreatedEvent(quiz.id, quiz, 1),
                        QuestionCreatedEvent(quiz.id, buzzerQuestion, 2),
                        QuestionCreatedEvent(quiz.id, freetextQuestion, 3),
                        ParticipantCreatedEvent(quiz.id, participant1, 4),
                        ParticipantCreatedEvent(quiz.id, participant2, 5),
                        BuzzeredEvent(quiz.id, participant1.id, 7),
                        QuestionAskedEvent(quiz.id, buzzerQuestion.id, 8),
                        BuzzeredEvent(quiz.id, participant1.id, 9),
                        AnsweredEvent(quiz.id, participant1.id, AnswerCommand.Answer.CORRECT, 10),
                        QuestionAskedEvent(quiz.id, freetextQuestion.id, 11),
                        EstimatedEvent(quiz.id, participant1.id, "Sergej Prokofjew", 12),
                        EstimatedEvent(quiz.id, participant2.id, "Max Mustermann", 13),
                        AnsweredEvent(quiz.id, participant1.id, AnswerCommand.Answer.CORRECT, 14),
                        QuizFinishedEvent(quiz.id, 15)
                ))

        val quizStatisticsProvider = QuizStatisticsProvider(eventRepository)

        StepVerifier.create(quizStatisticsProvider.generateStatistics(quiz.id))
                .consumeNextWith {
                    assertThat(it)
                            .questionStatisticsSizeId(2)
                            .hasQuestionStatistics(0) { questionStatistics ->
                                questionStatistics
                                        .hasQuestionId(buzzerQuestion.id)
                                        .buzzerStatisticsSizeIs(1)
                                        .hasBuzzerStatistics(0) { buzzerStatistics ->
                                            buzzerStatistics
                                                    .hasDuration(1L)
                                                    .hasParticipantId(participant1.id)
                                                    .isCorrect
                                        }
                            }
                            .hasQuestionStatistics(1) { questionStatistics ->
                                questionStatistics
                                        .hasQuestionId(freetextQuestion.id)
                                        .buzzerStatisticsSizeIs(2)
                                        .hasBuzzerStatistics(0) { buzzerStatistics ->
                                            buzzerStatistics
                                                    .hasDuration(1L)
                                                    .hasParticipantId(participant1.id)
                                                    .isCorrect
                                        }
                                        .hasBuzzerStatistics(1) { buzzerStatistics ->
                                            buzzerStatistics
                                                    .hasDuration(2L)
                                                    .hasParticipantId(participant2.id)
                                                    .isIncorrect
                                        }
                            }
                }
                .verifyComplete()
    }

}