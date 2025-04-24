package net.hywave.snake.db.player;

import lombok.Getter;

import java.text.SimpleDateFormat;
import java.util.Date;

@Getter
public class PlayerScore {
    private final String playerName;
    private final int score;
    private final int length;
    private final int durationSeconds;
    private final long timestamp;
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public PlayerScore(String playerName, int score, int length, int durationSeconds, long timestamp) {
        this.playerName = playerName;
        this.score = score;
        this.length = length;
        this.durationSeconds = durationSeconds;
        this.timestamp = timestamp;
    }
    
    public String getFormattedTime() {
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    public String getFormattedDate() {
        return DATE_FORMAT.format(new Date(timestamp));
    }
    
    @Override
    public String toString() {
        return String.format("%s - Score: %d, Length: %d, Time: %s", 
                playerName, score, length, getFormattedTime());
    }
} 