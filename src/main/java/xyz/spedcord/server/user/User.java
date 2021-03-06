package xyz.spedcord.server.user;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.util.Arrays;
import java.util.List;

/**
 * @author Maximilian Dorn
 * @version 2.1.1
 * @since 1.0.0
 */
@BsonDiscriminator
public class User {

    public static final User EMPTY = new User(-1, -1, null, null,
            null, -1, -1, -1, null, null, AccountType.USER);
    private final long discordId;
    private final List<Integer> jobList;
    private AccountType accountType;
    private int id;
    private String key;
    private String accessToken;
    private String refreshToken;
    private long tokenExpires;
    private int companyId;
    private double balance;
    private List<Flag> flags;

    @BsonCreator
    public User(@BsonId int id, @BsonProperty("discordId") long discordId, @BsonProperty("key") String key, @BsonProperty("accessToken") String accessToken, @BsonProperty("refreshToken") String refreshToken, @BsonProperty("tokenExpires") long tokenExpires, @BsonProperty("companyId") int companyId, @BsonProperty("balance") double balance, @BsonProperty("jobList") List<Integer> jobList, @BsonProperty("flags") List<Flag> flags, @BsonProperty("accountType") AccountType accountType) {
        this.id = id;
        this.discordId = discordId;
        this.key = key;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenExpires = tokenExpires;
        this.companyId = companyId;
        this.balance = balance;
        this.jobList = jobList;
        this.flags = flags;
        this.accountType = accountType;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getDiscordId() {
        return this.discordId;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getCompanyId() {
        return this.companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public double getBalance() {
        return this.balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public List<Integer> getJobList() {
        return this.jobList;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public long getTokenExpires() {
        return this.tokenExpires;
    }

    public void setTokenExpires(long tokenExpires) {
        this.tokenExpires = tokenExpires;
    }

    public List<Flag> getFlags() {
        return this.flags;
    }

    public void setFlags(List<Flag> flags) {
        this.flags = flags;
    }

    public AccountType getAccountType() {
        return this.accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public enum AccountType {
        USER(0),
        MOD(1),
        ADMIN(2);

        private final int val;

        AccountType(int val) {
            this.val = val;
        }

        public static AccountType fromVal(int val) {
            return Arrays.stream(values()).filter(t -> t.val == val).findAny().orElse(null);
        }

        public int getVal() {
            return this.val;
        }
    }
}
