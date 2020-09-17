package xyz.spedcord.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import dev.lukaesebrot.jal.endpoints.HttpServer;
import dev.lukaesebrot.jal.ratelimiting.RateLimiter;
import io.javalin.Javalin;
import io.javalin.http.HandlerType;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.PropertyCodecProvider;
import org.bson.codecs.pojo.PropertyCodecRegistry;
import org.bson.codecs.pojo.TypeWithTypeParameters;
import org.eclipse.jetty.http.HttpStatus;
import xyz.spedcord.common.config.Config;
import xyz.spedcord.common.mongodb.MongoDBService;
import xyz.spedcord.common.sql.MySqlService;
import xyz.spedcord.server.company.CompanyController;
import xyz.spedcord.server.endpoint.company.*;
import xyz.spedcord.server.endpoint.job.*;
import xyz.spedcord.server.endpoint.oauth.*;
import xyz.spedcord.server.endpoint.user.*;
import xyz.spedcord.server.job.JobController;
import xyz.spedcord.server.joinlink.JoinLinkController;
import xyz.spedcord.server.oauth.invite.InviteAuthController;
import xyz.spedcord.server.oauth.register.RegisterAuthController;
import xyz.spedcord.server.response.Responses;
import xyz.spedcord.server.user.Flag;
import xyz.spedcord.server.user.User;
import xyz.spedcord.server.user.UserController;
import xyz.spedcord.server.util.WebhookUtil;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class SpedcordServer {

    public static final boolean DEV = System.getenv("SPEDCORD_DEV") != null
            && System.getenv("SPEDCORD_DEV").equals("true");
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
            .create();
    public static final long[] MODERATORS = {347018538713874444L, 332142165402714113L};
    public static String KEY = null;

    private InviteAuthController inviteAuthController;
    private RegisterAuthController registerAuthController;
    private JoinLinkController joinLinkController;
    private UserController userController;
    private JobController jobController;
    private CompanyController companyController;
    private Config config;

    public void start() throws IOException {
        config = new Config(new File("config.cfg"), new String[]{
                "host", "localhost",
                "port", "5670",
                "requests-per-minute", "120",
                "dev-mode", "false",
                "key", "ENTER_A_SECRET_KEY",
                "oauth-clientid", "ENTER_THE_CLIENTID",
                "oauth-clientsecret", "ENTER_THE_CLIENTSECRET"
        });
        Config mongoConfig = new Config(new File("mongo.cfg"), new String[]{
                "host", "localhost",
                "port", "27017",
                "db", "spedcord"
        });

        KEY = config.get("key");

        WebhookUtil.loadWebhooks();

        MongoDBService mongoDBService = new MongoDBService(
                mongoConfig.get("host"),
                Integer.parseInt(mongoConfig.get("port")),
                mongoConfig.get("db"),
                createSuperUselessCodedProviderForFlagArray()
        );

        inviteAuthController = new InviteAuthController(
                config.get("oauth-clientid"),
                config.get("oauth-clientsecret")
        );
        registerAuthController = new RegisterAuthController(
                config.get("oauth-clientid"),
                config.get("oauth-clientsecret")
        );
        joinLinkController = new JoinLinkController(mongoDBService);
        userController = new UserController(mongoDBService);
        jobController = new JobController(mongoDBService);
        companyController = new CompanyController(mongoDBService);

        Javalin app = Javalin.create().start(config.get("host"), Integer.parseInt(config.get("port")));
        RateLimiter rateLimiter = new RateLimiter(Integer.parseInt(config.get("requests-per-minute")), ctx ->
                Responses.error(HttpStatus.TOO_MANY_REQUESTS_429, "Too many requests").respondTo(ctx));
        HttpServer server = new HttpServer(app, rateLimiter);

        registerEndpoints(server);
        startPayoutTimer();
    }

    private void startPayoutTimer() {
        String lastPayoutStr = config.get("lastPayout");
        if (lastPayoutStr == null) {
            lastPayoutStr = "0";
        }
        AtomicLong lastPayout = new AtomicLong(Long.parseLong(lastPayoutStr));

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            if (System.currentTimeMillis() - lastPayout.get() >= TimeUnit.DAYS.toMillis(7)) {
                lastPayout.set(System.currentTimeMillis());
                config.set("lastPayout", lastPayout.get() + "");
                try {
                    config.save();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                companyController.getCompanies().forEach(company -> {
                    AtomicInteger payouts = new AtomicInteger(0);
                    company.getMemberDiscordIds().forEach(memberId -> {
                        userController.getUser(memberId).ifPresent(user -> {
                            user.setBalance(user.getBalance() + 2500);
                            userController.updateUser(user);
                            payouts.incrementAndGet();
                        });
                    });

                    userController.getUser(company.getOwnerDiscordId()).ifPresent(user -> {
                        user.setBalance(user.getBalance() + 2500);
                        userController.updateUser(user);
                        payouts.incrementAndGet();
                    });

                    company.setBalance(company.getBalance() - (payouts.get() * 2500));
                    companyController.updateCompany(company);
                });
                System.out.println("Payouts were paid");
            }
        }, 5, 5, TimeUnit.MINUTES);

        Runtime.getRuntime().addShutdownHook(new Thread(executorService::shutdown));
    }

    private void registerEndpoints(HttpServer server) {
        server.endpoint("/invite/discord", HandlerType.GET, new DiscordEndpoint(inviteAuthController, joinLinkController, userController, companyController));
        server.endpoint("/invite/:id", HandlerType.GET, new InviteEndpoint(inviteAuthController, joinLinkController));

        server.endpoint("/user/register", HandlerType.GET, new RegisterEndpoint(registerAuthController));
        server.endpoint("/user/register/discord", HandlerType.GET, new RegisterDiscordEndpoint(registerAuthController, userController));
        server.endpoint("/user/info/:discordId", HandlerType.GET, new UserInfoEndpoint(config, userController));
        server.endpoint("/user/get/:discordId", HandlerType.GET, new UserGetEndpoint(userController, config));
        server.endpoint("/user/jobs/:discordId", HandlerType.GET, new UserJobsEndpoint(userController, jobController));
        server.endpoint("/user/changekey", HandlerType.POST, new UserChangekeyEndpoint(userController));
        server.endpoint("/user/checkauth", HandlerType.POST, new UserCheckAuthEndpoint(userController));
        server.endpoint("/user/cheater", HandlerType.POST, new UserCheaterEndpoint(userController));
        server.endpoint("/user/leavecompany", HandlerType.POST, new UserLeaveCompanyEndpoint(userController, companyController));
        server.endpoint("/user/listmods", HandlerType.GET, new UserListModsEndpoint());

        server.endpoint("/company/info", HandlerType.GET, new CompanyInfoEndpoint(companyController, userController, jobController));
        server.endpoint("/company/register", HandlerType.POST, new CompanyRegisterEndpoint(companyController, userController));
        server.endpoint("/company/kickmember", HandlerType.POST, new CompanyKickMemberEndpoint(companyController, userController));
        server.endpoint("/company/createjoinlink/:companyId", HandlerType.POST, new CreateJoinLinkEndpoint(joinLinkController,
                config.get("host"), Integer.parseInt(config.get("port"))));
        //server.endpoint("/company/shop", HandlerType.POST, new ShopBuyItemEndpoint(companyController, joinLinkController));
        server.endpoint("/company/list/:sortMode", HandlerType.GET, new CompanyListEndpoint(companyController, userController));

        server.endpoint("/job/start", HandlerType.POST, new JobStartEndpoint(jobController, userController));
        server.endpoint("/job/end", HandlerType.POST, new JobEndEndpoint(jobController, userController, companyController));
        server.endpoint("/job/cancel", HandlerType.POST, new JobCancelEndpoint(jobController, userController));
        server.endpoint("/job/listunverified", HandlerType.GET, new JobListUnverifiedEndpoint(jobController, userController));
        server.endpoint("/job/verify", HandlerType.POST, new JobVerifyEndpoint(jobController, userController));
    }

    /*
     * WHY DO I HAVE TO DO THIS? THIS TOOK ME HOURS TO DEBUG. PLEASE KILL ME
     */
    private PropertyCodecProvider createSuperUselessCodedProviderForFlagArray() {
        return new PropertyCodecProvider() {
            @Override
            public <T> Codec<T> get(TypeWithTypeParameters<T> type, PropertyCodecRegistry registry) {
                if(type.getType() != Flag[].class) {
                    return null;
                }
                return (Codec<T>) new Codec<Flag[]>() {
                    @Override
                    public Flag[] decode(BsonReader reader, DecoderContext decoderContext) {
                        String str = reader.readString();
                        String[] arr = str.split(";");
                        return Arrays.stream(arr).map(s -> Flag.valueOf(s)).toArray(Flag[]::new);
                    }

                    @Override
                    public void encode(BsonWriter writer, Flag[] value, EncoderContext encoderContext) {
                        writer.writeString(Arrays.stream(value).map(flag -> flag.name()).collect(Collectors.joining(";")));
                    }

                    @Override
                    public Class<Flag[]> getEncoderClass() {
                        return Flag[].class;
                    }
                };
            }
        };
    }

}
