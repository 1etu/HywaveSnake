package net.hywave.snake.model;

import lombok.Getter;
import lombok.Setter;
import java.util.LinkedList;
import java.util.List;

public class Snake {
    @Getter
    private final LinkedList<Position> body = new LinkedList<>();
    
    @Getter @Setter
    private Direction direction;
    
    @Getter @Setter
    private Direction lastMovedDirection;
    
    @Getter @Setter
    private boolean alive = true;
    
    public Snake(Position initialPosition, Direction initialDirection) {
        body.add(initialPosition);
        this.direction = initialDirection;
        this.lastMovedDirection = initialDirection;
    }
    
    public Position getHead() {
        return body.getFirst();
    }
    
    public Position getTail() {
        return body.getLast();
    }
    
    public void move() {
        Position newHead = getHead().add(direction);
        body.addFirst(newHead);
        lastMovedDirection = direction;
    }
    
    public void grow() {}
    
    public void shrink() {
        if (!body.isEmpty()) {
            body.removeLast();
        }
    }
    
    public boolean collides() {
        if (body.size() <= 1) {
            return false;
        }
        
        Position head = getHead();
        List<Position> bodyWithoutHead = body.subList(1, body.size());
        
        return bodyWithoutHead.stream()
            .anyMatch(pos -> pos.getX() == head.getX() && pos.getY() == head.getY());
    }
    
    public boolean isValidMove(Direction newDirection) {
        return newDirection != lastMovedDirection.getOpposite();
    }
} 