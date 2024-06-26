package dev.plastec.webhookreport.commands;

import dev.plastec.webhookreport.utils.DiscordWebhook;
import dev.plastec.webhookreport.utils.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class SendCommand extends SubCommand {
    Map<String, Long> durationBetweenSend;
    public static int sessionReportCount = 0;
    public static int publicReportsPer30Min = 0;
    public static int privateReportsPer30Min = 0;

    public SendCommand(Plugin plugin, FileConfiguration config, String name, String permission) {
        super(plugin, config, name, permission);
        this.durationBetweenSend = new HashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(ChatColor.RED + "you do not have permission to do that.");
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only a player is allowed to use this command");
            return false;
        }

        boolean useCooldown = !sender.hasPermission("webhookreport.bypasscooldown");

        if (useCooldown) {
            int sendReportCooldown = config.getInt("cooldown");
            long timeElapsed;
            if (durationBetweenSend.containsKey(sender.getName())) {
                timeElapsed = new Date().getTime() - durationBetweenSend.get(sender.getName());
            } else {
                timeElapsed = sendReportCooldown * 1000L;
            }

            if (timeElapsed < (sendReportCooldown * 1000L)) {
                sender.sendMessage(ChatColor.RED + "Report is on cooldown. You will be able to use it within " + (sendReportCooldown - (timeElapsed / 1000)) + " seconds");
                return false;
            }
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Invalid arguments");
            sender.sendMessage(ChatColor.WHITE + "usage: /report send [public/private] <message>");
            return false;
        }

        boolean isPublic = args[0].equalsIgnoreCase("public");
        boolean isPrivate = args[0].equalsIgnoreCase("private");
        if (!(isPublic || isPrivate)) {
            sender.sendMessage(ChatColor.RED + "Did not specify public or private report");
            return false;
        }

        String webhookURL = null;
        if (isPublic)
            webhookURL = config.getString("webhook.public");

        if (isPrivate)
            webhookURL = config.getString("webhook.private");


        if (webhookURL == null || webhookURL.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "WebhookReport is not set up properly. Contact server administration");
            return false;
        }

        if ((isPublic && publicReportsPer30Min >= 30) || (isPrivate && privateReportsPer30Min >= 30)) {
            StringBuilder builder = new StringBuilder();

            if (isPublic) builder.append("Public");
            else builder.append("Private");

            builder.append("'s webhook has reached its limit please wait");

            sender.sendMessage(builder.toString());
            return false;
        }


        try {
            DiscordWebhook webhook = new DiscordWebhook(webhookURL);
            StringBuilder stringBuilder = new StringBuilder();

            if (args[0].equalsIgnoreCase("private")) {
                stringBuilder.append("Anonymous");
            } else {
                stringBuilder.append(sender.getName());
            }

            stringBuilder.append(" report:\\n```");
            for (int i = 1; i < args.length; i++) {
                stringBuilder.append(args[i].replace("`", ""));
                if (i != args.length - 1) {
                    stringBuilder.append(" ");
                }
            }
            stringBuilder.append("```");

            webhook.setContent(stringBuilder.toString());
            sender.sendMessage(ChatColor.YELLOW + "Submitting report...");
            webhook.execute();
            sender.sendMessage(ChatColor.GREEN + "Successfully submitted report");
            durationBetweenSend.put(sender.getName(), new Date().getTime());
            sessionReportCount++;
            return true;
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Could not submit report. Please contact server administration");
            plugin.getLogger().log(Level.WARNING, e.toString());
            return false;
        }
    }
}
