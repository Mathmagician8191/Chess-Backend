package io.github.mathmagician8191.chessgame;

import java.util.ArrayList;
import java.util.Arrays;

public class Game {
  /*
  represents a chess game
  */
  
  //position
  public Board position;
  
  //for three-fold repitition, these variables are assumed immutable else stuff will break
  ArrayList<Board> pastPositions;
  ArrayList<Board> duplicatedPositions;
  
  //game status
  public int gameResult; //-1=black win, 0=draw, 1=white win
  public String endCause;
  
  //promotion options
  public String promotionOptions;
  
  public Game(String fen,int pawnRow,int pawnSquares,int queenRookColumn,
      int kingRookColumn, String promotionOptions) {
    this.position = new Board(fen,pawnRow,pawnSquares,queenRookColumn,kingRookColumn);
    this.checkResult();
    this.pastPositions = new ArrayList<>();
    this.pastPositions.add(new Board(this.position));
    this.duplicatedPositions = new ArrayList<>();
    this.promotionOptions = promotionOptions;
  }
  
  public Game(Game original) {
    this.position = new Board(original.position);
    this.gameResult = original.gameResult;
    this.endCause = original.endCause;
    this.promotionOptions = original.promotionOptions;
    
    //deep copy ArayLists
    this.pastPositions = new ArrayList<>();
    for (Board pastPosition : original.pastPositions) {
      this.pastPositions.add(pastPosition);
    }
    
    this.duplicatedPositions = new ArrayList<>();
    for (Board duplicatedPosition : original.duplicatedPositions) {
      this.duplicatedPositions.add(duplicatedPosition);
    }
  }
  
  @Override
  public String toString() {
    return this.position.toFen();
  }
  
  public void makeMove(int[] startSquare,int[] endSquare) {
    Board board = this.position;
    
    board.movePiece(startSquare,endSquare);
    
    //test moving this to the proper location
    boolean[] castling = new boolean[] {
      board.castleRights[0],
      board.castleRights[1],
      board.castleRights[2],
      board.castleRights[3]
    };
    
    if (board.halfmoveClock==0 || !Arrays.equals(castling,board.castleRights)) {
      this.pastPositions = new ArrayList<>();
      this.duplicatedPositions = new ArrayList<>();
    }
    
    this.checkResult();
   
    this.pastPositions.add(new Board(board));
    
    if (board.promotionAvailable && (this.promotionOptions.length() == 1)) {
      this.promotePiece(this.promotionOptions.charAt(0));
    }
  }
  
  public boolean checkResult() {
    //checks if the game is over
    
    //50-move rule
    if (this.position.halfmoveClock>100) {
      this.position.gameOver = true;
      this.gameResult = 0;
      this.endCause = "Draw by 50-move rule";
      return true;
    }
    
    //3-fold repitition
    if (!(null == this.duplicatedPositions)) {
      for (Board duplicatedPosition : this.duplicatedPositions) {
        if (this.position.equals(duplicatedPosition)) {
          this.position.gameOver = true;
          this.gameResult = 0;
          this.endCause = "Draw by 3-fold repitition";
          return true;
        }
      }
    }
    
    if (!(null == this.pastPositions)) {
      for (Board pastPosition : this.pastPositions) {
        if (this.position.equals(pastPosition)) {
          this.duplicatedPositions.add(new Board(this.position));
        }
      }
    }
    
    //checkmate/stalemate
    if (!this.position.anyMoves()) {
      this.position.gameOver = true;
      if (this.position.inCheck) {
        //checkmate
        if (this.position.toMove) {
          this.gameResult = -1;
          this.endCause = "Black wins by Checkmate";
        }
        else {
          this.gameResult = 1;
          this.endCause = "White wins by Checkmate";
        }
      }
      else {
        //stalemate
        this.gameResult = 0;
        this.endCause = "Draw by Stalemate";
      }
      return true;
    }
    
    //insufficient material
    // occurs when the non-kings are incapable of attacking 2 consecutive squares
    if (!this.position.isSufficientMaterial()) {
      this.position.gameOver = true;
      this.gameResult = 0;
      this.endCause = "Draw by insufficient material";
    }
    
    return false;
  }
  
  public boolean promotePiece(char piece) {
    Board board = this.position;
    if (board.promotionAvailable && (this.promotionOptions.indexOf(piece) != -1)) {
      int[] square = board.promotionSquare;
      int side = board.boardstate[square[0]][square[1]].side;
      board.boardstate[square[0]][square[1]] = new Piece((side==1) ? Character.toUpperCase(piece) : piece);
      board.promotionAvailable = false;
      board.promotionSquare = new int[] {-1,-1};
      
      //check the new position for check/checkmate
      board.detectCheck();
      this.checkResult();
      
      //add the current position to the past positions
      this.pastPositions.add(new Board(board));
      
      return true;
    }
    return false;
  }
}
