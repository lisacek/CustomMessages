package space.devport.wertik.custommessages.commands.subcommands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import space.devport.utils.commands.struct.ArgumentRange;
import space.devport.utils.commands.struct.CommandResult;
import space.devport.wertik.custommessages.MessagePlugin;
import space.devport.wertik.custommessages.commands.CommandUtils;
import space.devport.wertik.custommessages.commands.MessageSubCommand;
import space.devport.wertik.custommessages.system.struct.MessageType;

public class PreviewSubCommand extends MessageSubCommand {

    public PreviewSubCommand(MessagePlugin plugin) {
        super(plugin, "preview");
    }

    @Override
    protected @NotNull CommandResult perform(@NotNull CommandSender sender, @NotNull String label, String[] args) {

        MessageType type = CommandUtils.parseType(sender, args[0]);

        Player target;
        if (args.length > 1) {
            target = CommandUtils.parsePlayer(sender, args[1]);

            if (target == null) return CommandResult.FAILURE;

            if (!sender.hasPermission("custommessages.preview.others")) return CommandResult.NO_PERMISSION;
        } else {
            if (!(sender instanceof Player)) return CommandResult.NO_CONSOLE;

            target = (Player) sender;
        }

        String message = plugin.getMessageManager().getFormattedMessage(target, type);
        language.getPrefixed("Commands.Preview.Done")
                .replace("%player%", target.getName())
                .replace("%type%", type.toString().toLowerCase())
                .replace("%message%", message == null ? "&cNone" : message)
                .send(sender);
        return CommandResult.SUCCESS;
    }

    @Override
    public @NotNull String getDefaultUsage() {
        return "/%label% preview <type> (player)";
    }

    @Override
    public @NotNull String getDefaultDescription() {
        return "Preview a message.";
    }

    @Override
    public @NotNull ArgumentRange getRange() {
        return new ArgumentRange(1, 2);
    }
}