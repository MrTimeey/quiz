package team.undefined.quiz.core

import java.util.*
import kotlin.collections.ArrayList

data class Quiz(val id: UUID = UUID.randomUUID(), val name: String, val participants: List<Participant> = ArrayList(), val questions: List<Question> = ArrayList(), var finished: Boolean = false, var quizStatistics: QuizStatistics? = null) {

    private var timestamp: Long? = null;

    fun nobodyHasBuzzered(): Boolean {
        return participants
                .none { it.turn }
    }

    fun select(participantId: UUID): Quiz {
        participants
                .find { it.id == participantId }
                ?.turn = true
        return this
    }

    fun hasNoParticipantWithName(name: String): Boolean {
        return participants.none { it.name == name }
    }

    fun addParticipantIfNecessary(participant: Participant): Quiz {
        if (hasNoParticipantWithName(participant.name)) {
            (participants as MutableList).add(participant)
        }

        return this;
    }

    fun addQuestion(question: Question): Quiz {
        (questions as MutableList).add(question)
        return this;
    }

    fun startQuestion(questionId: UUID): Quiz {
        participants.forEach { it.turn = false }
        questions
                .filter { it.pending }
                .forEach {
                    it.pending = false
                    it.alreadyPlayed = true
                }
        questions.filter { it.id == questionId }[0].pending = true
        return this
    }

    fun answeredCorrect(): Quiz {
        participants
                .filter { it.turn }
                .forEach { it.points = it.points + 2 }

        questions
                .filter { it.pending }
                .forEach {
                    it.pending = false
                    it.alreadyPlayed = true
                }

        return this;
    }

    fun answeredInorrect(): Quiz {
        participants
                .filter { it.turn }
                .forEach { it.points = Math.max(it.points - 1, 0) }
        return this;
    }

    fun reopenQuestion(): Quiz {
        participants.forEach { it.turn = false }
        return this;
    }

    fun finishQuiz(): Quiz {
        finished = true
        return this
    }

    fun setTimestamp(timestamp: Long): Quiz {
        this.timestamp = timestamp
        return this
    }

    fun getTimestamp(): Long? {
        return timestamp
    }

}
