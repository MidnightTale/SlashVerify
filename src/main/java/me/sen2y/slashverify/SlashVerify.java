package me.sen2y.slashverify;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.commands.PluginSlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommandProvider;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class SlashVerify extends JavaPlugin implements SlashCommandProvider {

    String alias; // Package-private for testing
    String verificationSuccessMessage; // Package-private for testing
    int codeMinimumLength; // Package-private for testing
    String codeRegexPattern; // Package-private for testing
    String invalidCodeMessage; // Package-private for testing

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        alias = this.getConfig().getString("command-alias");
        verificationSuccessMessage = this.getConfig().getString("verification-success-message", "Verification successful!"); // Added a default if not found
        codeMinimumLength = this.getConfig().getInt("code-minimum-length", 0); // Default 0 means disabled
        codeRegexPattern = this.getConfig().getString("code-regex-pattern", ""); // Default empty means disabled
        invalidCodeMessage = this.getConfig().getString("invalid-code-message", "Invalid code. Reason: {reason}");
    }

    @Override
    public Set<PluginSlashCommand> getSlashCommands() {
        return new HashSet<>(Collections.singletonList(
                new PluginSlashCommand(this, new CommandData(alias, Objects.requireNonNull(this.getConfig().getString("command-description")))
                        .addOption(OptionType.STRING, Objects.requireNonNull(this.getConfig().getString("code-option-name")), Objects.requireNonNull(this.getConfig().getString("code-option-description")), true))
        ));
    }

    @SlashCommand(path = "*")
    public void verifyCommand(SlashCommandEvent event) {
        if (!event.getCommandPath().equalsIgnoreCase(alias)) {
            return;
        }
        String code = Objects.requireNonNull(event.getOption("code")).getAsString();
        String discordUserId = event.getUser().getId();

        // Code Validation Logic
        if (codeMinimumLength > 0 && code.length() < codeMinimumLength) {
            String reason = "Code too short (minimum " + codeMinimumLength + " characters).";
            event.reply(invalidCodeMessage.replace("{reason}", reason))
                 .setEphemeral(this.getConfig().getBoolean("ephemeral-reply", true)).queue();
            return;
        }

        if (codeRegexPattern != null && !codeRegexPattern.isEmpty()) {
            if (!code.matches(codeRegexPattern)) {
                String reason = "Code does not match the required format.";
                event.reply(invalidCodeMessage.replace("{reason}", reason))
                     .setEphemeral(this.getConfig().getBoolean("ephemeral-reply", true)).queue();
                return;
            }
        }

        // Original verification logic (from previous step)
        String dSrvMessage = DiscordSRV.getPlugin().getAccountLinkManager().process(code, discordUserId);
        // ... (rest of the logic for processing dSrvMessage and sending custom success message)
        // Ensure the boolean for ephemeral-reply has a default if not found in config for the validation messages as well.
        // The previous step's code for success/failure reply:
        boolean successfulVerification = dSrvMessage != null && !dSrvMessage.toLowerCase().contains("error") && !dSrvMessage.toLowerCase().contains("already linked") && !dSrvMessage.toLowerCase().contains("invalid");

        String replyMessage;
        if (successfulVerification) {
            if (verificationSuccessMessage != null && !verificationSuccessMessage.isEmpty()) {
                replyMessage = verificationSuccessMessage.replace("{user}", event.getUser().getAsTag()).replace("{code}", code);
            } else {
                replyMessage = dSrvMessage;
            }
        } else {
            replyMessage = dSrvMessage; // This will contain whatever error DiscordSRV itself provides
        }

        event.reply(replyMessage)
                .setEphemeral(this.getConfig().getBoolean("ephemeral-reply", true)).queue();
    }
}
