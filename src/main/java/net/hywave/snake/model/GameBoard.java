package net.hywave.snake.model;

import lombok.Getter;

public class GameBoard {
    
    @Getter
    private final int width;
    
    @Getter
    private final int height;
    
    public GameBoard() {
        this(20, 20);
    }
    
    public GameBoard(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    public boolean isInBounds(Position position) {
        return position.getX() >= 0 && position.getX() < width &&
               position.getY() >= 0 && position.getY() < height;
    }
} 