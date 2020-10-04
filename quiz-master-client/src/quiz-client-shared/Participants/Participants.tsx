import React, { useState, useEffect, useCallback, useRef } from 'react';
import Quiz, { Participant } from "../quiz";
import FlipMove from "react-flip-move"

import './Participants.css';
import ParticipantItem from './ParticipantItem';
const buzzerfile = require('./../../assets/buzzer.mp3');

interface ParticipantsProps {
    quiz: Quiz;
}

interface ParticipantState {
    id: string;
    points: number;
}

const Participants: React.FC<ParticipantsProps> = (props: ParticipantsProps) => {
    const buzzerAudio = useRef(null);
    const pendingQuestion = props.quiz.openQuestions.find(question => question.pending);
    const itsAPlayersTurn = props.quiz.participants.some(p => p.turn);
    const [stateAfterLastQuestion, setStateAfterLastQuestion] = useState(new Array<ParticipantState>()); 
    const [currentQuestionId, setCurrentQuestionId] = useState("none");
    const [wasAPlayersTurnBefore, setWasAPlayersTurnBefore] = useState(false);

    const getPointsAfterLastQuestionForParticipant = (participant: Participant) => {
        const participantStateAfterLastQuestion = stateAfterLastQuestion.find(p => p.id === participant.id);
        if (participantStateAfterLastQuestion) {
            return participantStateAfterLastQuestion.points;
        } else {
            const participantCurrentState = props.quiz.participants.find(p => p.id === participant.id);
            return participantCurrentState ? participantCurrentState.points : 0;
        }
    }
    
    const updateStateAfterLastQuestion = useCallback(() => {
        setCurrentQuestionId(pendingQuestion ? pendingQuestion.id : "none");
        setStateAfterLastQuestion(props.quiz.participants.map(p => { return {id: p.id, points: p.points}}));
    }, [props.quiz.participants, pendingQuestion]);

    const isNewQuestion = useCallback(() => {
        return pendingQuestion && currentQuestionId !== pendingQuestion.id;
    }, [pendingQuestion, currentQuestionId]); 

    const playerHasBuzzered = useCallback(() => {
        return !wasAPlayersTurnBefore && itsAPlayersTurn; 
    }, [wasAPlayersTurnBefore, itsAPlayersTurn]);

    useEffect(() => {
        if (playerHasBuzzered()) {
            buzzerAudio.current.muted = false;
            buzzerAudio.current.play();
        }
        setWasAPlayersTurnBefore(itsAPlayersTurn)
    }, [playerHasBuzzered, itsAPlayersTurn, setWasAPlayersTurnBefore]);

    useEffect(() => {
        if (isNewQuestion()) {
            updateStateAfterLastQuestion();
        } 
    }, [isNewQuestion, updateStateAfterLastQuestion]);
    
    useEffect(() => {
        // Trigger preloading of audio to prevent delays when buzzer is pressed
        buzzerAudio.current.muted = true;
        buzzerAudio.current.play();
    }, [])

    const comparePoints = (a: Participant, b: Participant) => {
        if (b.points === a.points) {
            return a.name.localeCompare(b.name);
        } else {
            return b.points - a.points;
        }  
    }

    const elements = props.quiz.participants?.sort(comparePoints).map(p => 
        <div key={p.name}>
            <ParticipantItem quiz={props.quiz} participant={p} pointsAfterLastQuestion={getPointsAfterLastQuestionForParticipant(p)}>
            </ParticipantItem>
        </div>)

    return (
        <div>
            <audio src={buzzerfile} ref={buzzerAudio} preload='auto'></audio>
            <h5 className="title is-5">Participants</h5>
            <div data-testid="participants" className="participants-list">
                <FlipMove>
                    {elements}
                </FlipMove>     
            </div>
        </div>
    )
};

export default Participants;