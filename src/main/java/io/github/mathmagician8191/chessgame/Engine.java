package io.github.mathmagician8191.chessgame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Engine extends Game {
  /*
  Represents a chess engine capable of evaluating and deciding on moves
  Also provides tools to test move generation to ensure its accuracy
  */
  
  public int depth;
  public int quiescenceDepth;
  
  public int rookBonus;
  public int bishopBonus;
  
  //which side the engine plays
  public boolean side;
  
  public Engine(String fen,int pawnRow,int pawnSquares,int queenRookColumn,int kingRookColumn,
      String promotionOptions,int depth,int quiescenceDepth,boolean side) {
    super(fen,pawnRow,pawnSquares,queenRookColumn,kingRookColumn,promotionOptions);
    this.depth = depth;
    this.quiescenceDepth = quiescenceDepth;
    this.side = side;
    
    Board board = this.position;
    
    //rook move bonus based on board size
    this.rookBonus = 2 * board.height + board.width;
    //bishop move bonus based on board size
    this.bishopBonus = 4 * Math.min(board.height,board.width);
  }
  
  public Engine(Engine original) {
    super(original);
    
    this.depth = original.depth;
    this.quiescenceDepth = original.quiescenceDepth;
    
    this.rookBonus = original.rookBonus;
    this.bishopBonus = original.bishopBonus;
    
    this.side = original.side;
  }
  
  public Engine(Game game,int depth,int quiescenceDepth) {
    super(game);
    this.depth = depth;
    this.quiescenceDepth = quiescenceDepth;
    
    Board board = this.position;
    
    //rook move bonus based on board size
    this.rookBonus = 2 * board.height + board.width;
    //bishop move bonus based on board size
    this.bishopBonus = 4 * Math.min(board.height,board.width);
  }
  
  public Game makeMove(Game game) {
    //operates on a game
    return Engine.makeMove(new Engine(game,this.depth,this.quiescenceDepth),this.depth);
  }
  
  public static Engine makeMove(Engine game) {
    return Engine.makeMove(game,game.depth);
  }
  
  public static Engine makeMove(Engine game, int depth) {
    //gets the right move for the position
    
    //starting best-case
    int alpha = Integer.MIN_VALUE+1;
    
    //initialise as no position found yet
    Engine newPosition = null;
    
    ArrayList<Engine> games = game.getMoves();
    
    //randomly orders the list to make it randomly decide between equal moves
    Collections.shuffle(games);
    
    for (Engine newGame : games) {
      int score = -newGame.evaluate(depth-1, Integer.MIN_VALUE+1, -alpha);
      if (score > alpha) {
        alpha = score;
        newPosition = newGame;
      }
      if (newPosition == null) {
        //we have a move that works
        newPosition = newGame;
      }
    }
    if (newPosition == null) {
      //no moves are possible
      return game;
    }
    return newPosition;
  }
  
  public int getScore(int depth) {
    //initialises alpha-beta
    //we add 1 to Integer.MIN_VALUE so we can multiply it by -1 and not overflow
    return this.evaluate(depth,(Integer.MIN_VALUE+1),Integer.MAX_VALUE);
  }
  
  public int evaluate(int depth,int alpha,int beta) {
    //searches and evaluates positions, gives value in centipawns
    
    //save board for further reference
    Board board = this.position;
    
    if (depth == 0) {
      //search has ended
      return this.quiescence(alpha,beta,this.quiescenceDepth);
    }
    //keep searching
    if (board.gameOver) {
      int gameScore = this.gameResult * (board.toMove ? 1 : -1);
      switch (gameScore) {
        case -1:
          return Integer.MIN_VALUE+1+board.moves;
        case 0:
          return 0;
        case 1:
          return Integer.MAX_VALUE-board.moves;
      }
    }
    //iterate over all possible moves
    ArrayList<Engine> games = this.getMoves();
    for (Engine newGame : games) {
      int score = -newGame.evaluate(depth-1,-beta,-alpha);
      if (score >= beta) {
        return beta;
      }
      if (score > alpha) {
        alpha = score;
      }
    }
    
    return alpha;
  }
  
  public int quiescence(int alpha,int beta,int depth) {
    //tests captures from a position so that the program misses less tactical combinations
    //could lead to a search explosion - should limit depth for robustness
    
    //get the idea of what the position is
    int result = this.evaluate();
    
    //if too good, other player won't let it happen
    if (result >= beta) {
      return beta;
    }
    
    //if not too bad, remember this as best achievable
    if (alpha < result) {
      alpha = result;
    }
    
    //if search over, stop
    if (depth==0) {
      return alpha;
    }
    
    //store board state for future reference
    Board board = this.position;
    
    //consider captures only as others are assumed tactically insignificant
    for (int i=0;i<board.width;i++) {
        for (int j=0;j<board.height;j++) {
          Piece piece = board.boardstate[i][j];
          if (piece.isPiece && piece.side == (board.toMove ? 1 : -1)) {
            for (int k=0;k<board.width;k++) {
              for (int l=0;l<board.height;l++) {
                Piece capture = board.boardstate[k][l];
                if (capture.isPiece) {
                  Board newBoard = this.position.getMove(new int[]{i,j},new int[]{k,l});
                  if (newBoard != null) {
                    Engine newGame = new Engine(this);
                    newGame.position = newBoard;
                    if (board.halfmoveClock==0) {
                      newGame.pastPositions = new ArrayList<>();
                      newGame.duplicatedPositions = new ArrayList<>();
                    }

                    newGame.checkResult();

                    newGame.pastPositions.add(new Board(board));
                    
                    if (newGame.position.promotionAvailable) {
                      //check all promotion options
                      String options = newGame.promotionOptions;
                      for (int m=0, length=options.length();m<length;m++) {
                        char letter = options.charAt(m);
                        Engine newerGame = new Engine(newGame);
                        newerGame.promotePiece(letter);
                        int evaluation = -newerGame.quiescence(-beta,-alpha,depth-1);
                        if (evaluation >= beta) {
                          return beta;
                        }
                        if (evaluation > alpha) {
                          alpha = evaluation;
                        }
                      }
                    }
                    else {
                      int evaluation = -newGame.quiescence(-beta,-alpha,depth-1);
                      if (evaluation >= beta) {
                        return beta;
                      }
                      if (evaluation > alpha) {
                        alpha = evaluation;
                      }
                    }
                  }
                }
              }
            }
          }
        }
    }
    
    return alpha;
  }
  
  public int evaluate() {
    //tries to figure out how good a position is for the side to move
    
    //save board for future reference
    Board board = this.position;
    
    //test for an existing game result
    if (board.gameOver) {
      //score for side to move
      int gameScore = this.gameResult * (board.toMove ? 1 : -1);
      switch (gameScore) {
        case -1:
          return Integer.MIN_VALUE+1+board.moves;
        case 0:
          return 0;
        case 1:
          return Integer.MAX_VALUE-board.moves;
      }
    }
    
    //sees how good a position is in centipawns
    int result = 0;
    
    //penalty for being in check
    if (board.inCheck) {
      result += board.toMove ? -75 : 75;
    }
    
    //to see if any side has no pieces left
    int whitePieces = 0;
    int blackPieces = 0;
    
    //iterate over squares
    for (int i=0;i<board.width;i++) {
      for (int j=0;j<board.height;j++) {
        Piece piece = board.boardstate[i][j];
        if (piece.isPiece) {
          if (piece.side==1) {
            whitePieces++;
          }
          else {
            blackPieces++;
          }
          //values dependent on the piece
          int pieceValue;
          int kingThreat;
          int roomNeeded;
          int roomValue;
          int advanceBonus;
          
          //value that can be used by other pieces
          int advancement = (piece.side==1) ? j : board.height-1-j;
          
          switch (piece.letter) {
            case 'm':
              pieceValue = 1200 + this.rookBonus;
              kingThreat = 120;
              roomNeeded = 2;
              roomValue = 25;
              advanceBonus = 1;
              break;
            case 'q':
              pieceValue = 870 + this.rookBonus;
              kingThreat = 90;
              roomNeeded = 1;
              roomValue = 10;
              advanceBonus = 0;
              break;
            case 'c':
              pieceValue = 870 + this.rookBonus;
              kingThreat = 90;
              roomNeeded = 2;
              roomValue = 20;
              advanceBonus = 1;
              break;
            case 'a':
              pieceValue = 800 + this.bishopBonus;
              kingThreat = 90;
              roomNeeded = 2;
              roomValue = 20;
              advanceBonus = 1;
              break;
            case 'h':
              pieceValue = 600;
              kingThreat = 70;
              roomNeeded = 2;
              roomValue = 20;
              advanceBonus = 2;
              break;
            case 'r':
              pieceValue = 470 + this.rookBonus;
              kingThreat = 45;
              roomNeeded = 1;
              roomValue = 5;
              advanceBonus = 0;
              
              //bonus for rook on the 2nd last rank
//              if (advancement==board.height-2) {
//                pieceValue += 10;
//              }
              break;
            case 'i':
              //use the bishop bonus becuase it moves diagonally-ish
              pieceValue = 470 + this.bishopBonus;
              kingThreat = 40;
              roomNeeded = 2;
              roomValue = 9;
              advanceBonus = 1;
              break;
            case 'b':
              pieceValue = 300 + this.bishopBonus;
              kingThreat = 30;
              roomNeeded = 1;
              roomValue = 10;
              advanceBonus = 0;
              break;
            case 'z':
              pieceValue = 320;
              kingThreat= 25;
              roomNeeded = 3;
              roomValue = 13;
              advanceBonus = 2;
              break;
            case 'n':
              pieceValue = 320;
              kingThreat = 35;
              roomNeeded = 2;
              roomValue = 18;
              advanceBonus = 0;
              break;
            case 'x':
              pieceValue = 320;
              kingThreat = 35;
              roomNeeded = 1;
              roomValue = 25;
              advanceBonus = 3;
              break;
            case 'w':
              pieceValue = 200;
              kingThreat = 25;
              roomNeeded = 1;
              roomValue = 25;
              advanceBonus = 2;
              break;
            case 'l':
              pieceValue = 200;
              kingThreat = 15;
              roomNeeded = 3;
              roomValue = 12;
              advanceBonus = 2;
              break;
            case 'f':
              pieceValue = 150;
              kingThreat = 15;
              roomNeeded = 1;
              roomValue = 25;
              advanceBonus = 2;
              break;
            case 'o':
              pieceValue = 150;
              kingThreat = 0;
              roomNeeded = 0;
              roomValue = 0;
              advanceBonus = 0;
              break;
            case 'p':
              pieceValue = 100;
              kingThreat = 15;
              roomNeeded = 1;
              roomValue = 20;
              advanceBonus = 10;
              break;
            case 'k':
              pieceValue = 0;
              kingThreat = 0;
              roomNeeded = 1;
              roomValue = 5;
              //the king does not want to approach the enemy side
              advanceBonus = 0;
              break;
            default:
              //assume some other piece is more valuable than a pawn so its not thrown away
              pieceValue = 110;
              //assume some other piece can attack the king so it moves it there
              kingThreat = 10;
              //keep it away from the edge
              roomNeeded = 2;
              roomValue = 10;
              //advance it
              advanceBonus = 1;
          }
          
          //distance from enemy king
          int kingX;
          int kingY;
          if (piece.side==1) {
            kingX = Math.abs(i-board.blackKingLocation[0]);
            kingY = Math.abs(j-board.blackKingLocation[1]);
          }
          else {
            kingX = Math.abs(i-board.whiteKingLocation[0]);
            kingY = Math.abs(j-board.whiteKingLocation[1]);
          }
          int kingDistance = kingX + kingY;
          //being close to the enemy king threatens it
          int kingDanger = kingDistance < 4 ? 4 - kingDistance : 0;
          
          //distance to nearest edge
          int distanceX = Math.min(i,board.width-1-i);
          int distanceY = Math.min(j,board.height-1-j);
          
          int crampX = distanceX < roomNeeded ? roomNeeded - distanceX : 0;
          int crampY = distanceY < roomNeeded ? roomNeeded - distanceY : 0;
          int cramp = crampX + crampY;
          
          //reduce the board height into graduations, at most 20 total
          //this means that there are no huge advancement bonuses on larger boards
          int advancementCapped = (advancement * 10)/board.height;
          
          result += (pieceValue + (kingDanger*kingThreat) - (cramp*roomValue) + (advancementCapped*advanceBonus)) * piece.side;
        }
      }
    }
    
    if (whitePieces==1) {
      //bonus for black driving the king to the corner
      int kingEdgeness = Math.abs(2*board.whiteKingLocation[0]-board.width+1)
          + Math.abs(2*board.whiteKingLocation[1]-board.height+1);
      //bonus for enemy king being away from the center
      //means winning side will drive enemy king to edge/corner and mate
      result -= 50 * kingEdgeness;
      
      //bonus for kings being close
      int kingCloseness = Math.abs(board.whiteKingLocation[0]-board.blackKingLocation[0])
          + Math.abs(board.whiteKingLocation[1]-board.blackKingLocation[1]);
      int kingDistance = board.height+board.width-kingCloseness;
      //bonus for king distance
      //means winning side keeps kings together and likes simplifying to a lone enemy king
      result -= kingDistance * 50;
    }
    if (blackPieces==1) {
      //bonus for white driving the king to the corner
      int kingEdgeness = Math.abs(2*board.blackKingLocation[0]-board.width+1)
          + Math.abs(2*board.blackKingLocation[1]-board.height+1);
      //bonus for enemy king being away from the center
      //means winning side will drive enemy king to edge/corner and mate
      result += 50 * kingEdgeness;
      
      //bonus for kings being close
      int kingCloseness = Math.abs(board.whiteKingLocation[0]-board.blackKingLocation[0])
          + Math.abs(board.whiteKingLocation[1]-board.blackKingLocation[1]);
      int kingDistance = board.height+board.width-kingCloseness;
      //bonus for king distance
      //means winning side keeps kings together and likes simplifying to a lone enemy king
      result += kingDistance * 50;
    }
    
    return result * (board.toMove ? 1 : -1);
  }
  
  public ArrayList<Engine> getMoves() {
    //gets every resulting game - still needs performance & reliability testing
    ArrayList<Engine> games = new ArrayList<>();
    
    Board board = this.position;
    
    if (board.gameOver) {
      return games;
    }
    
    for (int i=0;i<board.width;i++) {
      for (int j=0;j<board.height;j++) {
        Piece piece = board.boardstate[i][j];
        if (piece.isPiece && piece.side == (board.toMove ? 1 : -1)) {
          for (int k=0;k<board.width;k++) {
            for (int l=0;l<board.height;l++) {
              //how is this slower?
              Board newBoard = this.position.getMove(new int[]{i,j},new int[]{k,l});
              if (newBoard != null) {
                Engine newGame = new Engine(this);
                newGame.position = newBoard;
                if (board.halfmoveClock==0) {
                  newGame.pastPositions = new ArrayList<>();
                  newGame.duplicatedPositions = new ArrayList<>();
                }

                newGame.checkResult();

                newGame.pastPositions.add(new Board(board));
                
                if (board.promotionAvailable && (newGame.promotionOptions.length() == 1)) {
                  newGame.promotePiece(newGame.promotionOptions.charAt(0));
                }
                
                if (newGame.position.promotionAvailable) {
                  //check all promotion options
                  String options = newGame.promotionOptions;
                  for (int m=0, length=options.length();m<length;m++) {
                    char letter = options.charAt(m);
                    Engine newerGame = new Engine(newGame);
                    newerGame.promotePiece(letter);
                    games.add(newerGame);
                  }
                }
                else {
                  games.add(newGame);
                }
              }
            }
          }
        }
      }
    }
    
    return games;
  }
}