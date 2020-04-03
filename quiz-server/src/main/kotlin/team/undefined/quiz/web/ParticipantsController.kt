package team.undefined.quiz.web

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import team.undefined.quiz.core.QuizService

@RestController
@CrossOrigin(origins = ["*"], allowedHeaders = ["*"])
@RequestMapping("/api/quiz/{quizId}/participants")
class ParticipantsController(private val quizService: QuizService) {

    @PostMapping(consumes = ["text/plain"], produces = ["application/json"])
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@PathVariable quizId: Long, @RequestBody participantName: String): Mono<CreateParticipantResponse> {
        return quizService.createParticipant(quizId, participantName)
                .flatMap { it.map() }
                .map { CreateParticipantResponse(it.participants.find { it.name == participantName }!!.id!!, it) }
    }

    @PutMapping("/{participantId}/buzzer", produces = ["application/json"])
    fun buzzer(@PathVariable quizId: Long, @PathVariable participantId: Long): Mono<QuizDTO> {
        return quizService.buzzer(quizId, participantId)
                .flatMap { it.map() }
    }

}