package tterrag.tppibot.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.MessageEvent;

import tterrag.tppibot.annotations.Subscribe;
import tterrag.tppibot.config.Config;
import tterrag.tppibot.interfaces.ICommand;
import tterrag.tppibot.registry.CommandRegistry;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class AddCommand extends Command
{
    private Config config;

    public static List<CustomCommand> commandsAdded = new ArrayList<CustomCommand>();

    public AddCommand()
    {
        super("addcmd", PermLevel.CHANOP);
        config = new Config("customCommands.json");

        commandsAdded = new Gson().fromJson(config.getText(), new TypeToken<List<CustomCommand>>(){}.getType());

        if (commandsAdded == null)
            commandsAdded = new ArrayList<CustomCommand>();

        for (ICommand c : commandsAdded)
        {
            CommandRegistry.registerCommand(c);
        }
    }

    @Override
    public boolean onCommand(MessageEvent<?> event, String... args)
    {
        if (args.length < 2)
        {
            sendNotice(event.getUser(), "This requires at least two args, [command name] and [message]!");
            return false;
        }

        String cmdName = args[0];

        args = ArrayUtils.remove(args, 0);

        String toAdd = StringUtils.join(args, ' ');

        if (commandAlreadyRegistered(cmdName))
        {
            sendNotice(event.getUser(), cmdName + " is already registered!");
            return false;
        }

        CustomCommand command = new CustomCommand(cmdName, PermLevel.ALL, toAdd);
        CommandRegistry.registerCommand(command);
        commandsAdded.add(command);

        sendNotice(event.getUser(), "Registered command " + cmdName);

        return true;
    }

    private boolean commandAlreadyRegistered(String cmdName)
    {
        return CommandRegistry.getCommand(cmdName) != null;
    }

    @Override
    public String getDesc()
    {
        return "Adds a command with the name of the first argument and the output of any following arguments to the command registry.";
    }

    @Override
    public boolean shouldReceiveEvents()
    {
        return true;
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent<PircBotX> event)
    {
        config.writeJsonToFile(commandsAdded);
    }
}
