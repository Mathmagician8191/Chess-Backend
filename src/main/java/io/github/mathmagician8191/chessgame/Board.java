package io.github.mathmagician8191.chessgame;

import java.util.Arrays;

public class Board {
  /*
  Implements logic for a chess game, with an internal representation of the
  board and move validation. It also features the ability to convert to and from
  the common board representation FEN (Forsyth Edwards Notation).
  */
  
  //board size
  public int width;
  public int height;
  
  //array of pieces in the board
  public Piece[][] boardstate;
  
  //other game info
  public boolean toMove; //white to move=true
  boolean[] castleRights;
  int[] enPassant; //square for en passant capture
  public int halfmoveClock; //half moves since last capture/pawn move
  public int moves;
  
  public boolean inCheck; //whether the side to move is in check
  public boolean gameOver;
  
  //options for moving
  int pawnRow; //max row the pawns can n-move from
  int pawnSquares; //number of squares the pawns can move on their first move
  int queenRookColumn;
  int kingRookColumn;
  
  //promotion info
  public boolean promotionAvailable;
  int[] promotionSquare;
  
  //useful extra info
  public int[] whiteKingLocation;
  public int[] blackKingLocation;
  
  public Board(String fen,int pawnRow,int pawnSquares,int queenRookColumn,
      int kingRookColumn) {
    this.gameOver = false;
    this.promotionAvailable = false;
    this.promotionSquare = new int[] {-1,-1};
    this.pawnRow = pawnRow;
    this.pawnSquares = pawnSquares;
    
    //subtract 1 to 0-index rather than 1-index
    this.queenRookColumn = queenRookColumn-1;
    this.kingRookColumn = kingRookColumn-1;
    
    //FEN processing
    
    //split the FEN into the different information it provides
    String[] subsections = fen.split(" ");
    
    //figure out the number of rows on the board
    String[] rows = subsections[0].split("/");
    height = rows.length;
    
    //figure out the number of columns on the board
    int rowLetters = rows[0].length();
    width = 0;
    int extraSquares = 0;
    for (int i=0;i<rowLetters;i++) {
      char piece = rows[0].charAt(i);
      if (Character.isDigit(piece)) {
        extraSquares *= 10;
        extraSquares += Character.getNumericValue(piece);
      }
      else {
        //add the squares from the number and the one from the piece
        width += extraSquares + 1;
        extraSquares = 0;
      }
    }
    width += extraSquares;
    
    //set up a loop to decode the FEN board state
    this.boardstate = new Piece[width][height];
    int row = height-1;
    //decode the FEN board state
    for (int i=row;i>=0;i--) {
      int column = 0;
      int squares = 0;
      String rowData = rows[height-i-1];
      int length = rowData.length();
      for (int j=0;j<length;j++) {
        char piece = rowData.charAt(j);
        if (Character.isDigit(piece)) {
          squares *= 10;
          squares += Character.getNumericValue(piece);
        }
        else {
          if (squares > 0) {
            for (int k=0;k<squares;k++) {
              this.boardstate[column][i] = Piece.square;
              column++;
            }
            squares = 0;
          }
          if (Character.toLowerCase(piece)=='k') {
            //update king location
            if (Character.toLowerCase(piece)==piece) {
              this.blackKingLocation = new int[] {column,i};
            }
            else {
              this.whiteKingLocation = new int[] {column,i};
            }
          }
          this.boardstate[column][i] = new Piece(piece);
          column++;
        }
      }
      if (squares > 0) {
        for (int j=0;j<squares;j++) {
          this.boardstate[column][i] = Piece.square;
          column++;
        }
      }
    }
    
    //side to move
    this.toMove = subsections[1].equals("w");

    //castling rights
    this.castleRights = new boolean[]{false,false,false,false};
    if (!subsections[2].equals("-")) {
      int castles = subsections[2].length();
      for (int i=0;i<castles;i++) {
        char castle = subsections[2].charAt(i);
        switch (castle) {
          case 'K':
            this.castleRights[0] = true;
            break;
          case 'Q':
            this.castleRights[1] = true;
            break;
          case 'k':
            this.castleRights[2] = true;
            break;
          case 'q':
            this.castleRights[3] = true;
            break;
        }
      }
    }

    //en passsant square (if present). {-1,-1,-1} means no en passant available
    this.enPassant = new int[]{-1,-1,-1};
    if (!subsections[3].equals("-")) {
      int[] coordinates = Board.algebraicToNumber(subsections[3]);
      this.enPassant = new int[] {coordinates[0],coordinates[1],coordinates[1]};
    }

    //half move clock
    this.halfmoveClock = Integer.parseInt(subsections[4]);

    //full moves
    this.moves = Integer.parseInt(subsections[5]);
    
    this.detectCheck();
  }
  
  public Board(Board original) {
    //shallow copies of variables that pass by value
    this.width = original.width;
    this.height = original.height;
    
    this.toMove = original.toMove;
    
    this.halfmoveClock = original.halfmoveClock;
    this.moves = original.moves;
    
    this.inCheck = original.inCheck;
    this.gameOver = original.gameOver;
    
    this.pawnRow = original.pawnRow;
    this.pawnSquares = original.pawnSquares;
    this.kingRookColumn = original.kingRookColumn;
    this.queenRookColumn = original.queenRookColumn;
    
    this.promotionAvailable = original.promotionAvailable;
    
    //deep copies of variables that pass by reference
    this.castleRights = new boolean[] {
      original.castleRights[0],
      original.castleRights[1],
      original.castleRights[2],
      original.castleRights[3]
    };
    this.enPassant = new int[] {
      original.enPassant[0],
      original.enPassant[1],
      original.enPassant[2]
    };
    this.promotionSquare = new int[] {
      original.promotionSquare[0],
      original.promotionSquare[1]
    };
    this.whiteKingLocation = new int[] {
      original.whiteKingLocation[0],
      original.whiteKingLocation[1]
    };
    this.blackKingLocation = new int[] {
      original.blackKingLocation[0],
      original.blackKingLocation[1]
    };
    this.boardstate = new Piece[this.width][this.height];
    for (int i=0;i<this.width;i++) {
      for (int j=0;j<this.height;j++) {
        this.boardstate[i][j] = original.boardstate[i][j];
      }
    }
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
    return this.equals((Board) other);
  }
  
  public boolean equals(Board other) {
    //detects whether 2 positions are the same
    if (this==other) {
      return true;
    }
    if (other == null) {
      return false;
    }
    
    //test primitives for equality
    if (this.width!=other.width || this.height!=other.height ||
        this.toMove!=other.toMove || this.inCheck!=other.inCheck ||
        this.gameOver != other.gameOver || this.pawnRow!=other.pawnRow ||
        this.pawnSquares!=other.pawnSquares ||
        this.queenRookColumn!=other.queenRookColumn ||
        this.kingRookColumn!=other.kingRookColumn) {
      return false;
    }
    
    //test arrays for equality
    if (!Arrays.equals(this.castleRights,other.castleRights) ||
        !Arrays.equals(this.enPassant,other.enPassant) ||
        !Arrays.equals(this.whiteKingLocation,other.whiteKingLocation) ||
        !Arrays.equals(this.blackKingLocation,other.blackKingLocation)) {
      return false;
    }
    
    //test boardstate for equality
    return Arrays.deepEquals(this.boardstate, other.boardstate);
  }
  
  public String toFen() {
    String result = "";
    //piece arrangement
    int row = this.height-1;
    int column = 0;
    while (row>=0) {
      int emptySquares = 0;
      while (column < width) {
        Piece piece = this.boardstate[column][row];
        if (piece.isPiece) {
          if (emptySquares >  0) {
            result += Integer.toString(emptySquares);
            emptySquares = 0;
          }
          result += piece.side==1 ? Character.toUpperCase(piece.letter) : piece.letter;
        }
        else {
          emptySquares++;
        }
        column++;
      }
      if (emptySquares >  0) {
        result += Integer.toString(emptySquares);
      }
      result += "/";
      row--;
      column = 0;
    }

    //remove trailing slash
    result = result.substring(0,result.length()-1);

    //side to move
    result += this.toMove ? " w " : " b ";

    //castling rights
    String castleRooks = "";
    String[] rooks = {"K","Q","k","q"};
    for (int i=0;i<4;i++) {
      if (castleRights[i]) {
        castleRooks += rooks[i];
      }
    }
    result += castleRooks;

    if (castleRooks.equals("")) {
      result += "-";
    }

    //en passant square
    if (Arrays.equals(this.enPassant,new int[]{-1,-1,-1})) {
      result += " -";
    }
    else {
      result += " " + numberToAlgebraic(this.enPassant);
    }

    //half/full move clock
    result += " " + Integer.toString(this.halfmoveClock) + " " +
        Integer.toString(this.moves);
    return result;
  }

  //converts algebraic notation into lookup coordinates
  public static int[] algebraicToNumber(String algebraic) {
    int[] result = new int[2];
    //find index of letter in the alphabet
    result[0] = ((int) algebraic.charAt(0))-97;
    //-1 to shift from 1-indexed to 0-indexed
    result[1] = Integer.parseInt(algebraic.substring(1))-1;
    return result;
  }

  //converts coordinates to algebraic
  public static String numberToAlgebraic(int[] number) {
    //TODO: make work for values more than 25
    // (then update decode function to match)
    String result = String.valueOf((char) (number[0]+97));
    result += Integer.toString(number[1]+1);
    return result;
  }
  
  public Board getMove(int[] startSquare,int[] endSquare) {
    if (this.gameOver || this.promotionAvailable) {
      return null;
    }
    
    if (endSquare[0]<0||endSquare[1]<0||endSquare[0]>=this.width||endSquare[1]>=this.height) {
      //end Square is out-of-bounds
      return null;
    }
    
    Piece piece = this.getSquare(startSquare[0],startSquare[1]);
    Piece capture = this.boardstate[endSquare[0]][endSquare[1]];

    //test to make sure you're moving your own piece
    if (piece.side != (toMove ? 1 : -1)) {
      return null;
    }

    //test if trying to capture own piece
    if (capture.side == piece.side) {
      return null;
    }
    
    boolean result = this.validSquare(startSquare,endSquare,piece.letter,piece.side,
        capture);
    
    if (result) {
      //test for check
      Board moved = new Board(this);
      moved.movePiece(startSquare, endSquare);
      if (this.toMove) {
        if (moved.isAttacked(moved.whiteKingLocation,moved.toMove)) {
          return null;
        }
        else {
          return moved;
        }
      }
      else {
        if (moved.isAttacked(moved.blackKingLocation, moved.toMove)) {
          return null;
        }
        else {
          return moved;
        }
      }
    }
    else {
      return null;
    }
  }
  
  //boolean returns whether the move is legal
  public boolean isMoveValid(int[] startSquare,int[] endSquare) {
    //return this.getMove(startSquare, endSquare) != null; //3% slower
    
    if (this.gameOver || this.promotionAvailable) {
      return false;
    }
    
    if (endSquare[0]<0||endSquare[1]<0||endSquare[0]>=this.width||endSquare[1]>=this.height) {
      //end Square is out-of-bounds
      return false;
    }
    
    Piece piece = this.getSquare(startSquare[0],startSquare[1]);
    Piece capture = this.boardstate[endSquare[0]][endSquare[1]];

    //test to make sure you're moving your own piece
    if (piece.side != (toMove ? 1 : -1)) {
      return false;
    }

    //test if trying to capture own piece
    if (capture.side == piece.side) {
      return false;
    }
    
    boolean result = this.validSquare(startSquare,endSquare,piece.letter,piece.side,
        capture);
    
    if (result) {
      //test for check
      Board moved = new Board(this);
      moved.movePiece(startSquare, endSquare);
      if (this.toMove) {
        return !moved.isAttacked(moved.whiteKingLocation,moved.toMove);
      }
      else {
        return !moved.isAttacked(moved.blackKingLocation, moved.toMove);
      }
    }
    else {
      return false;
    }
  }
  
  public boolean validSquare(int[] startSquare,int[]endSquare,char letter,
      int side,Piece capture) {
    //rows and columns moved
    int rowDiff = Math.abs(startSquare[1]-endSquare[1]);
    int columnDiff = Math.abs(startSquare[0]-endSquare[0]);
    switch (letter) {
      //jumping pieces
      case 'n':
        return (rowDiff==2 && columnDiff==1) || (rowDiff==1 && columnDiff==2);
      case 'l':
        return (rowDiff==3 && columnDiff==1) || (rowDiff==1 && columnDiff==3);
      case 'z':
        return (rowDiff==3 && columnDiff==2) || (rowDiff==2 && columnDiff==3);
      case 'x':
        return rowDiff<=1 && columnDiff <= 1;
      case 'f':
        return rowDiff==1 && columnDiff==1;
      case 'w':
        return (rowDiff==0 && columnDiff==1) || (rowDiff==1 && columnDiff==0);
      case 'h':
        return (rowDiff==columnDiff && rowDiff<=2) || (rowDiff==0 && columnDiff<=2) ||
            (columnDiff==0 && rowDiff <=2);
      
      //pawn
      case 'p':
        //move forward test (columns are the same)
        if (columnDiff==0) {
          if (capture.isPiece) {
            //pawn cannot capture moving forwards
            return false;
          }
          
          //squares moved forwards
          int squaresMoved = this.toMove ? endSquare[1]-startSquare[1] :
              startSquare[1]-endSquare[1];
          int dy = toMove ? 1 : -1;
          if (squaresMoved > 1) {
            if (squaresMoved > this.pawnSquares) {
              //pawn is moving too far
              return false;
            }
            int squaresFromBack = side == 1 ? startSquare[1]+1 : this.height-startSquare[1];
            if (squaresFromBack>this.pawnRow) {
              //pawn has already moved too far
              return false;
            }
            for (int i=1;i<squaresMoved;i++) {
              int row = startSquare[1]+(i*dy);
              if (this.boardstate[startSquare[0]][row].isPiece) {
                //there is a piece in the way
                return false;
              }
            }
          }
          return !(squaresMoved < 0);
        }
        //test for capture
        else {
          //if not taking something, can't go diagonally
          if (!(capture.isPiece || this.validEnPassant(endSquare))) {
            return false;
          }
          //squares moved forwards
          int squaresMoved = this.toMove ? endSquare[1]-startSquare[1] :
              startSquare[1]-endSquare[1];
          //move is valid if it goes 1 square forward
          if (squaresMoved == 1) {
            //if moving 1 column away, move is valid
            return columnDiff==1;
          }
          else {
            return false;
          }
        }
      
      //king
      case 'k':
        if (rowDiff <= 1 && columnDiff <= 1) {
          return true;
        }
        else {
          //test for castling
          if (columnDiff==2 && rowDiff==0) {
            //test for valid castle
            if (endSquare[0]>startSquare[0]) {
              //kingside castle
              if (castleRights[toMove?0:2]) {
                //test for squares in the way of the rook/king
                for (int i=startSquare[0]+1;i<kingRookColumn;i++) {
                  if (this.boardstate[i][startSquare[1]].isPiece) {
                    return false;
                  }
                }
                if (this.isAttacked(new int[] {(endSquare[0]+startSquare[0])/2,startSquare[1]},
                    !this.toMove)) {
                  //moving through check
                  return false;
                }
                return !inCheck;
              }
              else {
                return false;
              }
            }
            else {
              //queenside castle
              if (castleRights[toMove?1:3]) {
                //test for squares in the way of the rook/king
                for (int i=startSquare[0]-1;i>queenRookColumn;i--) {
                  if (this.boardstate[i][startSquare[1]].isPiece) {
                    return false;
                  }
                }
                if (this.isAttacked(new int[] {(endSquare[0]+startSquare[0])/2,startSquare[1]},
                    !this.toMove)) {
                  //moving through check
                  return false;
                }
                return !inCheck;
              }
              else {
                return false;
              }
            }
          }
          else {
            return false;
          }
        }
      
      //ray attack pieces
      case 'b':
        if (rowDiff==columnDiff) {
          //test for pieces in the way
          int squaresMoved = rowDiff;
          return validRay(startSquare,endSquare,squaresMoved);
        }
        else {
          //not diagonal
          return false;
        }
      case 'r':
        if (columnDiff==0 || rowDiff==0) {
          //test for pieces in the way
          int squaresMoved = Math.max(columnDiff,rowDiff);
          return validRay(startSquare,endSquare,squaresMoved);
        }
        else {
          //not straight line
          return false;
        }
      case 'i':
        //TODO: add nightirder moves
        if (rowDiff==2*columnDiff || columnDiff==2*rowDiff) {
          //moving in a 2-1 ratio, need to check for pieces in the way
          int squaresMoved = Math.min(rowDiff,columnDiff);
          return validRay(startSquare,endSquare,squaresMoved);
        }
        return false;
      
      //combination movers
      case 'q':
        return (validSquare(startSquare,endSquare,'b',side,capture))
            || (validSquare(startSquare,endSquare,'r',side,capture));
      case 'a':
        return (validSquare(startSquare,endSquare,'b',side,capture))
            || (validSquare(startSquare,endSquare,'n',side,capture));
      case 'c':
        return (validSquare(startSquare,endSquare,'r',side,capture))
            || (validSquare(startSquare,endSquare,'n',side,capture));
      case 'm':
        return (validSquare(startSquare,endSquare,'b',side,capture))
            || (validSquare(startSquare,endSquare,'r',side,capture))
            || (validSquare(startSquare,endSquare,'n',side,capture));
      
      //obstacle
      case 'o':
        return !capture.isPiece;
      
      //any other piece we don't know, so it can go wherever
      default:
        return true;
    }
  }
  
  public boolean validRay(int[] startSquare,int[] endSquare,int squaresMoved) {
    int dx = (endSquare[0]-startSquare[0])/squaresMoved;
    int dy = (endSquare[1]-startSquare[1])/squaresMoved;
    for (int i=1;i<squaresMoved;i++) {
      int column = startSquare[0] + (i*dx);
      int row = startSquare[1] + (i*dy);
      if (this.boardstate[column][row].isPiece) {
        //a piece is in the way of the move
        return false;
      }
    }
    return true;
  }
  
  public boolean isAttacked(int[] square,boolean side) {
    int direction = side ? 1 : -1;
    
    //test for jump attacks
    Piece[] knightJumps = this.allJumps(square, 2, 1);
    int knightJumpCount = knightJumps.length;
    for (int i=0;i<knightJumpCount;i++) {
      Piece jumpTarget = knightJumps[i];
      if (jumpTarget.side==direction) {
        switch (jumpTarget.letter) {
          case 'n':
          case 'c':
          case 'a':
          case 'i':
          case 'm':
            return true;
        }
      }
    }
    
    Piece[] camelJumps = this.allJumps(square, 3, 1);
    int camelJumpCount = camelJumps.length;
    for (int i=0;i<camelJumpCount;i++) {
      Piece jumpTarget = camelJumps[i];
      if (jumpTarget.side==direction) {
        switch (jumpTarget.letter) {
          case 'l':
            return true;
        }
      }
    }
    
    Piece[] zebraJumps = this.allJumps(square, 3, 2);
    int zebraJumpCount = zebraJumps.length;
    for (int i=0;i<zebraJumpCount;i++) {
      Piece jumpTarget = zebraJumps[i];
      if (jumpTarget.side==direction) {
        switch (jumpTarget.letter) {
          case 'z':
            return true;
        }
      }
    }
    
    Piece[] straightJumps = this.allJumps(square, 1, 0);
    int straightJumpCount = straightJumps.length;
    for (int i=0;i<straightJumpCount;i++) {
      Piece jumpTarget = straightJumps[i];
      if (jumpTarget.side==direction) {
        switch (jumpTarget.letter) {
          case 'h':
          case 'x':
          case 'w':
          case 'k':
          case 'r':
          case 'q':
          case 'c':
          case 'm':
            return true;
        }
      }
    }
    Piece[] straighterJumps = this.allJumps(square, 2, 0);
    int straighterJumpCount = straighterJumps.length;
    for (int i=0;i<straighterJumpCount;i++) {
      Piece jumpTarget = straighterJumps[i];
      if (jumpTarget.side==direction) {
        switch (jumpTarget.letter) {
          case 'h':
            return true;
        }
      }
    }
    
    Piece[] diagonalJumps = this.allJumps(square, 1, 1);
    int diagonalJumpCount = diagonalJumps.length;
    for (int i=0;i<diagonalJumpCount;i++) {
      Piece jumpTarget = diagonalJumps[i];
      if (jumpTarget.side==direction) {
        switch (jumpTarget.letter) {
          case 'h':
          case 'x':
          case 'f':
          case 'k':
          case 'b':
          case 'q':
          case 'a':
          case 'm':
            return true;
        }
      }
    }
    Piece[] diagonalerJumps = this.allJumps(square, 2, 2);
    int diagonalerJumpCount = diagonalerJumps.length;
    for (int i=0;i<diagonalerJumpCount;i++) {
      Piece jumpTarget = diagonalerJumps[i];
      if (jumpTarget.side==direction) {
        switch (jumpTarget.letter) {
          case 'h':
            return true;
        }
      }
    }
    
    //test for pawn check
    Piece left = this.getSquare(square[0]-1,square[1]-direction);
    Piece right = this.getSquare(square[0]+1, square[1]-direction);
    if ((left.side==direction&&left.letter=='p')||(right.side==direction&&right.letter=='p')) {
      return true;
    }
    
    //rook attacks
    Piece[] rookRays = this.allRays(square,1,0);
    int rookRayCount = rookRays.length;
    for (int i=0;i<rookRayCount;i++) {
      Piece rayTarget = rookRays[i];
      if (rayTarget.side==direction) {
        switch (rayTarget.letter) {
          case 'r':
          case 'q':
          case 'c':
          case 'm':
            //a rook-attacking piece is on a straight line
            return true;
        }
      }
    }
    
    //bishop attacks
    Piece[] bishopRays = this.allRays(square,1,1);
    int bishopRayCount = bishopRays.length;
    for (int i=0;i<bishopRayCount;i++) {
      Piece rayTarget = bishopRays[i];
      if (rayTarget.side==direction) {
        switch (rayTarget.letter) {
          case 'b':
          case 'q':
          case 'a':
          case 'm':
            //a bishop-attacking piece is on a diagonal
            return true;
        }
      }
    }
    
    //nightrider attacks
    Piece[] nightriderRays = this.allRays(square,2,1);
    int nightriderRayCount = nightriderRays.length;
    for (int i=0;i<nightriderRayCount;i++) {
      Piece rayTarget = nightriderRays[i];
      if (rayTarget.side==direction) {
        if (rayTarget.letter=='i') {
          return true;
        }
      }
    }
    
    return false;
  }
  
  public Piece[] allJumps(int[] square,int dx,int dy) {
    if (dx==dy) {
      return new Piece[] {
        this.getSquare(square[0]+dx,square[1]+dy),
        this.getSquare(square[0]-dx,square[1]+dy),
        this.getSquare(square[0]+dx,square[1]-dy),
        this.getSquare(square[0]-dx,square[1]-dy)
      };
    }
    else {
      return new Piece[] {
        this.getSquare(square[0]+dx,square[1]+dy),
        this.getSquare(square[0]-dx,square[1]+dy),
        this.getSquare(square[0]+dx,square[1]-dy),
        this.getSquare(square[0]-dx,square[1]-dy),
        this.getSquare(square[0]+dy,square[1]+dx),
        this.getSquare(square[0]-dy,square[1]+dx),
        this.getSquare(square[0]+dy,square[1]-dx),
        this.getSquare(square[0]-dy,square[1]-dx)
      };
    }
  }
  
  public Piece[] allRays(int[] square,int dx,int dy) {
    //cast rays in all directions until it hits something
    if (dx==dy) {
      //send out 4 rays
      return new Piece[] {
        this.rayTarget(square,dx,dy),
        this.rayTarget(square,-dx,dy),
        this.rayTarget(square,dx,-dy),
        this.rayTarget(square,-dx,-dy)
      };
    }
    else {
      //send out 8 rays
      return new Piece[] {
        this.rayTarget(square,dx,dy),
        this.rayTarget(square,-dx,dy),
        this.rayTarget(square,dx,-dy),
        this.rayTarget(square,-dx,-dy),
        this.rayTarget(square,dy,dx),
        this.rayTarget(square,-dy,dx),
        this.rayTarget(square,dy,-dx),
        this.rayTarget(square,-dy,-dx)
      };
    }
  }
  
  public Piece rayTarget(int[] square,int dx,int dy) {
    //follow ray until either a piece ir the edge of the board is reached
    int i=0;
    while (true) {
      i++;
      int column = square[0]+i*dx;
      int row = square[1]+i*dy;
      if (column<0 || row<0 || column>=this.width || row>=this.height) {
        //have gone off board
        return Piece.square;
      }
      Piece testLocation = this.boardstate[column][row];
      if (testLocation.isPiece) {
        return testLocation;
      }
    }
  }
  
  public Piece getSquare(int x,int y) {
    if (x<0 || y<0 || x>=this.width || y>=this.height) {
      return Piece.square;
    }
    return this.boardstate[x][y];
  }
  
  public boolean validEnPassant(int[] square) {
    return (square[0]==this.enPassant[0])&&(square[1]>=this.enPassant[1])&&
        (square[1]<=this.enPassant[2]);
  }

  public void movePiece(int[] startSquare,int[] endSquare) {
    this.halfmoveClock++;
    
    //change side to move
    this.toMove = !this.toMove;
    if (this.toMove) {
      this.moves++;
    }
    
    //piece capturing
    Piece piece = this.boardstate[startSquare[0]][startSquare[1]];
    
    //change in row/column
    int columnDiff = Math.abs(startSquare[0]-endSquare[0]);
    int rowDiff = Math.abs(startSquare[1]-endSquare[1]);
    
    //reset en passant square if a pawn is not moving
    //the switch statement will reset the en passant square if a pawn is moving
    if (!(piece.letter=='p')) {
      this.enPassant = new int[]{-1,-1,-1};
    }
    switch (piece.letter) {
      case 'p':
        //detected en passant
        if (columnDiff==1 && !this.boardstate[endSquare[0]][endSquare[1]].isPiece) {
          //en passant capture has occured
          int rowOfPawn = this.toMove ? this.enPassant[2]+1 : this.enPassant[1]-1;
          this.boardstate[endSquare[0]][rowOfPawn] = Piece.square;
        }
        
        //en passant square detection
        if (rowDiff>1) {
          if (this.toMove) {
            //black moved last
            this.enPassant = new int[] {startSquare[0],endSquare[1]+1,startSquare[1]-1};
          }
          else {
            //white moved last
            this.enPassant = new int[] {startSquare[0],startSquare[1]+1,endSquare[1]-1};
          }
        }
        else {
          //no double move occured
          this.enPassant = new int[]{-1,-1,-1};
        }
        
        //pawn promotion detection
        int squaresFromBack = piece.side == 1 ? endSquare[1]+1 : this.height-endSquare[1];
        if (squaresFromBack == this.height) {
          this.promotionAvailable = true;
          this.promotionSquare = new int[] {
            endSquare[0],
            endSquare[1]
          };
        }
        
        
        this.halfmoveClock = 0;
        break;
      case 'k':
        //update king location
        if (this.toMove) {
          blackKingLocation = endSquare;
        }
        else {
          whiteKingLocation = endSquare;
        }
        
        //loss of castling due to king moves
        if (piece.side == 1) {
          this.castleRights[0] = false;
          this.castleRights[1] = false;
        }
        else {
          this.castleRights[2] = false;
          this.castleRights[3] = false;
        }
        if (columnDiff==2) {
          if (endSquare[0]>startSquare[0]) {
            //kingside castle
            Piece kingRook = this.boardstate[kingRookColumn][startSquare[1]];
            this.boardstate[endSquare[0]-1][startSquare[1]] = kingRook;
            this.boardstate[kingRookColumn][startSquare[1]] = Piece.square;
          }
          else {
            //queenside castle
            Piece queenRook = this.boardstate[queenRookColumn][startSquare[1]];
            this.boardstate[endSquare[0]+1][startSquare[1]] = queenRook;
            this.boardstate[queenRookColumn][startSquare[1]] = Piece.square;
          }
        }
        break;
    }
    //check for white loss of castle due to castle piece move
    int kingRow = toMove ? blackKingLocation[1] : whiteKingLocation[1];
    if (startSquare[1]==kingRow) {
      if (startSquare[0]==kingRookColumn) {
        this.castleRights[toMove?2:0] = false;
      }
      else if (startSquare[0]==queenRookColumn) {
        this.castleRights[toMove?3:1] = false;
      }
    }
    //reset halfmoveClock in case of capture
    if (this.boardstate[endSquare[0]][endSquare[1]].isPiece) {
      this.halfmoveClock = 0;
      //detect castling piece capture
      if (this.toMove) {
        if (Arrays.equals(endSquare,new int[] {kingRookColumn,whiteKingLocation[1]})) {
          this.castleRights[0] = false;
        }
        else if (Arrays.equals(endSquare,new int[] {queenRookColumn,whiteKingLocation[1]})) {
          this.castleRights[1] = false;
        }
      }
      else {
        if (Arrays.equals(endSquare,new int[] {kingRookColumn,blackKingLocation[1]})) {
          this.castleRights[2] = false;
        }
        else if (Arrays.equals(endSquare,new int[] {queenRookColumn,blackKingLocation[1]})) {
          this.castleRights[3] = false;
        }
      }
    }
    //replace piece in destination with moving piece
    this.boardstate[endSquare[0]][endSquare[1]] = piece;
    //empty start square
    this.boardstate[startSquare[0]][startSquare[1]] = Piece.square;
    
    this.detectCheck();
  }
  
  public void detectCheck() {
    this.inCheck = this.toMove ? this.isAttacked(whiteKingLocation,!this.toMove) :
        this.isAttacked(blackKingLocation,!this.toMove);
  }
  
  public boolean anyMoves() {
    //if a promotion is available, the promotion needs to be played first
    if (this.promotionAvailable) {
      return true;
    }
    
    //checkmate/stalemate
    for (int i=0;i<this.width;i++) {
      for (int j=0;j<this.height;j++) {
        Piece target = this.boardstate[i][j];
        if (target.isPiece && target.side == (this.toMove ? 1 : -1)) {
          //test for any valid moves
          for (int k=0;k<this.width;k++) {
            for (int l=0;l<this.height;l++) {
              if (this.isMoveValid(new int[] {i,j},new int[] {k,l})) {
                return true;
              }
            }
          }
        }
      }
    }
    return false;
  }
  
  public boolean isSufficientMaterial() {
    int colourBoundWhite = 0;
    int colourBoundBlack = 0;
    int otherPieces = 0;
    int barriers = 0;
    for (int i=0;i<this.width;i++) {
      for (int j=0;j<this.height;j++) {
        Piece piece = this.boardstate[i][j];
        if (piece.isPiece) {
          switch (piece.letter) {
            case 'm':
            case 'q':
            case 'c':
            case 'a':
            case 'r':
            case 'h':
            case 'x':
            case 'p':
              //these pieces can mate alone
              return true;
            case 'n':
            case 'w':
            case 'z':
            case 'i':
              otherPieces++;
              break;
            case 'b':
            case 'l':
            case 'f':
              //colour-bound pieces
              if ((i+j)%2==1) {
                colourBoundWhite++;
              }
              else {
                colourBoundBlack++;
              }
              break;
            case 'o':
              barriers++;
            case 'k':
              //ignored as irrelevant
              break;
            default:
              //assume any other piece can mate alone
              return true;
          }
        }
      }
    }
    if (otherPieces >= 2) {
      return true;
    }
    else if (otherPieces==1) {
      //detect if a colour bound piece exists to mate with
      return (colourBoundWhite > 0) || (colourBoundBlack > 0) || (barriers > 0);
    }
    else {
      //detect if colurbound pieces exist on both colours
      int attacks = 0;
      attacks += (colourBoundWhite > 0) ? 1 : 0;
      attacks += (colourBoundBlack > 0) ? 1 : 0;
      attacks += (barriers > 0) ? 1 : 0;
      return  attacks >= 2;
    }
  }
}