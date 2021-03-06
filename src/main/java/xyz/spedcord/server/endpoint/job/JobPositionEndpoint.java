package xyz.spedcord.server.endpoint.job;

import io.javalin.http.Context;
import xyz.spedcord.server.endpoint.Endpoint;
import xyz.spedcord.server.job.Job;
import xyz.spedcord.server.job.JobController;
import xyz.spedcord.server.job.Location;
import xyz.spedcord.server.response.Responses;
import xyz.spedcord.server.user.User;
import xyz.spedcord.server.user.UserController;

import java.util.Optional;

/**
 * Handles job positions
 *
 * @author Maximilian Dorn
 * @version 2.0.0
 * @since 1.0.0
 */
public class JobPositionEndpoint extends Endpoint {

    private final UserController userController;
    private final JobController jobController;

    public JobPositionEndpoint(UserController userController, JobController jobController) {
        this.userController = userController;
        this.jobController = jobController;
    }

    @Override
    public void handle(Context ctx) {
        // Get x and z coords
        Optional<String> xzOptional = this.getQueryParam("xz", ctx);
        if (xzOptional.isEmpty()) {
            Responses.error("Invalid xz param").respondTo(ctx);
            return;
        }
        String xz = xzOptional.get();

        // Parse coords
        long x;
        long z;
        try {
            String[] split = xz.split(";");
            x = Long.parseLong(split[0]);
            z = Long.parseLong(split[1]);
        } catch (Exception ignored) {
            Responses.error("Invalid xz param").respondTo(ctx);
            return;
        }

        // Get user
        Optional<User> optional = this.getUserFromQuery("discordId", true, ctx, this.userController);
        if (optional.isEmpty()) {
            Responses.error("Unknown user / Invalid request").respondTo(ctx);
            return;
        }
        User user = optional.get();

        // Abort if user has no pending job
        if (this.jobController.getPendingJob(user.getDiscordId()) == null) {
            Responses.error("You don't have a pending job").respondTo(ctx);
            return;
        }

        // Add position
        Job pendingJob = this.jobController.getPendingJob(user.getDiscordId());
        pendingJob.getPositions().add(new Location(x, 0, z));

        Responses.success("Position saved").respondTo(ctx);
    }
}
