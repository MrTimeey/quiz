package team.undefined.quiz.web

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import team.undefined.quiz.core.QuizService

@Component
class ReactiveWebSocketHandler(private val quizService: QuizService,
                               private val objectMapper: ObjectMapper) : WebSocketHandler {

    override fun handle(webSocketSession: WebSocketSession): Mono<Void> {
        return webSocketSession.send(quizService.observeQuiz()
                .flatMap { it.map() }
                .map { objectMapper.writeValueAsString(it) }
                .map { webSocketSession.textMessage(it.toString()) })
                .and(webSocketSession.receive()
                        .map { it.payloadAsText }
                        .log());
    }
}