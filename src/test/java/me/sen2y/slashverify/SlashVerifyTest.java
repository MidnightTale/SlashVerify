package me.sen2y.slashverify;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.AccountLinkManager;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionMapping;
import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.interactions.ReplyAction;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlashVerifyTest {

    @Mock
    JavaPlugin mockPlugin; // Representing the SlashVerify plugin instance itself for some Bukkit methods

    @Mock
    FileConfiguration mockConfig;

    @Mock
    SlashCommandEvent mockEvent;

    @Mock
    OptionMapping mockCodeOption;

    @Mock
    User mockUser;

    @Mock
    ReplyAction mockReplyAction;

    @Mock
    DiscordSRV mockDiscordSRV;

    @Mock
    AccountLinkManager mockAccountLinkManager;

    @Mock
    Logger mockLogger;


    // Use @InjectMocks for the class under test if it were a simple POJO.
    // However, SlashVerify extends JavaPlugin and has complex initialization.
    // We will manually instantiate and configure it.
    SlashVerify slashVerify;

    @BeforeEach
    void setUp() {
        // Manually instantiate SlashVerify - it's a JavaPlugin, so direct injection is tricky.
        // We'll mock its JavaPlugin inherited methods like getConfig() and getLogger()
        slashVerify = new SlashVerify();

        // Mocking Bukkit's JavaPlugin parts
        // It seems like the class under test itself is the JavaPlugin, so we need to use a spy or careful mocking.
        // For simplicity, let's assume we're testing the command logic, not the plugin lifecycle itself extensively.
        // We'll use a real instance and then mock its interactions with Bukkit/DiscordSRV.

        // We need to initialize the fields that onEnable would normally set.
        // This requires mocking getConfig() on the slashVerify instance itself.
        // This is hard because it's not a mock. So, we use a real FileConfiguration mock (mockConfig)
        // and then reflectively set it or make SlashVerify more testable.

        // For now, let's directly set the config-derived fields in SlashVerify for tests.
        // This is a common pattern when testing classes tightly coupled to frameworks.
        // This means we need to make them package-private or use setters if we want to avoid reflection.
        // Let's assume for now they are package-private for testability.

        // Setup default config values directly for fields (as if onEnable was called)
        slashVerify.alias = "verify"; // Default or common value
        slashVerify.verificationSuccessMessage = "Congrats {user}, verified with {code}!";
        slashVerify.codeMinimumLength = 6;
        slashVerify.codeRegexPattern = "^[a-zA-Z0-9]+$";
        slashVerify.invalidCodeMessage = "Invalid code. Reason: {reason}";

        // Mocking getConfig() on the plugin instance. This is tricky.
        // A better way would be to have a constructor or setter for the config.
        // For now, we'll rely on setting fields directly and mocking behavior of getConfig for ephemeral reply.
        // This part is problematic: `slashVerify.getConfig()` cannot be directly mocked without a spy
        // or refactoring SlashVerify. Let's assume `ephemeral-reply` is true for tests for now.

        when(mockEvent.getOption("code")).thenReturn(mockCodeOption);
        when(mockEvent.getUser()).thenReturn(mockUser);
        when(mockUser.getId()).thenReturn("testUserId");
        when(mockUser.getAsTag()).thenReturn("TestUser#1234");
        when(mockEvent.getCommandPath()).thenReturn("verify"); // Matches alias

        // Mock the reply chain
        when(mockEvent.reply(anyString())).thenReturn(mockReplyAction);
        when(mockReplyAction.setEphemeral(anyBoolean())).thenReturn(mockReplyAction);

        // Mock getConfig for ephemeral reply (this is where it gets tricky without a spy)
        // We'll assume the plugin instance passed to PluginSlashCommand is `slashVerify` itself.
        // And that `this.getConfig()` inside `SlashVerify` needs to be handled.
        // For the purpose of this subtask, let's assume `this.getConfig().getBoolean("ephemeral-reply")`
        // is part of the `SlashVerify` instance state or refactored for testability.
        // The subtask that modified `SlashVerify` used `this.getConfig().getBoolean("ephemeral-reply", true)`
        // So, we don't need to mock getConfig for this specific call if we assume it will default.

    }

    @Test
    void verifyCommand_validCode_usesCustomSuccessMessage() {
        try (MockedStatic<DiscordSRV> mockedDiscordSRVStatic = Mockito.mockStatic(DiscordSRV.class)) {
            mockedDiscordSRVStatic.when(DiscordSRV::getPlugin).thenReturn(mockDiscordSRV);
            when(mockDiscordSRV.getAccountLinkManager()).thenReturn(mockAccountLinkManager);
            when(mockCodeOption.getAsString()).thenReturn("VALID123");
            // Simulate successful verification by DiscordSRV's manager
            when(mockAccountLinkManager.process("VALID123", "testUserId")).thenReturn("DiscordSRV success message"); // Actual message doesn't matter if custom is used

            slashVerify.verifyCommand(mockEvent);

            verify(mockEvent).reply("Congrats TestUser#1234, verified with VALID123!");
            verify(mockReplyAction).setEphemeral(true); // Assuming default true for ephemeral
            verify(mockReplyAction).queue();
        }
    }

    @Test
    void verifyCommand_validCode_noCustomMessage_usesDiscordSRVMessage() {
        slashVerify.verificationSuccessMessage = ""; // Disable custom message

        try (MockedStatic<DiscordSRV> mockedDiscordSRVStatic = Mockito.mockStatic(DiscordSRV.class)) {
            mockedDiscordSRVStatic.when(DiscordSRV::getPlugin).thenReturn(mockDiscordSRV);
            when(mockDiscordSRV.getAccountLinkManager()).thenReturn(mockAccountLinkManager);
            when(mockCodeOption.getAsString()).thenReturn("VALID456");
            when(mockAccountLinkManager.process("VALID456", "testUserId")).thenReturn("Actual DiscordSRV success!");

            slashVerify.verifyCommand(mockEvent);

            verify(mockEvent).reply("Actual DiscordSRV success!");
            verify(mockReplyAction).setEphemeral(true);
            verify(mockReplyAction).queue();
        }
    }

    @Test
    void verifyCommand_discordSRVReturnsError_usesDiscordSRVMessage() {
        // Even if custom success message is set, if DiscordSRV indicates failure, its message should be used.
        // The heuristic `!dSrvMessage.toLowerCase().contains("error")` etc. is tested here.
        try (MockedStatic<DiscordSRV> mockedDiscordSRVStatic = Mockito.mockStatic(DiscordSRV.class)) {
            mockedDiscordSRVStatic.when(DiscordSRV::getPlugin).thenReturn(mockDiscordSRV);
            when(mockDiscordSRV.getAccountLinkManager()).thenReturn(mockAccountLinkManager);
            when(mockCodeOption.getAsString()).thenReturn("ANYCODE");
            when(mockAccountLinkManager.process("ANYCODE", "testUserId")).thenReturn("DiscordSRV error: Already linked");

            slashVerify.verifyCommand(mockEvent);

            verify(mockEvent).reply("DiscordSRV error: Already linked");
            verify(mockReplyAction).setEphemeral(true);
            verify(mockReplyAction).queue();
        }
    }

    @Test
    void verifyCommand_codeTooShort() {
        when(mockCodeOption.getAsString()).thenReturn("short"); // 5 chars, min is 6

        slashVerify.verifyCommand(mockEvent);

        verify(mockEvent).reply("Invalid code. Reason: Code too short (minimum 6 characters).");
        verify(mockReplyAction).setEphemeral(true);
        verify(mockReplyAction).queue();
        verifyNoInteractions(mockAccountLinkManager); // Ensure DiscordSRV process not called
    }

    @Test
    void verifyCommand_codeInvalidFormat() {
        when(mockCodeOption.getAsString()).thenReturn("INVALID!"); // Contains '!' not in regex ^[a-zA-Z0-9]+$

        slashVerify.verifyCommand(mockEvent);

        verify(mockEvent).reply("Invalid code. Reason: Code does not match the required format.");
        verify(mockReplyAction).setEphemeral(true);
        verify(mockReplyAction).queue();
        verifyNoInteractions(mockAccountLinkManager);
    }

    @Test
    void verifyCommand_codeTooShortAndInvalidFormat_reportsTooShort() {
        // Assuming length check comes before regex check in implementation
        slashVerify.codeMinimumLength = 7; // Set higher for this test
        when(mockCodeOption.getAsString()).thenReturn("short!"); // 6 chars, min 7, also invalid format

        slashVerify.verifyCommand(mockEvent);

        // The implementation checks length first.
        verify(mockEvent).reply("Invalid code. Reason: Code too short (minimum 7 characters).");
        verify(mockReplyAction).setEphemeral(true);
        verify(mockReplyAction).queue();
        verifyNoInteractions(mockAccountLinkManager);
    }


    @Test
    void verifyCommand_validationDisabled_validCode() {
        slashVerify.codeMinimumLength = 0; // Disable length check
        slashVerify.codeRegexPattern = "";   // Disable regex check

        try (MockedStatic<DiscordSRV> mockedDiscordSRVStatic = Mockito.mockStatic(DiscordSRV.class)) {
            mockedDiscordSRVStatic.when(DiscordSRV::getPlugin).thenReturn(mockDiscordSRV);
            when(mockDiscordSRV.getAccountLinkManager()).thenReturn(mockAccountLinkManager);
            when(mockCodeOption.getAsString()).thenReturn("V@L!D_C0D3-now"); // Should pass if validation disabled
            when(mockAccountLinkManager.process("V@L!D_C0D3-now", "testUserId")).thenReturn("DiscordSRV success");

            slashVerify.verifyCommand(mockEvent);

            verify(mockEvent).reply("Congrats TestUser#1234, verified with V@L!D_C0D3-now!");
            verify(mockReplyAction).queue();
        }
    }

    @Test
    void verifyCommand_commandAliasMismatch_doesNothing() {
        when(mockEvent.getCommandPath()).thenReturn("wrongcommand");

        slashVerify.verifyCommand(mockEvent);

        verify(mockEvent, never()).getOption(anyString());
        verify(mockEvent, never()).reply(anyString());
        verifyNoInteractions(mockAccountLinkManager);
    }
}
