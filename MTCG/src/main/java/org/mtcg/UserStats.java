package org.mtcg;

public class UserStats {
    private String username;
    private int elo;
    private int wins;
    private int losses;
    private int rank; // Platzierung

    public UserStats(String username, int elo, int wins, int losses) {
        this.username = username;
        this.elo = elo;
        this.wins = wins;
        this.losses = losses;
    }

    public String getUsername() {
        return username;
    }

    public int getElo() {
        return elo;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }
}
