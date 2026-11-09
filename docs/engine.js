/*
 * Chess engine for the browser version of the game.
 *
 * This is a JavaScript port of the Java engine in src/main/java/chess:
 * full move rules, SAN notation, draw detection and the same minimax
 * search (negamax with alpha-beta pruning and quiescence search) with the
 * same piece-square evaluation.
 *
 * Everything lives inside createEngine() so the page can start a Web Worker
 * from this function's source text and search without freezing the board.
 *
 * Squares are numbered 0..63 with 0 = a8 and 63 = h1 (row 0 is rank 8),
 * matching the Java Position class. Pieces are FEN letters: uppercase for
 * White, lowercase for Black.
 */
function createEngine() {
  const START_FEN = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';
  const VALUES = { P: 100, N: 320, B: 330, R: 500, Q: 900, K: 0 };

  const KNIGHT_JUMPS = [[-2, -1], [-2, 1], [-1, -2], [-1, 2], [1, -2], [1, 2], [2, -1], [2, 1]];
  const ORTHOGONAL = [[-1, 0], [1, 0], [0, -1], [0, 1]];
  const DIAGONAL = [[-1, -1], [-1, 1], [1, -1], [1, 1]];
  const ALL_DIRECTIONS = ORTHOGONAL.concat(DIAGONAL);

  const WHITE_KINGSIDE = 1;
  const WHITE_QUEENSIDE = 2;
  const BLACK_KINGSIDE = 4;
  const BLACK_QUEENSIDE = 8;

  const colorOf = (piece) => (piece === piece.toUpperCase() ? 'w' : 'b');
  const typeOf = (piece) => piece.toUpperCase();
  const opposite = (color) => (color === 'w' ? 'b' : 'w');
  const forward = (color) => (color === 'w' ? -1 : 1);
  const onBoard = (row, col) => row >= 0 && row < 8 && col >= 0 && col < 8;
  const squareName = (sq) => 'abcdefgh'[sq & 7] + (8 - (sq >> 3));
  const squareIndex = (name) => (8 - Number(name[1])) * 8 + (name.charCodeAt(0) - 97);
  const withColor = (type, color) => (color === 'w' ? type : type.toLowerCase());

  // Piece-square tables from White's point of view (row 0 = rank 8),
  // adapted from Tomasz Michniewski's "Simplified Evaluation Function".
  const PST = {
    P: [0, 0, 0, 0, 0, 0, 0, 0, 50, 50, 50, 50, 50, 50, 50, 50, 10, 10, 20, 30, 30, 20, 10, 10,
      5, 5, 10, 25, 25, 10, 5, 5, 0, 0, 0, 20, 20, 0, 0, 0, 5, -5, -10, 0, 0, -10, -5, 5,
      5, 10, 10, -20, -20, 10, 10, 5, 0, 0, 0, 0, 0, 0, 0, 0],
    N: [-50, -40, -30, -30, -30, -30, -40, -50, -40, -20, 0, 0, 0, 0, -20, -40,
      -30, 0, 10, 15, 15, 10, 0, -30, -30, 5, 15, 20, 20, 15, 5, -30, -30, 0, 15, 20, 20, 15, 0, -30,
      -30, 5, 10, 15, 15, 10, 5, -30, -40, -20, 0, 5, 5, 0, -20, -40, -50, -40, -30, -30, -30, -30, -40, -50],
    B: [-20, -10, -10, -10, -10, -10, -10, -20, -10, 0, 0, 0, 0, 0, 0, -10, -10, 0, 5, 10, 10, 5, 0, -10,
      -10, 5, 5, 10, 10, 5, 5, -10, -10, 0, 10, 10, 10, 10, 0, -10, -10, 10, 10, 10, 10, 10, 10, -10,
      -10, 5, 0, 0, 0, 0, 5, -10, -20, -10, -10, -10, -10, -10, -10, -20],
    R: [0, 0, 0, 0, 0, 0, 0, 0, 5, 10, 10, 10, 10, 10, 10, 5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5,
      -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, 0, 0, 0, 5, 5, 0, 0, 0],
    Q: [-20, -10, -10, -5, -5, -10, -10, -20, -10, 0, 0, 0, 0, 0, 0, -10, -10, 0, 5, 5, 5, 5, 0, -10,
      -5, 0, 5, 5, 5, 5, 0, -5, 0, 0, 5, 5, 5, 5, 0, -5, -10, 5, 5, 5, 5, 5, 0, -10,
      -10, 0, 5, 0, 0, 0, 0, -10, -20, -10, -10, -5, -5, -10, -10, -20],
    K: [-30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30,
      -30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30,
      -20, -30, -30, -40, -40, -30, -30, -20, -10, -20, -20, -20, -20, -20, -20, -10,
      20, 20, 0, 0, 0, 0, 20, 20, 20, 30, 10, 0, 0, 10, 30, 20],
    KEnd: [-50, -40, -30, -20, -20, -30, -40, -50, -30, -20, -10, 0, 0, -10, -20, -30,
      -30, -10, 20, 30, 30, 20, -10, -30, -30, -10, 30, 40, 40, 30, -10, -30,
      -30, -10, 30, 40, 40, 30, -10, -30, -30, -10, 20, 30, 30, 20, -10, -30,
      -30, -30, 0, 0, 0, 0, -30, -30, -50, -30, -30, -30, -30, -30, -30, -50]
  };

  class Board {
    constructor(fen = START_FEN) {
      const fields = fen.trim().split(/\s+/);
      this.squares = new Array(64).fill(null);
      this.kings = { w: -1, b: -1 };
      let sq = 0;
      for (const ch of fields[0]) {
        if (ch === '/') continue;
        if (ch >= '1' && ch <= '8') {
          sq += Number(ch);
        } else {
          this.squares[sq] = ch;
          if (ch === 'K') this.kings.w = sq;
          if (ch === 'k') this.kings.b = sq;
          sq++;
        }
      }
      this.turn = fields[1] === 'b' ? 'b' : 'w';
      this.rights = 0;
      for (const ch of fields[2] || '-') {
        if (ch === 'K') this.rights |= WHITE_KINGSIDE;
        if (ch === 'Q') this.rights |= WHITE_QUEENSIDE;
        if (ch === 'k') this.rights |= BLACK_KINGSIDE;
        if (ch === 'q') this.rights |= BLACK_QUEENSIDE;
      }
      this.ep = !fields[3] || fields[3] === '-' ? -1 : squareIndex(fields[3]);
      this.halfmove = Number(fields[4] || 0);
      this.fullmove = Number(fields[5] || 1);
      this.history = [];
    }

    // ---- making and undoing moves ----

    makeMove(move) {
      this.history.push({ move, rights: this.rights, ep: this.ep, halfmove: this.halfmove, fullmove: this.fullmove });
      const s = this.squares;
      const row = move.from >> 3;
      const color = colorOf(move.piece);

      s[move.from] = null;
      if (move.flag === 'e') s[row * 8 + (move.to & 7)] = null;
      if (move.flag === 'k') { s[row * 8 + 5] = s[row * 8 + 7]; s[row * 8 + 7] = null; }
      if (move.flag === 'q') { s[row * 8 + 3] = s[row * 8]; s[row * 8] = null; }
      s[move.to] = move.promo || move.piece;

      if (typeOf(move.piece) === 'K') {
        this.kings[color] = move.to;
        this.rights &= color === 'w' ? ~(WHITE_KINGSIDE | WHITE_QUEENSIDE) : ~(BLACK_KINGSIDE | BLACK_QUEENSIDE);
      }
      for (const corner of [move.from, move.to]) {
        if (corner === 63) this.rights &= ~WHITE_KINGSIDE;
        else if (corner === 56) this.rights &= ~WHITE_QUEENSIDE;
        else if (corner === 7) this.rights &= ~BLACK_KINGSIDE;
        else if (corner === 0) this.rights &= ~BLACK_QUEENSIDE;
      }
      this.ep = move.flag === 'd' ? (move.from + move.to) >> 1 : -1;
      this.halfmove = typeOf(move.piece) === 'P' || move.captured ? 0 : this.halfmove + 1;
      if (this.turn === 'b') this.fullmove++;
      this.turn = opposite(this.turn);
    }

    undoMove() {
      const info = this.history.pop();
      const move = info.move;
      const s = this.squares;
      const row = move.from >> 3;

      s[move.to] = null;
      s[move.from] = move.piece;
      if (move.flag === 'e') {
        s[row * 8 + (move.to & 7)] = move.captured;
      } else if (move.flag === 'k') {
        s[row * 8 + 7] = s[row * 8 + 5]; s[row * 8 + 5] = null;
      } else if (move.flag === 'q') {
        s[row * 8] = s[row * 8 + 3]; s[row * 8 + 3] = null;
      } else if (move.captured) {
        s[move.to] = move.captured;
      }
      if (typeOf(move.piece) === 'K') this.kings[colorOf(move.piece)] = move.from;

      this.rights = info.rights;
      this.ep = info.ep;
      this.halfmove = info.halfmove;
      this.fullmove = info.fullmove;
      this.turn = opposite(this.turn);
    }

    lastMove() {
      return this.history.length ? this.history[this.history.length - 1].move : null;
    }

    // ---- move generation ----

    pseudoMovesFrom(from, moves) {
      const piece = this.squares[from];
      const color = colorOf(piece);
      const type = typeOf(piece);
      const row = from >> 3;
      const col = from & 7;
      const s = this.squares;

      const add = (to, flag = 'n') => {
        moves.push({ from, to, piece, captured: s[to], promo: null, flag });
      };

      if (type === 'P') {
        const dir = forward(color);
        const lastRow = color === 'w' ? 0 : 7;
        const startRow = color === 'w' ? 6 : 1;
        const pushPawn = (to, captured, flag) => {
          if ((to >> 3) === lastRow) {
            for (const t of ['Q', 'R', 'B', 'N']) {
              moves.push({ from, to, piece, captured, promo: withColor(t, color), flag: 'p' });
            }
          } else {
            moves.push({ from, to, piece, captured, promo: null, flag });
          }
        };
        const oneRow = row + dir;
        if (onBoard(oneRow, col) && !s[oneRow * 8 + col]) {
          pushPawn(oneRow * 8 + col, null, 'n');
          const twoRow = row + 2 * dir;
          if (row === startRow && !s[twoRow * 8 + col]) {
            moves.push({ from, to: twoRow * 8 + col, piece, captured: null, promo: null, flag: 'd' });
          }
        }
        for (const dc of [-1, 1]) {
          if (!onBoard(oneRow, col + dc)) continue;
          const to = oneRow * 8 + col + dc;
          const target = s[to];
          if (target && colorOf(target) !== color) {
            pushPawn(to, target, 'n');
          } else if (!target && to === this.ep) {
            moves.push({ from, to, piece, captured: s[row * 8 + col + dc], promo: null, flag: 'e' });
          }
        }
        return;
      }

      if (type === 'N' || type === 'K') {
        const steps = type === 'N' ? KNIGHT_JUMPS : ALL_DIRECTIONS;
        for (const [dr, dc] of steps) {
          const r = row + dr;
          const c = col + dc;
          if (!onBoard(r, c)) continue;
          const target = s[r * 8 + c];
          if (!target || colorOf(target) !== color) add(r * 8 + c);
        }
        if (type === 'K') this.addCastling(from, color, moves);
        return;
      }

      const directions = type === 'R' ? ORTHOGONAL : type === 'B' ? DIAGONAL : ALL_DIRECTIONS;
      for (const [dr, dc] of directions) {
        let r = row + dr;
        let c = col + dc;
        while (onBoard(r, c)) {
          const target = s[r * 8 + c];
          if (!target) {
            add(r * 8 + c);
          } else {
            if (colorOf(target) !== color) add(r * 8 + c);
            break;
          }
          r += dr;
          c += dc;
        }
      }
    }

    addCastling(from, color, moves) {
      const row = color === 'w' ? 7 : 0;
      if (from !== row * 8 + 4) return;
      const kingside = color === 'w' ? WHITE_KINGSIDE : BLACK_KINGSIDE;
      const queenside = color === 'w' ? WHITE_QUEENSIDE : BLACK_QUEENSIDE;
      if (!(this.rights & (kingside | queenside))) return;
      const enemy = opposite(color);
      if (this.isAttacked(from, enemy)) return;
      const s = this.squares;
      const rook = withColor('R', color);
      const base = row * 8;
      const piece = s[from];

      if ((this.rights & kingside) && s[base + 7] === rook && !s[base + 5] && !s[base + 6]
        && !this.isAttacked(base + 5, enemy) && !this.isAttacked(base + 6, enemy)) {
        moves.push({ from, to: base + 6, piece, captured: null, promo: null, flag: 'k' });
      }
      if ((this.rights & queenside) && s[base] === rook && !s[base + 1] && !s[base + 2] && !s[base + 3]
        && !this.isAttacked(base + 3, enemy) && !this.isAttacked(base + 2, enemy)) {
        moves.push({ from, to: base + 2, piece, captured: null, promo: null, flag: 'q' });
      }
    }

    legalMoves() {
      const pseudo = [];
      for (let sq = 0; sq < 64; sq++) {
        const piece = this.squares[sq];
        if (piece && colorOf(piece) === this.turn) this.pseudoMovesFrom(sq, pseudo);
      }
      return this.filterLegal(pseudo);
    }

    legalMovesFrom(sq) {
      const piece = this.squares[sq];
      if (!piece || colorOf(piece) !== this.turn) return [];
      const pseudo = [];
      this.pseudoMovesFrom(sq, pseudo);
      return this.filterLegal(pseudo);
    }

    filterLegal(pseudo) {
      const mover = this.turn;
      const legal = [];
      for (const move of pseudo) {
        this.makeMove(move);
        if (!this.inCheck(mover)) legal.push(move);
        this.undoMove();
      }
      return legal;
    }

    inCheck(color) {
      return this.isAttacked(this.kings[color], opposite(color));
    }

    /** Whether any piece of color `by` attacks square `sq`. */
    isAttacked(sq, by) {
      const s = this.squares;
      const row = sq >> 3;
      const col = sq & 7;
      const pawnRow = row - forward(by);
      const pawn = withColor('P', by);
      for (const dc of [-1, 1]) {
        if (onBoard(pawnRow, col + dc) && s[pawnRow * 8 + col + dc] === pawn) return true;
      }
      const knight = withColor('N', by);
      for (const [dr, dc] of KNIGHT_JUMPS) {
        if (onBoard(row + dr, col + dc) && s[(row + dr) * 8 + col + dc] === knight) return true;
      }
      const king = withColor('K', by);
      for (const [dr, dc] of ALL_DIRECTIONS) {
        if (onBoard(row + dr, col + dc) && s[(row + dr) * 8 + col + dc] === king) return true;
      }
      const queen = withColor('Q', by);
      const rook = withColor('R', by);
      const bishop = withColor('B', by);
      for (const [directions, slider] of [[ORTHOGONAL, rook], [DIAGONAL, bishop]]) {
        for (const [dr, dc] of directions) {
          let r = row + dr;
          let c = col + dc;
          while (onBoard(r, c)) {
            const piece = s[r * 8 + c];
            if (piece) {
              if (piece === slider || piece === queen) return true;
              break;
            }
            r += dr;
            c += dc;
          }
        }
      }
      return false;
    }

    // ---- draws and notation ----

    hasInsufficientMaterial() {
      let minors = 0;
      const bishops = [];
      for (let sq = 0; sq < 64; sq++) {
        const piece = this.squares[sq];
        if (!piece) continue;
        const type = typeOf(piece);
        if (type === 'K') continue;
        if (type === 'P' || type === 'R' || type === 'Q') return false;
        minors++;
        if (type === 'B') bishops.push(sq);
      }
      if (minors <= 1) return true;
      if (minors === 2 && bishops.length === 2) {
        const [a, b] = bishops;
        const dark = (sq) => ((sq >> 3) + (sq & 7)) % 2 === 1;
        return colorOf(this.squares[a]) !== colorOf(this.squares[b]) && dark(a) === dark(b);
      }
      return false;
    }

    fen() {
      let placement = '';
      for (let row = 0; row < 8; row++) {
        let empty = 0;
        for (let col = 0; col < 8; col++) {
          const piece = this.squares[row * 8 + col];
          if (!piece) { empty++; continue; }
          if (empty) { placement += empty; empty = 0; }
          placement += piece;
        }
        if (empty) placement += empty;
        if (row < 7) placement += '/';
      }
      let rights = '';
      if (this.rights & WHITE_KINGSIDE) rights += 'K';
      if (this.rights & WHITE_QUEENSIDE) rights += 'Q';
      if (this.rights & BLACK_KINGSIDE) rights += 'k';
      if (this.rights & BLACK_QUEENSIDE) rights += 'q';
      return [placement, this.turn, rights || '-', this.ep < 0 ? '-' : squareName(this.ep),
        this.halfmove, this.fullmove].join(' ');
    }

    /** FEN without move counters, used for threefold repetition. */
    positionKey() {
      return this.fen().split(' ').slice(0, 4).join(' ');
    }
  }

  function toUci(move) {
    return squareName(move.from) + squareName(move.to) + (move.promo ? move.promo.toLowerCase() : '');
  }

  /** Standard Algebraic Notation for a legal move, e.g. "Nf3", "exd5", "O-O", "Qh7#". */
  function toSan(board, move) {
    let san;
    const type = typeOf(move.piece);
    if (move.flag === 'k') {
      san = 'O-O';
    } else if (move.flag === 'q') {
      san = 'O-O-O';
    } else if (type === 'P') {
      san = (move.captured ? 'abcdefgh'[move.from & 7] + 'x' : '') + squareName(move.to);
      if (move.promo) san += '=' + typeOf(move.promo);
    } else {
      let ambiguous = false;
      let sameFile = false;
      let sameRank = false;
      for (const other of board.legalMoves()) {
        if (other.to === move.to && other.from !== move.from && other.piece === move.piece) {
          ambiguous = true;
          if ((other.from & 7) === (move.from & 7)) sameFile = true;
          if ((other.from >> 3) === (move.from >> 3)) sameRank = true;
        }
      }
      let hint = '';
      if (ambiguous) {
        const name = squareName(move.from);
        hint = !sameFile ? name[0] : !sameRank ? name[1] : name;
      }
      san = type + hint + (move.captured ? 'x' : '') + squareName(move.to);
    }
    board.makeMove(move);
    if (board.inCheck(board.turn)) san += board.legalMoves().length === 0 ? '#' : '+';
    board.undoMove();
    return san;
  }

  // ---- AI: negamax with alpha-beta pruning and quiescence search ----

  const INFINITY = 1e9;
  const MATE_SCORE = 1e6;
  const MAX_QUIESCENCE_DEPTH = 6;

  function evaluate(board) {
    let nonPawn = 0;
    for (const piece of board.squares) {
      if (piece && typeOf(piece) !== 'P') nonPawn += VALUES[typeOf(piece)];
    }
    const endgame = nonPawn <= 1600;
    let score = 0;
    for (let sq = 0; sq < 64; sq++) {
      const piece = board.squares[sq];
      if (!piece) continue;
      const type = typeOf(piece);
      const white = colorOf(piece) === 'w';
      const tableIndex = white ? sq : (7 - (sq >> 3)) * 8 + (sq & 7);
      const table = type === 'K' && endgame ? PST.KEnd : PST[type];
      const value = VALUES[type] + table[tableIndex];
      score += white ? value : -value;
    }
    return board.turn === 'w' ? score : -score;
  }

  function orderingScore(move) {
    let score = 0;
    if (move.captured) score += 10 * VALUES[typeOf(move.captured)] - VALUES[typeOf(move.piece)] + 10000;
    if (move.promo) score += VALUES[typeOf(move.promo)];
    return score;
  }

  const byOrdering = (a, b) => orderingScore(b) - orderingScore(a);

  /**
   * Finds the best move for the side to move.
   * Returns { move, nodes } where move is null if there are no legal moves.
   */
  function search(board, depth) {
    let nodes = 0;

    function quiescence(alpha, beta, qDepth) {
      nodes++;
      const standPat = evaluate(board);
      if (standPat >= beta) return beta;
      if (standPat > alpha) alpha = standPat;
      if (qDepth >= MAX_QUIESCENCE_DEPTH) return alpha;
      const captures = board.legalMoves().filter((m) => m.captured || m.promo).sort(byOrdering);
      for (const move of captures) {
        board.makeMove(move);
        const score = -quiescence(-beta, -alpha, qDepth + 1);
        board.undoMove();
        if (score >= beta) return beta;
        if (score > alpha) alpha = score;
      }
      return alpha;
    }

    function negamax(remaining, alpha, beta, ply) {
      nodes++;
      const moves = board.legalMoves();
      if (moves.length === 0) return board.inCheck(board.turn) ? -(MATE_SCORE - ply) : 0;
      if (board.halfmove >= 100 || board.hasInsufficientMaterial()) return 0;
      if (remaining === 0) return quiescence(alpha, beta, 0);
      moves.sort(byOrdering);
      for (const move of moves) {
        board.makeMove(move);
        const score = -negamax(remaining - 1, -beta, -alpha, ply + 1);
        board.undoMove();
        if (score >= beta) return beta;
        if (score > alpha) alpha = score;
      }
      return alpha;
    }

    const moves = board.legalMoves();
    if (moves.length === 0) return { move: null, nodes };
    // Shuffle before the stable sort so equal moves vary from game to game
    for (let i = moves.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [moves[i], moves[j]] = [moves[j], moves[i]];
    }
    moves.sort(byOrdering);

    let best = moves[0];
    let alpha = -INFINITY;
    for (const move of moves) {
      board.makeMove(move);
      const score = -negamax(depth - 1, -INFINITY, -alpha, 1);
      board.undoMove();
      if (score > alpha) {
        alpha = score;
        best = move;
      }
    }
    return { move: best, nodes };
  }

  return { Board, START_FEN, VALUES, colorOf, typeOf, opposite, squareName, squareIndex, toUci, toSan, search, evaluate };
}

if (typeof module !== 'undefined') module.exports = { createEngine };
