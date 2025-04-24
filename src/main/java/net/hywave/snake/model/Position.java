package net.hywave.snake.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Position {
    private int x;
    private int y;
    
    public Position copy() {
        return new Position(x, y);
    }
    
    public Position add(Direction direction) {
        switch (direction) {
            case UP:
                return new Position(x, y - 1);
            case RIGHT:
                return new Position(x + 1, y);
            case DOWN:
                return new Position(x, y + 1);
            case LEFT:
                return new Position(x - 1, y);
            default:
                return this.copy();
        }
    }
} 