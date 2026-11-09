package chess.model;

/** Special cases the board needs to know about when making or undoing a move. */
public enum MoveType {
    NORMAL,
    DOUBLE_PAWN_PUSH,
    EN_PASSANT,
    CASTLE_KINGSIDE,
    CASTLE_QUEENSIDE,
    PROMOTION
}
