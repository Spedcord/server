package xyz.spedcord.server.user;

import java.util.List;

public class User {

    private final int id;
    private final long discordId;
    private String key;
    private String accessToken;
    private String refreshToken;
    private int companyId;
    private final List<Integer> jobList;

    public User(int id, long discordId, String key, String accessToken, String refreshToken, int companyId, List<Integer> jobList) {
        this.id = id;
        this.discordId = discordId;
        this.key = key;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.companyId = companyId;
        this.jobList = jobList;
    }

    public int getId() {
        return id;
    }

    public long getDiscordId() {
        return discordId;
    }

    public String getKey() {
        return key;
    }

    public int getCompanyId() {
        return companyId;
    }

    public List<Integer> getJobList() {
        return jobList;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

}
