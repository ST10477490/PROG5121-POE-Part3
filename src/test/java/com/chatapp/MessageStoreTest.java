package com.chatapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Part 3 unit tests for the arrays, searching, deleting and reports, using the
 * test data supplied in the brief.
 */
public class MessageStoreTest {

    private static final String TEXT_1 = "Did you get the cake?";
    private static final String TEXT_2 = "Where are you? You are late! I have asked you to be on time.";
    private static final String TEXT_3 = "Yohoooo, I am at your gate.";
    private static final String TEXT_4 = "It is dinner time!";
    private static final String TEXT_5 = "Ok, I am leaving without you.";

    private static final String STORAGE_FILE = "target/test-store-messages.json";

    private Message message1;
    private Message message2;
    private Message message3;
    private Message message4;
    private Message message5;

    @BeforeEach
    void setUp() throws IOException {
        Message.resetAll();
        Message.setStoragePath(STORAGE_FILE);
        Message.setCurrentSender("Kyle Smith");
        Files.deleteIfExists(Paths.get(STORAGE_FILE));

        // Test data messages 1 to 5 (message IDs are set by the developer)
        message1 = new Message("1234567801", 0, "+27834557896", TEXT_1);
        message2 = new Message("2345678902", 1, "+27838884567", TEXT_2);
        message3 = new Message("3456789003", 2, "+27834484567", TEXT_3);
        message4 = new Message("0838884567", 3, "+27838884999", TEXT_4);
        message5 = new Message("5678901205", 4, "+27838884567", TEXT_5);

        // Apply each message's flag, which populates the arrays
        message1.sentMessage(1);   // Sent
        message2.sentMessage(3);   // Stored
        message3.sentMessage(2);   // Disregarded
        message4.sentMessage(1);   // Sent
        message5.sentMessage(3);   // Stored
    }

    // ---------- assertEquals tests from the brief ----------

    @Test
    void sentMessagesArrayCorrectlyPopulated() {
        assertEquals(List.of("Did you get the cake?", "It is dinner time!"),
                MessageStore.getSentMessages());
    }

    @Test
    void displayTheLongestMessage() {
        assertEquals("Where are you? You are late! I have asked you to be on time.",
                MessageStore.longestStoredMessage());
    }

    @Test
    void searchForMessageId() {
        assertEquals("It is dinner time!", MessageStore.searchMessageId("0838884567"));
    }

    @Test
    void searchAllMessagesForARecipient() {
        assertEquals(List.of("Where are you? You are late! I have asked you to be on time.",
                "Ok, I am leaving without you."),
                MessageStore.searchRecipient("+27838884567"));
    }

    @Test
    void deleteAMessageUsingItsHash() {
        assertEquals("Message: \"Where are you? You are late! I have asked you to be on time\""
                + " successfully deleted.",
                MessageStore.deleteByHash(message2.getMessageHash()));
    }

    @Test
    void displayReportOfSentMessages() {
        String expected = "SENT MESSAGES REPORT\n"
                + "Message Hash: " + message1.getMessageHash() + "\n"
                + "Recipient: +27834557896\n"
                + "Message: Did you get the cake?\n\n"
                + "Message Hash: " + message4.getMessageHash() + "\n"
                + "Recipient: +27838884999\n"
                + "Message: It is dinner time!";
        assertEquals(expected, MessageStore.sentMessagesReport());
    }

    // ---------- extra tests for the other arrays and features ----------

    @Test
    void disregardedMessagesArrayCorrectlyPopulated() {
        assertEquals(List.of("Yohoooo, I am at your gate."), MessageStore.getDisregardedMessages());
    }

    @Test
    void storedMessagesArrayIsReadFromTheJsonFile() {
        // Clear the memory, then read the stored messages back from the file
        MessageStore.resetAll();
        int loaded = MessageStore.loadStoredMessages();

        assertEquals(2, loaded);
        assertEquals(List.of(TEXT_2, TEXT_5), MessageStore.getStoredMessages());
    }

    @Test
    void messageHashAndIdArraysContainAllMessages() {
        assertEquals(List.of(message1.getMessageHash(), message2.getMessageHash(),
                message3.getMessageHash(), message4.getMessageHash(), message5.getMessageHash()),
                MessageStore.getMessageHashes());
        assertEquals(List.of("1234567801", "2345678902", "3456789003", "0838884567", "5678901205"),
                MessageStore.getMessageIDs());
        assertEquals("08:3:ITTIME", message4.getMessageHash());
    }

    @Test
    void deletingAMessageRemovesItFromTheStoredArrayAndFile() {
        MessageStore.deleteByHash(message2.getMessageHash());
        assertEquals(List.of(TEXT_5), MessageStore.getStoredMessages());

        // A fresh read of the JSON file also no longer contains it
        MessageStore.resetAll();
        MessageStore.loadStoredMessages();
        assertEquals(List.of(TEXT_5), MessageStore.getStoredMessages());
    }

    @Test
    void deletingAnUnknownHashChangesNothing() {
        assertEquals("Message hash not found.", MessageStore.deleteByHash("00:0:UNKNOWN"));
        assertEquals(2, MessageStore.getStoredMessages().size());
    }

    @Test
    void sendersAndRecipientsOfStoredMessagesAreDisplayed() {
        assertEquals("Sender: Kyle Smith | Recipient: +27838884567\n"
                + "Sender: Kyle Smith | Recipient: +27838884567",
                MessageStore.displaySendersAndRecipients());
    }

    @Test
    void storedMessagesReportShowsFullDetails() {
        String report = MessageStore.storedMessagesReport();
        assertTrue(report.contains("Message ID: 2345678902"));
        assertTrue(report.contains("Message Hash: " + message2.getMessageHash()));
        assertTrue(report.contains("Sender: Kyle Smith"));
        assertTrue(report.contains("Recipient: +27838884567"));
        assertTrue(report.contains("Message: " + TEXT_5));
    }

    @Test
    void searchForAnUnknownMessageId() {
        assertEquals("Message ID not found.", MessageStore.searchMessageId("9999999999"));
    }
}
