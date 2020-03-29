import React from 'react';
import Quiz from "./quiz";

import './Participants.css';

interface ParticipantsProps {
    quiz: Quiz;
}

const Participants: React.FC<ParticipantsProps> = (props: ParticipantsProps) => {
    const elements = props.quiz.participants?.map(p => <span key={p.name} className={"participant " + (p.turn ? 'turn' : '')}>{p.name} ({p.points})</span>)

    return (
        <div>
            <h3>Teilnehmer</h3>
            <p data-testid="participants">
                {elements}
            </p>
        </div>
    )
};

export default Participants;