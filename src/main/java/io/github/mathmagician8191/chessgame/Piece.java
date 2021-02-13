package io.github.mathmagician8191.chessgame;

public class Piece {
  /*
  Implements a piece representing a square on the board, filled or empty
  */
  
  static Piece square = new Piece();
  
  public final boolean isPiece;
  public final int side; //white=1 black = -1
  public final char letter; //used to convert to FEN and to see legal moves
  public Piece(char letter) {
    this.letter = Character.toLowerCase(letter);
    this.isPiece = true;
    this.side = Character.toUpperCase(letter)==letter ? 1 : -1;
  }
  public Piece() {
    this.isPiece = false; //for empty squares
    this.side = 0;
    this.letter = ' ';
  }
  
  @Override
  public boolean equals(Object other) {
    if (this==other) {
      return true;
    }
    if (other == null) {
      return false;
    }
    if (this.getClass() != other.getClass()) {
      return false;
    }
    Piece otherPiece = (Piece) other;
    return this.isPiece==otherPiece.isPiece && this.side==otherPiece.side &&
        this.letter==otherPiece.letter;
  }
}