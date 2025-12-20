package eu._4fh.wow2discord.discord;

import eu._4fh.wow2discord.db.DbGuildCharacters;
import eu._4fh.wow2discord.db.Transaction;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.Objects;

public class WowCommandHandler extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("Command must be used in a server context.").queue();
            return;
        }
        Discord discord = Discord.i();
        if (event.getCommandIdLong() == discord.linkCommandId) {
            handleLinkCommand(event);
        } else if (event.getCommandIdLong() == discord.searchLinkedCommandId) {
            handleSearchLinked(event);
        } else if (event.getCommandIdLong() == discord.searchUnlinkedCommandId) {
            handleSearchUnlinked(event);
        }
    }

    private long guildId(SlashCommandInteractionEvent event) {
        return Objects.requireNonNull(event.getGuild()).getIdLong();
    }

    private OptionMapping opt(SlashCommandInteractionEvent event, String optionName) {
        return Objects.requireNonNull(event.getOption(optionName),
                "Missing option " + optionName + " for event " + event.getFullCommandName());
    }

    private void handleLinkCommand(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        InteractionHook hook = event.getHook();

        long guildId = guildId(event);
        long wowCharId = opt(event, "wow-char-id").getAsLong();
        Member member = opt(event, "discord-user").getAsMember();
        if (member == null) {
            hook.editOriginal("Given user is not a member of this server").queue();
            return;
        }
        try (Transaction t = new Transaction()) {
            DbGuildCharacters.Wow2Dc wow2Dc = new DbGuildCharacters.Wow2Dc(guildId, wowCharId, member.getIdLong(),
                    member.getEffectiveName());
            if (!t.guildCharacters.update(wow2Dc)) {
                hook.editOriginal("Character ID not found").queue();
            } else {
                hook.editOriginal("Character added to Discord user").queue();
            }
            t.commit();
        }
    }

    private void handleSearchLinked(SlashCommandInteractionEvent event) {
    }

    private void handleSearchUnlinked(SlashCommandInteractionEvent event) {
    }
}
