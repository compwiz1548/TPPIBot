package tterrag.tppibot.listeners;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import tterrag.tppibot.interfaces.ICommand;
import tterrag.tppibot.interfaces.ICommand.PermLevel;
import tterrag.tppibot.registry.CommandRegistry;
import tterrag.tppibot.registry.PermRegistry;
import tterrag.tppibot.runnables.MessageSender;
import tterrag.tppibot.util.IRCUtils;

public class PrivateMessageListener extends ListenerAdapter<PircBotX>
{
    @Override
    public void onPrivateMessage(PrivateMessageEvent<PircBotX> event) throws Exception
    {
        String[] args = event.getMessage().split(" ");
        if (args.length <= 0) { return; }

        List<String> lines = new ArrayList<String>();
        List<ICommand> commands = CommandRegistry.getCommands();

        for (int i = 0; i < commands.size(); i++)
        {
            ICommand c = commands.get(i);
            if (c.getIdent().equals(args[0].startsWith(MessageListener.controlChar) ? args[0].substring(MessageListener.controlChar.length()) : args[0]))
            {
                Channel channel = null;
                boolean invalidChan = false;
                if (args.length >= 2)
                {
                    channel = IRCUtils.getChannelByName(event.getBot(), IRCUtils.fmtChan(args[1]));
                    invalidChan = channel == null;
                }
                
                if (channel != null)
                {
                    PermLevel userLevel = PermRegistry.instance().getPermLevelForUser(channel, event.getUser());
                    if (IRCUtils.isPermLevelAboveOrEqualTo(userLevel, c.getPermLevel()) && IRCUtils.isPermLevelAboveOrEqualTo(userLevel, PermLevel.TRUSTED))
                    {
                        c.onCommand(event.getBot(), event.getUser(), channel, lines, ArrayUtils.remove(ArrayUtils.remove(args, 0), 0));
                    }
                    else
                    {
                        lines.add("You are not of the level " + (c.getPermLevel() == PermLevel.DEFAULT ? PermLevel.TRUSTED : c.getPermLevel()) + " in channel " + args[1] + ".");
                    }
                }
                else if (c.executeWithoutChannel())
                {
                    if (c.getPermLevel().equals(PermLevel.DEFAULT) || PermRegistry.instance().isController(event.getUser()))
                    {
                        c.onCommand(event.getBot(), event.getUser(), null, lines, ArrayUtils.remove(args, 0));
                    }
                    else
                    {
                        lines.add("You may not execute " + c.getPermLevel().toString().toLowerCase() + " commands.");
                    }
                }
                else
                {
                    if (invalidChan)
                    {
                        lines.add(args[1] + " is not a valid channel that this bot is connected to.");
                    }
                    else
                    {
                        lines.add("This command must be sent to a specific channel, please specify this as the first arg.");
                    }
                }

                if (i < commands.size() && commands.get(i) != c)
                {
                    i--;
                }
            }
        }

        for (String s : lines)
        {
            MessageSender.instance.enqueue(event.getBot(), event.getUser().getNick(), s);
        }
    }
}
