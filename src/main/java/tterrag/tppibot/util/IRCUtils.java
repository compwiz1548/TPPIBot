package tterrag.tppibot.util;

import static tterrag.tppibot.interfaces.ICommand.PermLevel.DEFAULT;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UserLevel;
import org.pircbotx.hooks.WaitForQueue;
import org.pircbotx.hooks.events.WhoisEvent;

import tterrag.tppibot.Main;
import tterrag.tppibot.commands.Command;
import tterrag.tppibot.commands.Mode;
import tterrag.tppibot.commands.Mode.BotMode;
import tterrag.tppibot.interfaces.ICommand.PermLevel;
import tterrag.tppibot.registry.CommandRegistry;
import tterrag.tppibot.registry.PermRegistry;
import tterrag.tppibot.runnables.MessageSender;

public class IRCUtils
{
    public static boolean userIsOp(Channel channel, User user)
    {
        return user.getUserLevels(channel).contains(UserLevel.OP);
    }

    public static boolean userIsVoice(Channel channel, User user)
    {
        return user.getUserLevels(channel).contains(UserLevel.VOICE);
    }

    /**
     * Can a user perform a command or action with the passed perm level
     * 
     * @param channel - Channel the action was performed in
     * @param user - User to check
     * @param perm - {@link PermLevel} to check against
     */
    public static boolean userMatchesPerms(Channel channel, User user, PermLevel perm)
    {
        if (perm == null)
            perm = PermLevel.DEFAULT;

        PermLevel userPerm = PermRegistry.instance().getPermLevelForUser(channel, user);

        return isPermLevelAboveOrEqualTo(userPerm, perm);

    }

    /**
     * Can a user perform a command or action with the passed perm level, use this version if you need to repeat the check multiple times
     * 
     * @param channel - Channel the action was performed in
     * @param user - User to check
     * @param userPerm - {@link PermLevel} of the user (not checked)
     * @param perm - {@link PermLevel} to check against
     */
    public static boolean userMatchesPerms(Channel channel, User user, PermLevel userPerm, PermLevel toCheck)
    {
        toCheck = toCheck == null ? DEFAULT : toCheck;

        return isPermLevelAboveOrEqualTo(userPerm, toCheck);
    }

    /**
     * Determines if a user is at or above this level, can only be used for the <code>{@link PermLevel}s</code> returned by {@link PermLevel.getSettablePermLevels()}
     * 
     * @throws IllegalArgumentException If the {@link PermLevel} is not returned by the aforementioned method.
     */
    public static boolean isUserAboveOrEqualTo(Channel chan, PermLevel perm, User user)
    {
        if (!ArrayUtils.contains(PermLevel.getSettablePermLevels(), perm)) { throw new IllegalArgumentException("The perm level " + perm.toString() + " is not valid."); }

        return isPermLevelAboveOrEqualTo(PermRegistry.instance().getPermLevelForUser(chan, user), perm);
    }

    /**
     * Performs a simple ordinal check on the perm levels, but also checks for null and converts to {@link PermLevel.DEFAULT}
     * @return <code>true</code> if <code>userLevel</code> is above or equal to <code>toCheck</code>
     */
    public static boolean isPermLevelAboveOrEqualTo(PermLevel userLevel, PermLevel toCheck)
    {
        if (userLevel == null)
            userLevel = PermLevel.DEFAULT;
        if (toCheck == null)
            toCheck = PermLevel.DEFAULT;

        return userLevel.ordinal() >= toCheck.ordinal();
    }

    public static String getMessageForUser(User user, String message, String... args)
    {
        return message.replace("%user%", args.length >= 1 ? args[0] : user.getNick());
    }

    @Deprecated
    public static void sendNoticeForUser(Channel channel, User user, String message, String... args)
    {
        if (args.length > 0)
        {
            User to = getUserByNick(channel, args[0]);
            if (to != null)
            {
                to.send().notice(message.replace("%user%", to.getNick()));
                return;
            }
        }
        user.send().notice(message.replace("%user%", user.getNick()));
    }

    public static User getUserByNick(Channel channel, String nick)
    {
        for (User u : channel.getUsers())
        {
            if (u.getNick().equalsIgnoreCase(nick))
                return u;
        }
        return null;
    }

    public static Channel getChannelByName(PircBotX bot, String channel)
    {
        for (Channel c : bot.getUserBot().getChannels())
        {
            if (c.getName().equalsIgnoreCase(channel))
                return c;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static String getAccount(User u)
    {
        String user = "";
        Main.bot.sendRaw().rawLineNow("WHOIS " + u.getNick());
        WaitForQueue waitForQueue = new WaitForQueue(Main.bot);
        WhoisEvent<PircBotX> test = null;
        try
        {
            test = waitForQueue.waitFor(WhoisEvent.class);
            waitForQueue.close();
            user = test.getRegisteredAs();
        }
        catch (InterruptedException ex)
        {
            ex.printStackTrace();
            user = null;
        }

        return user;
    }
    
    public static String fmtChan(String chan)
    {
        return chan.startsWith("#") ? chan : "#" + chan;
    }
    
    public static void modeSensitiveEnqueue(PircBotX bot, User user, Channel channel, String message)
    {
        BotMode mode = Mode.getMode(channel.getName());
        switch(mode)
        {
        case MESSAGE:
            MessageSender.instance.enqueue(bot, channel.getName(), message);
            break;
        case NOTICE:
            MessageSender.instance.enqueueNotice(bot, user.getNick(), message);
            break;
        case PM:
            MessageSender.instance.enqueue(bot, user.getNick(), message);
            break;
        }
    }
    
    public static void timeout(PircBotX bot, User user, Channel channel, String amount)
    {
        Command quiet = (Command) CommandRegistry.getCommand("timeout");
        List<String> toQueue = new ArrayList<String>();
        quiet.onCommand(bot, user, channel, toQueue, user.getNick(),"" + amount);
        for (String s : toQueue)
        {
            IRCUtils.modeSensitiveEnqueue(bot, user, channel, s);
        }
    }
    
    public static int getSecondsFromString(String s) throws NumberFormatException
    {
        String modifier = "none";

        char c = s.charAt(s.length() - 1);
        modifier = c >= '0' && c <= '9' ? "none" : Character.toString(c).toLowerCase();
        if (!modifier.equals("none"))
        {
            s = s.substring(0, s.length() - 1);
        }

        int mult = getMultiplierForModifier(modifier);
        
        return Integer.parseInt(s) * mult;
    }
    
    public static int getMultiplierForModifier(String modifier)
    {
        switch (modifier)
        {
        case "s":
            return 1;
        case "m":
            return 60;
        case "h":
            return 3600;
        case "d":
            return 86400;
        case "w":
            return 604800;
        default:
            return 60;
        }
    }
}
