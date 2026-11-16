package com.chatapp;

import java.util.Scanner;

/**
 * Console application: registration and login (Part 1), QuickChat sending
 * messages (Part 2) and stored messages with reports (Part 3).
 */
public class Main {

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        // ===== PART 1: REGISTRATION =====
        System.out.println("===== REGISTRATION =====");
        System.out.print("Enter your first name: ");
        String firstName = input.nextLine();
        System.out.print("Enter your last name: ");
        String lastName = input.nextLine();

        Login login = new Login(firstName, lastName);

        String username;
        do {
            System.out.print("Enter a username: ");
            username = input.nextLine();
            System.out.println(login.usernameMessage(username));
        } while (!login.checkUserName(username));

        String password;
        do {
            System.out.print("Enter a password: ");
            password = input.nextLine();
            System.out.println(login.passwordMessage(password));
        } while (!login.checkPasswordComplexity(password));

        String cellNumber;
        do {
            System.out.print("Enter your cell phone number (e.g. +27838968976): ");
            cellNumber = input.nextLine();
            System.out.println(login.cellNumberMessage(cellNumber));
        } while (!login.registerCellNumber(cellNumber));

        System.out.println(login.registerUser(username, password));

        // ===== PART 1: LOGIN =====
        System.out.println();
        System.out.println("===== LOGIN =====");
        boolean loggedIn;
        do {
            System.out.print("Enter your username: ");
            String enteredUsername = input.nextLine();
            System.out.print("Enter your password: ");
            String enteredPassword = input.nextLine();

            loggedIn = login.loginUser(enteredUsername, enteredPassword);
            System.out.println(login.returnLoginStatus(loggedIn));
        } while (!loggedIn);

        // The logged-in user is the sender of every message
        Message.setCurrentSender(firstName + " " + lastName);

        // ===== QUICKCHAT (only reached after a successful login) =====
        runQuickChat(input);
        input.close();
    }

    /**
     * Runs the QuickChat menu until the user chooses to quit.
     */
    private static void runQuickChat(Scanner input) {
        System.out.println();
        System.out.println("Welcome to QuickChat.");

        // Part 3: read previously stored messages from the JSON file into the array
        MessageStore.loadStoredMessages();

        int messageLimit = readPositiveInt(input, "How many messages do you wish to enter? ");
        int messagesEntered = 0;
        boolean running = true;

        while (running) {
            System.out.println();
            System.out.println("1) Send Messages");
            System.out.println("2) Show recently sent messages");
            System.out.println("3) Quit");
            System.out.println("4) Stored Messages");
            int choice = readInt(input, "Choose an option: ");

            switch (choice) {
                case 1:
                    if (messagesEntered >= messageLimit) {
                        System.out.println("You have entered all " + messageLimit + " messages.");
                    } else {
                        messagesEntered++;
                        sendOneMessage(input);
                    }
                    break;
                case 2:
                    System.out.println(Message.printMessages());
                    break;
                case 3:
                    running = false;
                    break;
                case 4:
                    storedMessagesMenu(input);
                    break;
                default:
                    System.out.println("Invalid option. Please choose 1, 2, 3 or 4.");
            }
        }

        System.out.println("Total number of messages sent: " + Message.returnTotalMessages());
    }

    /**
     * Collects one message from the user and sends, discards or stores it.
     */
    private static void sendOneMessage(Scanner input) {
        // Recipient
        String recipient;
        do {
            System.out.print("Enter the recipient cell number: ");
            recipient = input.nextLine();
            if (!Message.isValidRecipient(recipient)) {
                System.out.println("Cell phone number is incorrectly formatted or does not contain "
                        + "an international code. Please correct the number and try again.");
            }
        } while (!Message.isValidRecipient(recipient));

        // Message text (250 characters maximum)
        String text;
        do {
            System.out.print("Enter your message: ");
            text = input.nextLine();
            System.out.println(Message.checkMessageLength(text));
        } while (!Message.isValidLength(text) || text.trim().isEmpty());

        Message message = new Message(recipient, text);
        System.out.println(message.messageIdGenerated());

        // Send, disregard or store
        System.out.println("1) Send Message");
        System.out.println("2) Disregard Message");
        System.out.println("3) Store Message to send later");
        int option = readInt(input, "Choose an option: ");
        System.out.println(message.sentMessage(option));

        if (option == 1) {
            System.out.println();
            System.out.println(message.getDetails());
        } else if (option == 2) {
            System.out.print("Press 0 to delete the message: ");
            if (input.nextLine().trim().equals("0")) {
                System.out.println("Message deleted.");
            }
        }
    }

    /**
     * Part 3: menu for working with the stored messages array.
     */
    private static void storedMessagesMenu(Scanner input) {
        boolean back = false;

        while (!back) {
            System.out.println();
            System.out.println("===== STORED MESSAGES =====");
            System.out.println("1) Display sender and recipient of all stored messages");
            System.out.println("2) Display the longest stored message");
            System.out.println("3) Search for a message ID");
            System.out.println("4) Search for all messages to a recipient");
            System.out.println("5) Delete a message using its message hash");
            System.out.println("6) Display report of all stored messages");
            System.out.println("7) Back to main menu");
            int choice = readInt(input, "Choose an option: ");

            switch (choice) {
                case 1:
                    System.out.println(MessageStore.displaySendersAndRecipients());
                    break;
                case 2:
                    System.out.println(MessageStore.longestStoredMessage());
                    break;
                case 3:
                    System.out.print("Enter the message ID: ");
                    Message found = MessageStore.findByMessageId(input.nextLine().trim());
                    if (found == null) {
                        System.out.println("Message ID not found.");
                    } else {
                        System.out.println("Recipient: " + found.getRecipient());
                        System.out.println("Message: " + found.getText());
                    }
                    break;
                case 4:
                    System.out.print("Enter the recipient cell number: ");
                    java.util.List<String> matches = MessageStore.searchRecipient(input.nextLine().trim());
                    if (matches.isEmpty()) {
                        System.out.println("No messages found for that recipient.");
                    } else {
                        for (String match : matches) {
                            System.out.println(match);
                        }
                    }
                    break;
                case 5:
                    System.out.print("Enter the message hash: ");
                    System.out.println(MessageStore.deleteByHash(input.nextLine().trim().toUpperCase()));
                    break;
                case 6:
                    System.out.println(MessageStore.storedMessagesReport());
                    break;
                case 7:
                    back = true;
                    break;
                default:
                    System.out.println("Invalid option. Please choose 1 to 7.");
            }
        }
    }

    /** Reads a whole number, asking again until the input is valid. */
    private static int readInt(Scanner input, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(input.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    /** Reads a whole number greater than zero. */
    private static int readPositiveInt(Scanner input, String prompt) {
        int value;
        do {
            value = readInt(input, prompt);
            if (value < 1) {
                System.out.println("Please enter a number greater than 0.");
            }
        } while (value < 1);
        return value;
    }
}
