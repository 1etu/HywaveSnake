package net.hywave.snake.model;

public enum Direction {
    UP, RIGHT, DOWN, LEFT;
    
    public Direction getOpposite() {
        switch (this) {
            case UP: return DOWN;
            case RIGHT: return LEFT;
            case DOWN: return UP;
            case LEFT: return RIGHT;
            default: return this;
        }
    }
} 