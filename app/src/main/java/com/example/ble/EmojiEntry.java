package com.example.ble;

public class EmojiEntry {
    private float x;
    private float y;
    private String emoji;

    public EmojiEntry(float x, float y, String emoji) {
        this.x = x;
        this.y = y;
        this.emoji = emoji;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public String getEmoji() { return emoji; }

    public void setEmoji(String emoji) { this.emoji = emoji; }
}
