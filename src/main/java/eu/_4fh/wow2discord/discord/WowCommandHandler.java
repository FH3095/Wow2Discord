package eu._4fh.wow2discord.discord;

import eu._4fh.wow2discord.db.DbGuildCharacters;
import eu._4fh.wow2discord.db.Transaction;
import eu._4fh.wow2discord.util.Config;
import eu._4fh.wow2discord.util.SimplePattern;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.*;
import java.util.stream.Stream;

/**
 * Handles the discord commands
 */
public class WowCommandHandler extends ListenerAdapter {

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (event.getGuild() == null) {
            // Command not used in a server context
            return;
        }
        if (!event.getFullCommandName().equals("wow link") || !event.getFocusedOption()
                .getName()
                .equals("wow-char-id")) {
            return;
        }
        long guildId = guildId(event);
        String currentValue = event.getFocusedOption().getValue();

        List<Command.Choice> choices = new ArrayList<>(25);
        try (Transaction t = new Transaction()) {
            Stream<DbGuildCharacters.Wow2DcMapping> mappings = t.guildCharacters.getAllUnlinkedMappingsWithNameMatchOrderedByRankLimit25(
                    guildId, currentValue + "%").stream();
            mappings.forEach(mapping -> choices.add(
                    new Command.Choice(mapping.wowCharName() + "-" + mapping.wowCharServer(), mapping.wowCharId())));
        }
        event.replyChoices(choices).queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("Command must be used in a server context.").setEphemeral(true).queue();
            return;
        }

        switch (event.getFullCommandName()) {
            case "wow link" -> handleLinkCommand(event);
            case "wow search linked" -> handleSearch(event, true);
            case "wow search unlinked" -> handleSearch(event, false);
        }
    }

    private long guildId(Interaction event) {
        return Objects.requireNonNull(event.getGuild()).getIdLong();
    }

    private OptionMapping opt(SlashCommandInteractionEvent event, String optionName) {
        return Objects.requireNonNull(event.getOption(optionName),
                "Missing option " + optionName + " for event " + event.getFullCommandName());
    }

    private Optional<OptionMapping> optOpt(SlashCommandInteractionEvent event, String optionName) {
        return Optional.ofNullable(event.getOption(optionName));
    }

    /**
     * Handles the command to link a wow character to a discord account
     */
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

    private void handleSearch(SlashCommandInteractionEvent event, boolean alreadyLinkedChars) {
        event.deferReply(true).queue();
        InteractionHook hook = event.getHook();
        hook.setEphemeral(true);

        long guildId = guildId(event);
        Long userId = optOpt(event, "discord-user").map(opt -> opt.getAsUser().getIdLong()).orElse(null);
        Byte charRank = optOpt(event, "guild-rank").map(opt -> (byte) opt.getAsLong()).orElse(null);
        SimplePattern charNamePattern = optOpt(event, "char-name").map(opt -> new SimplePattern(opt.getAsString()))
                .orElse(null);
        Stream<DbGuildCharacters.Wow2DcMapping> filteredMappings;
        try (Transaction t = new Transaction()) {
            filteredMappings = t.guildCharacters.getAllMappingsOrderedByRank(guildId).stream();
        }
        if (alreadyLinkedChars) {
            filteredMappings = filteredMappings.filter(mapping -> mapping.dcId() != null);
        } else {
            filteredMappings = filteredMappings.filter(mapping -> mapping.dcId() == null);
        }
        if (userId != null) {
            filteredMappings = filteredMappings.filter(mapping -> userId.equals(mapping.dcId()));
        }
        if (charRank != null) {
            filteredMappings = filteredMappings.filter(mapping -> charRank.equals(mapping.wowCharRank()));
        }
        if (charNamePattern != null) {
            filteredMappings = filteredMappings.filter(mapping -> charNamePattern.doesMatch(mapping.wowCharName()));
        }


        filteredMappings = filteredMappings.limit(Config.maxAnnouncedCharactersAtOnce);
        int foundResults = 0;
        StringBuilder result = new StringBuilder("`");
        for (Iterator<DbGuildCharacters.Wow2DcMapping> it = filteredMappings.iterator(); it.hasNext(); ) {
            foundResults++;
            DbGuildCharacters.Wow2DcMapping mapping = it.next();
            result.append(mapping.wowCharName())
                    .append("-")
                    .append(mapping.wowCharServer())
                    .append(" (")
                    .append(mapping.wowCharId())
                    .append(")");
            if (alreadyLinkedChars) {
                result.append(" to ").append(mapping.dcName()).append(" (").append(mapping.dcId()).append(")");
            }
            result.append("\n");
            if (result.length() > 1500) {
                result.append("`");
                hook.sendMessage(result.toString()).queue();
                result = new StringBuilder("`");
            }
        }
        result.append("Found ").append(foundResults).append(" characters`");
        hook.sendMessage(result.toString()).queue();
    }
}
