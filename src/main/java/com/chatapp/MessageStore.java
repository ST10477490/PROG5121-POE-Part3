package com.chatapp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Part 3 - Store Data and Display Task Report.
 * Holds the arrays of sent, disregarded and stored messages, message hashes
 * and message IDs, and provides the features that use them: display, longest
 * message, search, delete and reports.
 */
public final class MessageStore {

    // Status flags for a message
    public static final String SENT = "Sent";
    public static final String DISREGARDED = "Disregarded";
    public static final String STORED = "Stored";

    // Every message that has been sent, disregarded or stored (in order)
    private static final List<Message> records = new ArrayList<>();

    // The arrays required by the brief (populated from the records)
    private static final ArrayList<String> sentMessages = new ArrayList<>();
    private static final ArrayList<String> disregardedMessages = new ArrayList<>();
    private static final ArrayList<String> storedMessages = new ArrayList<>();
    private static final ArrayList<String> messageHashes = new ArrayList<>();
    private static final ArrayList<String> messageIDs = new ArrayList<>();

    // File that stored messages are written to and read from
    private static String storagePath = "messages.json";

    private MessageStore() {
    }

    // ------------------------------------------------------------------
    // Populating the arrays
    // ------------------------------------------------------------------

    /**
     * Records a message with its flag and refreshes all of the arrays.
     *
     * @param message the message
     * @param status  SENT, DISREGARDED or STORED
     */
    public static void record(Message message, String status) {
        message.setStatus(status);
        if (!records.contains(message)) {
            records.add(message);
        }
        rebuildArrays();
    }

    /**
     * Clears the arrays and fills them again from the recorded messages.
     */
    private static void rebuildArrays() {
        sentMessages.clear();
        disregardedMessages.clear();
        storedMessages.clear();
        messageHashes.clear();
        messageIDs.clear();

        for (Message message : records) {
            switch (message.getStatus()) {
                case SENT:        sentMessages.add(message.getText()); break;
                case DISREGARDED: disregardedMessages.add(message.getText()); break;
                case STORED:      storedMessages.add(message.getText()); break;
                default: break;
            }
            messageHashes.add(message.getMessageHash());
            messageIDs.add(message.getMessageId());
        }
    }

    /**
     * Saves every stored message to the JSON file.
     *
     * @return true if the file was written
     */
    public static boolean saveStoredMessages() {
        List<Map<String, Object>> data = new ArrayList<>();
        for (Message message : records) {
            if (STORED.equals(message.getStatus())) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("messageId", message.getMessageId());
                entry.put("messageNumber", message.getNumMessagesSent());
                entry.put("messageHash", message.getMessageHash());
                entry.put("sender", message.getSender());
                entry.put("recipient", message.getRecipient());
                entry.put("message", message.getText());
                data.add(entry);
            }
        }
        return JsonStorage.write(storagePath, data);
    }

    /**
     * Reads the stored messages from the JSON file into the arrays.
     * Messages that are already in memory are not added twice.
     *
     * @return the number of messages read from the file
     */
    public static int loadStoredMessages() {
        int loaded = 0;
        for (Map<String, String> entry : JsonStorage.read(storagePath)) {
            String id = entry.get("messageId");
            if (id == null || findByMessageId(id) != null) {
                continue;
            }

            int number = 0;
            try {
                number = Integer.parseInt(entry.getOrDefault("messageNumber", "0"));
            } catch (NumberFormatException e) {
                // keep the default number
            }

            Message message = new Message(id, number,
                    entry.getOrDefault("recipient", ""), entry.getOrDefault("message", ""));
            message.setSender(entry.getOrDefault("sender", "Unknown"));
            message.setStatus(STORED);
            records.add(message);
            loaded++;
        }
        rebuildArrays();
        return loaded;
    }

    // ------------------------------------------------------------------
    // Features using the arrays
    // ------------------------------------------------------------------

    /**
     * Displays the sender and recipient of all stored messages.
     */
    public static String displaySendersAndRecipients() {
        StringBuilder output = new StringBuilder();
        for (Message message : records) {
            if (STORED.equals(message.getStatus())) {
                output.append("Sender: ").append(message.getSender())
                      .append(" | Recipient: ").append(message.getRecipient()).append("\n");
            }
        }
        return output.length() == 0 ? "No stored messages." : output.toString().trim();
    }

    /**
     * @return the longest stored message
     */
    public static String longestStoredMessage() {
        if (storedMessages.isEmpty()) {
            return "No stored messages.";
        }
        String longest = storedMessages.get(0);
        for (String message : storedMessages) {
            if (message.length() > longest.length()) {
                longest = message;
            }
        }
        return longest;
    }

    /**
     * Finds a message by its message ID.
     *
     * @return the message, or null if there is no match
     */
    public static Message findByMessageId(String messageId) {
        for (Message message : records) {
            if (message.getMessageId().equals(messageId)) {
                return message;
            }
        }
        return null;
    }

    /**
     * Searches for a message ID and returns the message text.
     */
    public static String searchMessageId(String messageId) {
        Message message = findByMessageId(messageId);
        return message == null ? "Message ID not found." : message.getText();
    }

    /**
     * Finds all sent or stored messages for a recipient.
     *
     * @param recipient the recipient's cell number
     * @return the matching message texts, in the order they were created
     */
    public static List<String> searchRecipient(String recipient) {
        List<String> matches = new ArrayList<>();
        for (Message message : records) {
            boolean sentOrStored = SENT.equals(message.getStatus())
                    || STORED.equals(message.getStatus());
            if (sentOrStored && message.getRecipient().equals(recipient)) {
                matches.add(message.getText());
            }
        }
        return matches;
    }

    /**
     * Deletes a stored message using its message hash and updates the JSON file.
     *
     * @param hash the message hash
     * @return the confirmation message
     */
    public static String deleteByHash(String hash) {
        for (Message message : records) {
            if (STORED.equals(message.getStatus()) && message.getMessageHash().equals(hash)) {
                String text = message.getText();
                records.remove(message);
                rebuildArrays();
                saveStoredMessages();

                // The confirmation shows the message without its closing full stop
                if (text.endsWith(".")) {
                    text = text.substring(0, text.length() - 1);
                }
                return "Message: \"" + text + "\" successfully deleted.";
            }
        }
        return "Message hash not found.";
    }

    /**
     * @return a report of all sent messages (hash, recipient and message)
     */
    public static String sentMessagesReport() {
        StringBuilder report = new StringBuilder("SENT MESSAGES REPORT\n");
        boolean found = false;
        for (Message message : records) {
            if (SENT.equals(message.getStatus())) {
                found = true;
                report.append("Message Hash: ").append(message.getMessageHash()).append("\n")
                      .append("Recipient: ").append(message.getRecipient()).append("\n")
                      .append("Message: ").append(message.getText()).append("\n\n");
            }
        }
        return found ? report.toString().trim() : "No messages have been sent.";
    }

    /**
     * @return a report with the full details of all stored messages
     */
    public static String storedMessagesReport() {
        StringBuilder report = new StringBuilder("STORED MESSAGES REPORT\n");
        boolean found = false;
        for (Message message : records) {
            if (STORED.equals(message.getStatus())) {
                found = true;
                report.append("Message ID: ").append(message.getMessageId()).append("\n")
                      .append("Message Hash: ").append(message.getMessageHash()).append("\n")
                      .append("Sender: ").append(message.getSender()).append("\n")
                      .append("Recipient: ").append(message.getRecipient()).append("\n")
                      .append("Message: ").append(message.getText()).append("\n\n");
            }
        }
        return found ? report.toString().trim() : "No stored messages.";
    }

    /**
     * @return the full details of every sent message (used by Message.printMessages)
     */
    public static String sentMessagesDetails() {
        StringBuilder output = new StringBuilder();
        for (Message message : records) {
            if (SENT.equals(message.getStatus())) {
                output.append(message.getDetails()).append("\n\n");
            }
        }
        return output.length() == 0 ? "No messages have been sent." : output.toString().trim();
    }

    /**
     * @return the total number of messages sent
     */
    public static int totalSent() {
        return sentMessages.size();
    }

    // ------------------------------------------------------------------
    // Access to the arrays
    // ------------------------------------------------------------------

    public static List<String> getSentMessages() { return new ArrayList<>(sentMessages); }
    public static List<String> getDisregardedMessages() { return new ArrayList<>(disregardedMessages); }
    public static List<String> getStoredMessages() { return new ArrayList<>(storedMessages); }
    public static List<String> getMessageHashes() { return new ArrayList<>(messageHashes); }
    public static List<String> getMessageIDs() { return new ArrayList<>(messageIDs); }

    /**
     * Changes the JSON file used for stored messages (used for testing).
     */
    public static void setStoragePath(String path) {
        storagePath = path;
    }

    /**
     * Clears everything in memory (the JSON file is not touched).
     */
    public static void resetAll() {
        records.clear();
        rebuildArrays();
    }
}
