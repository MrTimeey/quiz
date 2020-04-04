import React, { useState, useEffect } from 'react';
import Quiz from './quiz';
import Participants from './Participants';
import Questions from './Questions';
import Answers from './Answers';

interface QuizMasterProps {
    quiz: Quiz;
}

const QuizMaster: React.FC<QuizMasterProps> = (props: QuizMasterProps) => {
    const [quiz, setQuiz] = useState(props.quiz);

    useEffect(() => {
        console.log('websocket wird erstellt');
        const clientWebSocket = new WebSocket(`${process.env.REACT_APP_WS_BASE_URL}/event-emitter/${quiz.id}`);
        clientWebSocket.onmessage = ev => {
            console.log('event', ev);
            setQuiz(JSON.parse(ev.data));
        };

        const i = setInterval(() => clientWebSocket.send('heartBeat'), 10000);

        return () => {
            console.log('websocket wird geschlossen');
            clientWebSocket.close();
            clearInterval(i);
        };
    }, []);

    return (
        <div className="container Quiz-dashboard">
            <h4 className="title is-4">{quiz.name} (ID: {quiz.id})</h4>
            <div className="container Dashboard-content">
                <div className="container participants">
                    <Participants quiz={quiz}></Participants>
                </div>
                <div className="container question">
                    <Answers quiz={quiz}></Answers>
                    <Questions quiz={quiz}></Questions>
                </div>
               
            </div>
        </div>
    )
}

export default QuizMaster;