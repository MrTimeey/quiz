package team.undefined.quiz.persistence

import org.springframework.data.annotation.Id

data class QuestionEntity(@Id var id: Long?, var question: String, var pending: Int, var quizId: Long)