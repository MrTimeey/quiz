package team.undefined.quiz.core

import com.google.common.collect.MultimapBuilder
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import reactor.core.publisher.Mono
import java.time.Duration.ofSeconds
import java.util.*
import kotlin.collections.HashSet

@Component
class QuestionProjection(eventBus: EventBus,
                         eventRepository: EventRepository) {

    private val questions = MultimapBuilder.hashKeys().arrayListValues().build<UUID, Question>()

    init {
        eventBus.register(this)

        Mono.delay(ofSeconds(5))
                .flatMapMany { eventRepository.determineEvents() }
                .filter { it is QuestionCreatedEvent || it is QuestionDeletedEvent || it is QuestionEditedEvent || it is QuestionAskedEvent || it is QuizDeletedEvent }
                .subscribe {
                    if (it is QuestionCreatedEvent) {
                        handleQuestionCreation(it)
                    } else if (it is QuestionDeletedEvent) {
                        handleQuestionDeletion(it)
                    } else if (it is QuestionEditedEvent) {
                        handleQuestionEdit(it)
                    } else if (it is QuestionAskedEvent) {
                        handleQuestionAsked(it)
                    } else if (it is QuizDeletedEvent) {
                        handleQuizDeletion(it)
                    }
                }
    }

    @Subscribe
    fun handleQuestionCreation(event: QuestionCreatedEvent) {
        questions.put(event.quizId, event.question.copy())
    }

    @Subscribe
    fun handleQuestionDeletion(event: QuestionDeletedEvent) {
        questions[event.quizId].removeIf { it.id == event.questionId }
    }

    @Subscribe
    fun handleQuestionEdit(event: QuestionEditedEvent) {
        questions[event.quizId].replaceAll {
            if (it.id == event.question.id) {
                event.question.copy()
            } else {
                it
            }
        }
    }

    @Subscribe
    fun handleQuestionAsked(event: QuestionAskedEvent) {
        val question = questions.get(event.quizId).find { it.id == event.questionId }
        question?.alreadyPlayed = true
        question?.pending = true
    }

    @Subscribe
    fun handleQuizDeletion(event: QuizDeletedEvent) {
        questions.removeAll(event.quizId)
    }

    fun determineQuestions(): Map<UUID, List<Question>> {
        val proposedQuestions = HashMap<UUID, List<Question>>()

        val distinct = HashSet<String>()

        questions.asMap().entries.forEach { entry ->
            proposedQuestions[entry.key] = entry.value.asSequence()
                    .filter { it.alreadyPlayed }
                    .filter { it.visibility == Question.QuestionVisibility.PUBLIC }
                    .filter { StringUtils.isEmpty(it.imageUrl) }
                    .filter { distinct.add(it.question) }
                    .toList()
        }

        return proposedQuestions
    }

}