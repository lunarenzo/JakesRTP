package biz.donvi.jakesRTP;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.ArrayList;
import java.util.List;

import static biz.donvi.jakesRTP.JakesRtpPlugin.plugin;

public class CmdRtp implements TabExecutor {

    private final RandomTeleporter randomTeleporter;

    public CmdRtp(RandomTeleporter randomTeleporter) { this.randomTeleporter = randomTeleporter; }

    /**
     * This is called when a player runs the in-game "/rtp" command.
     * Anything (except errors) that directly deals with the player is done here.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can run this command.");
                return true;
            }
            if (args.length > 1) {
                sender.sendMessage(Messages.RTP_USAGE.format());
                return true;
            }
            Player player = (Player) sender;
            if (args.length == 1 && !sender.hasPermission("jakesrtp.usebyname") && !sender.hasPermission("jakesrtp.use." + args[0].toLowerCase())) {
                player.sendMessage(Messages.RTP_USAGE.format());
                return true;
            }
            RtpProfile relSettings = args.length == 0
                ? randomTeleporter.getRtpSettingsByWorldForPlayer(player)
                : randomTeleporter.getRtpSettingsByNameForPlayer(player, args[0]);
            if (player.hasPermission("jakesrtp.nocooldown") // If the player has permission to skip cooldown
                || player.hasPermission("jakesrtp.nocooldown." + relSettings.name.toLowerCase())
                || relSettings.coolDown.check(player.getName())) { // Or they are not on cooldown
                if (!randomTeleporter.playersInWarmup.containsKey(player.getUniqueId())) {
                    final boolean warmup =
                        relSettings.warmupEnabled && // Obvious...
                        !player.hasPermission("jakesrtp.nowarmup") && // Or if they have the perm to avoid it
                        !player.hasPermission("jakesrtp.nowarmup." + relSettings.name.toLowerCase());
                    if (!plugin.canUseEconomy() || relSettings.cost <= 0 ||
                        plugin.getEconomy().getBalance(player) >= relSettings.cost) {
                        // ==== By this point, all checks are done and the player WILL be teleported. ====
                        final Runnable execRtp = makeRunnable(player, relSettings, warmup);
                        if (warmup) { // If there is a warmup, schedule the runnable
                            final int taskID = sender
                                .getServer().getScheduler() // Get the task ID so that we can cancel it later.
                                .scheduleSyncRepeatingTask(plugin, execRtp, 2, 20);
                            if (taskID == -1) // This should only really happen during shutdown.
                                throw new JrtpBaseException("Could not schedule rtp-after-warmup.");
                            randomTeleporter.playersInWarmup.put(player.getUniqueId(),
                                                                 taskID); // Needed for canceling.
                        } else execRtp.run(); // No warmup, just run the teleport.
                    } else player.sendMessage(Messages.ECON_NOT_ENOUGH_MONEY.format(
                        relSettings.cost, plugin.getEconomy().getBalance(player)));
                } else player.sendMessage(Messages.WARMUP_RTP_ALREADY_CALLED.format());
            } else player.sendMessage(Messages.NEED_WAIT_COOLDOWN.format(
                relSettings.coolDown.timeLeftWords(player.getName())));
        } catch (JrtpBaseException.NotPermittedException npe) {
            sender.sendMessage(Messages.NP_GENERIC.format(npe.getMessage()));
        } catch (JrtpBaseException e) {
            sender.sendMessage(e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    private Runnable makeRunnable(final Player player, final RtpProfile rtpProfile, boolean calculatedWarmup) {
        return new Runnable() {
            private final BukkitScheduler scheduler = player.getServer().getScheduler();
            private final Location startLoc = player.getLocation().clone();
            private final boolean warmup = calculatedWarmup;
            private final long startTime = System.currentTimeMillis();
            private int done = 0;

            @Override
            public void run() {
                // The annoying error message, lets hope we never need use this...
                if (done > 1) taskError();
                    // If There should be no warmup, we teleport the user immediately.
                else if (!warmup) teleport();
                    // If we want the user to stand still AND they move, we cancel this runnable / future rtp.
                else if (rtpProfile.warmupCancelOnMove &&
                         (startLoc.getWorld() != player.getWorld() || startLoc.distanceSquared(player.getLocation()) > 1))
                    cancel();
                    // If we have waited enough time, we teleport the user.
                else if (timeDifInSeconds() >= rtpProfile.warmup) teleport();
                    // If we got to this point, the user still has to wait, and if wanted, we let them know how long.
                else if (rtpProfile.warmupCountDown) countDown();
                // If none of these were called, we just silently wait until the next time run() is called.
            }

            private int timeDifInSeconds() { return (int) ((System.currentTimeMillis() - startTime) / 1000); }

            private void countDown() {
                int timeLeft = rtpProfile.warmup - timeDifInSeconds();
                player.sendMessage(Messages.WARMUP_TELEPORTING_IN_X.format(timeLeft));
                String title = rtpProfile.warmupTitleCountdown;
                String subtitle = rtpProfile.warmupSubtitleCountdown == null ? "" : rtpProfile.warmupSubtitleCountdown.replace("%time%", String.valueOf(timeLeft));
                player.sendTitle(title, subtitle, 0, 20, 5);
                float pitch = 1.0f;
                if (rtpProfile.warmupSoundCountdownPitchIncrease) {
                    pitch = 0.8f + (float) (rtpProfile.warmup - timeLeft) * 0.15f;
                }
                playSound(player, rtpProfile.warmupSoundCountdown, 0.6f, pitch);
            }

            private void teleport() {
                try {
                    if (rtpProfile.cost > 0 && plugin.getEconomy().getBalance(player) < rtpProfile.cost) {
                        player.sendMessage(Messages.ECON_NO_LONGER_ENOUGH_MONEY.format());
                        return;
                    }
                    // Do the teleport action
                    var rtpAction = new RandomTeleportAction(
                        randomTeleporter,
                        rtpProfile,
                        player.getLocation(),
                        true,
                        true,
                        randomTeleporter.logRtpOnCommand, "Rtp-from-command triggered!"
                    );
                    rtpAction.onComplete(() -> {
                        if (!player.isOnline()) return;
                        final Location targetLoc = player.getLocation().clone();
                        new BukkitRunnable() {
                            private int ticksElapsed = 0;
                            @Override
                            public void run() {
                                ticksElapsed += 2;
                                if (!player.isOnline()) {
                                    cancel();
                                    return;
                                }
                                Location currentLoc = player.getLocation();
                                double dx = currentLoc.getX() - targetLoc.getX();
                                double dy = currentLoc.getY() - targetLoc.getY();
                                double dz = currentLoc.getZ() - targetLoc.getZ();
                                float dyaw = currentLoc.getYaw() - targetLoc.getYaw();
                                float dpitch = currentLoc.getPitch() - targetLoc.getPitch();
                                boolean moved = (dx * dx + dy * dy + dz * dz > 1e-6) || (dyaw * dyaw + dpitch * dpitch > 1e-6);
                                if (moved || ticksElapsed >= 40) {
                                    player.sendTitle(rtpProfile.warmupTitleSuccess, rtpProfile.warmupSubtitleSuccess, 5, 45, 15);
                                    for (String soundStr : rtpProfile.warmupSoundsSuccess) {
                                        playSound(player, soundStr, 1.0f, 1.0f);
                                    }
                                    cancel();
                                }
                            }
                        }.runTaskTimer(plugin, 0L, 2L);
                    });

                    if (rtpProfile.preferSyncTpOnCommand)
                         rtpAction.teleportSync(player);
                    else rtpAction.teleportFullyAsync(player); // Use fully async to avoid main thread chunk loading

                    // Log in the cooldown list
                    rtpProfile.coolDown.log(player.getName(), System.currentTimeMillis());
                    // Charge the player
                    if (rtpProfile.cost > 0) {
                        EconomyResponse er = plugin.getEconomy().withdrawPlayer(player, rtpProfile.cost);
                        if (er.transactionSuccess()) player.sendMessage(Messages.ECON_YOU_WERE_CHARGED_X.format(
                            plugin.getEconomy().format(er.amount),
                            plugin.getEconomy().format(er.balance)));
                        else player.sendMessage(Messages.ECON_ERROR.format(
                            "An economy error occurred: {0}", er.errorMessage));
                    }
                } catch (Exception e) {
                    player.sendMessage(Messages.NP_UNEXPECTED_EXCEPTION.format(e.getMessage()));
                    e.printStackTrace();
                } finally {
                    cancelTask();
                }
            }

            private void cancel() {
                player.sendMessage(Messages.WARMUP_CANCEL_BECAUSE_MOVE.format());
                player.sendTitle(rtpProfile.warmupTitleCancel, rtpProfile.warmupSubtitleCancel, 5, 40, 15);
                for (String soundStr : rtpProfile.warmupSoundsCancel) {
                    playSound(player, soundStr, 1.0f, 1.0f);
                }
                cancelTask();
            }

            private void cancelTask() {
                done++;
                Integer taskID = randomTeleporter.playersInWarmup.remove(player.getUniqueId());
                if (taskID != null)
                    scheduler.cancelTask(taskID); // Only cancel if task existed.
            }

            private void taskError() {
                if (done > 1000) scheduler.cancelTasks(plugin); // Emergency cleanup.
                if (done < 10 || done % 100 == 0) throw new RuntimeException("RTP task run twice?? Please report.");
                done++; // This is meant to be annoying, but not *too* annoying.
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ArrayList<String> validSearches = new ArrayList<>();
        if (args.length == 0 ||
            !(sender instanceof Player) ||
            !sender.hasPermission("jakesrtp.usebyname")
        ) return validSearches;
        for (String name : randomTeleporter.getRtpSettingsNamesForPlayer((Player) sender))
            if (name.contains(args[0]))
                validSearches.add(name);
        return validSearches;
    }

    private void playSound(Player player, String soundStr, float defaultVolume, float defaultPitch) {
        if (soundStr == null || soundStr.isEmpty()) return;
        String[] parts = soundStr.split(":");
        String name = parts[0].trim();
        float volume = defaultVolume;
        float pitch = defaultPitch;
        if (parts.length > 1) {
            try {
                volume = Float.parseFloat(parts[1].trim());
            } catch (NumberFormatException ignored) {}
        }
        if (parts.length > 2) {
            try {
                pitch = Float.parseFloat(parts[2].trim());
            } catch (NumberFormatException ignored) {}
        }
        try {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(name.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            try {
                player.playSound(player.getLocation(), name.toLowerCase(), volume, pitch);
            } catch (Exception ignored) {}
        }
    }
}
