import java.util.*;

public class EightQueens {
    public static void main(String[] args) {
        int[][] board = new int[8][8];
        placeFirstQueen(board);
        if (placeQueens(board, 1)) {
            printBoard(board);
        } else {
            System.out.println("No solution exists.");
        }
    }

    public static void placeFirstQueen(int[][] board) {
        Random rand = new Random();
        int col = rand.nextInt(8);
        board[0][col] = 1;
    }

    public static boolean placeQueens(int[][] board, int row) {
        if (row == 8) {
            return true; // all queens have been placed
        }
        for (int col = 0; col < 8; col++) {
            if (isSafe(board, row, col)) {
                board[row][col] = 1; // place the queen
                if (placeQueens(board, row + 1)) {
                    return true; // found a solution
                }
                board[row][col] = 0; // backtrack and try the next column
            }
        }
        return false; // no solution found in this row
    }

    public static boolean isSafe(int[][] board, int row, int col) {
        for (int i = 0; i < row; i++) {
            if (board[i][col] == 1) {
                return false; // there is a queen in the same column
            }
            if (col - (row - i) >= 0 && board[i][col - (row - i)] == 1) {
                return false; // there is a queen in the same diagonal
            }
            if (col + (row - i) < 8 && board[i][col + (row - i)] == 1) {
                return false; // there is a queen in the same diagonal
            }
        }
        return true; // the position is safe
    }

    public static void printBoard(int[][] board) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                System.out.print(board[i][j] + " ");
            }
            System.out.println();
        }
    }
}
