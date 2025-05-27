package com.tetrisbot;

/**
 * This class handles interactions with the Discord API. Each instance should
 * be deployed to a separate channel.
 * Status codes:
    * 0 = success
    * 1 = ... (work in progress)
 */

public class Discord {
    //Add static variables here as necessary

    String guildId;
    String channelId;
    //Add instance variables here as necessary

    /**
     * Links the instance of the bot to the channel in the server specified by
     * the parameters. Returns status code based on result of link attempt.
     * @param guild The id of the server that contains the channel to link to.
     * @param channel The id of the channel to link to.
     * @return An integer representing the result of the link attempt, as
     * mapped in the class description.
     */
    public int linkToChannel(String guild, String channel) {
        return 0;
    }

    /**
     * Sends the specified message to the channel that the bot is linked to.
     * @param text The message content to be sent.
     * @return An integer representing the result of the messaging attempt, as
     * mapped in the class description.
     */
    public int sendMessage(String text) {
        return 0;
    }

    /**
     * Sends the specified message to the specified user's direct messages.
     * @param user The id of the user to send the message to.
     * @param text The message content to be sent.
     * @return An integer representing the result of the messaging attempt, as
     * mapped in the class description.
     */
    public int sendDirectMsg(String user, String text) {
        return 0;
    }
    //addReaction()

    //retrieveReactions()
}
