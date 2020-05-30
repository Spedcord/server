package xyz.spedcord.server.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import xyz.spedcord.server.SpedcordServer;
import xyz.spedcord.server.job.Job;
import xyz.spedcord.server.user.User;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class WebhookUtil {

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final List<String> webhooks = new ArrayList<>();

    private WebhookUtil() {
        throw new UnsupportedOperationException();
    }

    public static void loadWebhooks() {
        File webhooksFile = new File("webhooks.txt");
        if (!webhooksFile.exists()) {
            try {
                webhooksFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            webhooks.addAll(Files.readAllLines(webhooksFile.toPath()).stream()
                    .filter(s -> !s.equals("")).collect(Collectors.toList()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void callWebhooks(long user, JsonObject data) {
        executorService.submit(() -> {
            JsonObject object = new JsonObject();
            object.addProperty("user", user);
            object.add("data", data);

            webhooks.forEach(s -> {
                try {
                    URL url = new URL(s);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("User-Agent", "SpedcordServer");
                    connection.setRequestProperty("Authorization", "Bearer " + SpedcordServer.KEY);

                    connection.setDoOutput(true);
                    connection.connect();

                    OutputStream outputStream = connection.getOutputStream();
                    outputStream.write(object.toString().getBytes(StandardCharsets.UTF_8));
                } catch (IOException ignored) {
                }
            });
        });
    }

    public static List<String> getWebhooks() {
        return webhooks;
    }

}