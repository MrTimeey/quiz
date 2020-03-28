interface Link {
    rel: string;
    href: string;
}

interface Participant {
    id: number;
    name: string;
    turn: boolean;
    points: number;
    links: Array<Link>;
}

export default interface Quiz {
    id: number;
    turn?: string;
    participants: Array<Participant>;
    links: Array<Link>;
}