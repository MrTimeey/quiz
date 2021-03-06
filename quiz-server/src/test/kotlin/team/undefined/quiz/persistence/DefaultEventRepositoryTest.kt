package team.undefined.quiz.persistence

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.context.annotation.Import
import reactor.test.StepVerifier
import team.undefined.quiz.core.*
import java.util.*

data class TestEvent(@JsonProperty("quizId") override val quizId: UUID, @JsonProperty("sequenceNumber") override val sequenceNumber: Long, @JsonProperty("timestamp") override val timestamp: Long, @JsonProperty("payload") val payload: Map<String, String>) : Event {
    override fun process(quiz: Quiz): Quiz {
        return quiz.setTimestamp(Date().time)
    }
}

@DataR2dbcTest
@Import(DefaultEventRepository::class, PersistenceConfiguration::class, UndoneEventsCache::class, DefaultQuizService::class, QuizProjectionConfiguration::class, DefaultQuizProjection::class, ObjectMapper::class, QuizStatisticsProjectionConfiguration::class, QuizStatisticsProjection::class)
internal class DefaultEventRepositoryTest {

    @Autowired
    private lateinit var defaultEventRepository: DefaultEventRepository

    @Autowired
    private lateinit var eventEntityRepository: EventEntityRepository;

    @BeforeEach
    fun clearDB() {
        eventEntityRepository.deleteAll().subscribe()
    }

    @Test
    fun shouldStoreRetrieveAndDeleteEvents() {
        val firstQuizId = UUID.randomUUID()
        val secondQuizId = UUID.randomUUID()

        StepVerifier.create(defaultEventRepository.storeEvent(TestEvent(firstQuizId, 0, Date().time, mapOf(Pair("key1", "value1")))))
                .consumeNextWith {
                    assertThat(it.quizId).isEqualTo(firstQuizId)
                    assertThat((it as TestEvent).payload).isEqualTo(mapOf(Pair("key1", "value1")))
                }
                .verifyComplete()

        Thread.sleep(1)

        StepVerifier.create(defaultEventRepository.storeEvent(TestEvent(firstQuizId, 1, Date().time, mapOf(Pair("key2", "value2")))))
                .consumeNextWith {
                    assertThat(it.quizId).isEqualTo(firstQuizId)
                    assertThat((it as TestEvent).payload).isEqualTo(mapOf(Pair("key2", "value2")))
                }
                .verifyComplete()

        Thread.sleep(1)

        StepVerifier.create(defaultEventRepository.storeEvent(TestEvent(secondQuizId, 2, Date().time, mapOf(Pair("key1", "value1")))))
                .consumeNextWith {
                    assertThat(it.quizId).isEqualTo(secondQuizId)
                    assertThat((it as TestEvent).payload).isEqualTo(mapOf(Pair("key1", "value1")))
                }
                .verifyComplete()

        Thread.sleep(1)

        StepVerifier.create(defaultEventRepository.determineEvents(firstQuizId))
                .consumeNextWith {
                    assertThat(it.quizId).isEqualTo(firstQuizId)
                    assertThat((it as TestEvent).payload).isEqualTo(mapOf(Pair("key1", "value1")))
                }
                .consumeNextWith {
                    assertThat(it.quizId).isEqualTo(firstQuizId)
                    assertThat((it as TestEvent).payload).isEqualTo(mapOf(Pair("key2", "value2")))
                }
                .verifyComplete()

        StepVerifier.create(defaultEventRepository.determineEvents())
                .consumeNextWith {
                    assertThat(it.quizId).isEqualTo(firstQuizId)
                    assertThat((it as TestEvent).payload).isEqualTo(mapOf(Pair("key1", "value1")))
                }
                .consumeNextWith {
                    assertThat(it.quizId).isEqualTo(firstQuizId)
                    assertThat((it as TestEvent).payload).isEqualTo(mapOf(Pair("key2", "value2")))
                }
                .consumeNextWith {
                    assertThat(it.quizId).isEqualTo(secondQuizId)
                    assertThat((it as TestEvent).payload).isEqualTo(mapOf(Pair("key1", "value1")))
                }
                .verifyComplete()

        val allQuizIds = mutableSetOf(firstQuizId, secondQuizId)
        StepVerifier.create(defaultEventRepository.determineQuizIds())
                .consumeNextWith { allQuizIds.remove(it) }
                .consumeNextWith { allQuizIds.remove(it) }
                .verifyComplete()

        assertThat(allQuizIds).isEmpty()

        StepVerifier.create(defaultEventRepository.deleteEvents(firstQuizId))
                .verifyComplete()

        StepVerifier.create(defaultEventRepository.determineEvents(firstQuizId))
                .verifyComplete()

        StepVerifier.create(defaultEventRepository.determineEvents())
                .consumeNextWith {
                    assertThat(it.quizId).isEqualTo(secondQuizId)
                    assertThat((it as TestEvent).payload).isEqualTo(mapOf(Pair("key1", "value1")))
                }
                .verifyComplete()
    }

    @Test
    fun shouldUndoLastAction() {
        val firstQuizId = UUID.randomUUID()

        StepVerifier.create(defaultEventRepository.storeEvent(TestEvent(firstQuizId, 0, Date().time, mapOf(Pair("key1", "value1")))))
                .consumeNextWith {
                    assertThat(it.quizId).isEqualTo(firstQuizId)
                    assertThat((it as TestEvent).payload).isEqualTo(mapOf(Pair("key1", "value1")))
                }
                .verifyComplete()
        Thread.sleep(1)

        StepVerifier.create(defaultEventRepository.storeEvent(TestEvent(firstQuizId, 1, Date().time, mapOf(Pair("key2", "value2")))))
                .consumeNextWith {
                    assertThat(it.quizId).isEqualTo(firstQuizId)
                    assertThat((it as TestEvent).payload).isEqualTo(mapOf(Pair("key2", "value2")))
                }
                .verifyComplete()
        Thread.sleep(1)

        StepVerifier.create(defaultEventRepository.undoLastAction(firstQuizId))
                .consumeNextWith {
                    assertThat(it.quizId).isEqualTo(firstQuizId)
                    assertThat((it as TestEvent).payload).isEqualTo(mapOf(Pair("key2", "value2")))
                }
                .verifyComplete()

        StepVerifier.create(defaultEventRepository.determineEvents(firstQuizId))
                .consumeNextWith {
                    assertThat(it.quizId).isEqualTo(firstQuizId)
                    assertThat((it as TestEvent).payload).isEqualTo(mapOf(Pair("key1", "value1")))
                }
                .verifyComplete()
    }

}