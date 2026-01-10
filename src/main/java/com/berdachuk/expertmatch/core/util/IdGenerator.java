package com.berdachuk.expertmatch.core.util;

import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.Enumeration;

/**
 * Utility for generating MongoDB-compatible 24-character hexadecimal IDs.
 * <p>
 * Implements MongoDB ObjectId algorithm:
 * - 4 bytes: timestamp (seconds since Unix epoch)
 * - 3 bytes: machine identifier (hash of MAC address)
 * - 2 bytes: process ID (or random if unavailable)
 * - 3 bytes: counter (random start, then increments)
 * <p>
 * Format: 24-character hexadecimal string (0-9, a-f)
 * Pattern: ^[0-9a-fA-F]{24}$
 */
public class IdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ID_LENGTH = 24; // 12 bytes = 24 hex chars

    // Cached machine identifier (3 bytes)
    private static final byte[] MACHINE_ID = generateMachineId();

    // Process identifier (2 bytes)
    private static final byte[] PROCESS_ID = generateProcessId();
    private static final Object COUNTER_LOCK = new Object();
    // Counter (3 bytes) - starts random, increments per call
    private static int counter = RANDOM.nextInt(0xFFFFFF);

    /**
     * Generates a MongoDB-compatible 24-character hexadecimal ID using ObjectId algorithm.
     * <p>
     * Structure (12 bytes total):
     * - Bytes 0-3:   Timestamp (4 bytes, big-endian)
     * - Bytes 4-6:   Machine identifier (3 bytes)
     * - Bytes 7-8:   Process ID (2 bytes)
     * - Bytes 9-11:  Counter (3 bytes, big-endian)
     *
     * @return 24-character hexadecimal string
     */
    public static String generateId() {
        // 12 bytes total
        byte[] idBytes = new byte[12];
        ByteBuffer buffer = ByteBuffer.wrap(idBytes);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // 4 bytes: Timestamp (seconds since Unix epoch)
        int timestamp = (int) (System.currentTimeMillis() / 1000);
        buffer.putInt(timestamp);

        // 3 bytes: Machine identifier
        buffer.put(MACHINE_ID, 0, 3);

        // 2 bytes: Process ID
        buffer.put(PROCESS_ID, 0, 2);

        // 3 bytes: Counter (increment with thread safety)
        synchronized (COUNTER_LOCK) {
            counter = (counter + 1) & 0xFFFFFF; // Wrap at 24 bits
            buffer.put((byte) ((counter >>> 16) & 0xFF));
            buffer.put((byte) ((counter >>> 8) & 0xFF));
            buffer.put((byte) (counter & 0xFF));
        }

        // Convert to 24-character hex string
        return bytesToHex(idBytes);
    }

    /**
     * Generates a 3-byte machine identifier from MAC address hash.
     * Falls back to random bytes if MAC address cannot be determined.
     */
    private static byte[] generateMachineId() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                byte[] mac = ni.getHardwareAddress();
                if (mac != null && mac.length >= 6) {
                    // Use first 3 bytes of MAC address as machine ID
                    byte[] machineId = new byte[3];
                    System.arraycopy(mac, 0, machineId, 0, 3);
                    return machineId;
                }
            }
        } catch (Exception e) {
            // Fall through to random generation
        }

        // Fallback: generate random 3 bytes
        byte[] machineId = new byte[3];
        RANDOM.nextBytes(machineId);
        return machineId;
    }

    /**
     * Generates a 2-byte process identifier.
     * Uses actual process ID if available, otherwise random.
     */
    private static byte[] generateProcessId() {
        byte[] processId = new byte[2];

        try {
            // Try to get actual process ID
            long pid = ProcessHandle.current().pid();
            if (pid > 0 && pid <= 0xFFFF) {
                processId[0] = (byte) ((pid >>> 8) & 0xFF);
                processId[1] = (byte) (pid & 0xFF);
                return processId;
            }
        } catch (Exception e) {
            // Fall through to random generation
        }

        // Fallback: generate random 2 bytes
        RANDOM.nextBytes(processId);
        return processId;
    }

    /**
     * Converts byte array to hexadecimal string.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    /**
     * Validates if a string is a valid MongoDB-compatible ID.
     *
     * @param id The ID string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidId(String id) {
        if (id == null || id.length() != ID_LENGTH) {
            return false;
        }
        return id.matches("^[0-9a-fA-F]{24}$");
    }

    /**
     * Generates an employee ID in external system format.
     * External system uses numeric strings like "8760000000000420950" (19 digits).
     * <p>
     * Format: 19-digit numeric string
     * Pattern: ^[0-9]{19}$
     *
     * @return 19-digit numeric string
     */
    public static String generateEmployeeId() {
        // Generate 19-digit numeric string
        // Format: starts with 876, followed by 16 random digits
        StringBuilder sb = new StringBuilder(19);
        sb.append("876"); // Prefix from external system format
        for (int i = 0; i < 16; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Validates if a string is a valid employee ID (external system format).
     *
     * @param id The ID string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidEmployeeId(String id) {
        if (id == null || id.length() != 19) {
            return false;
        }
        return id.matches("^[0-9]{19}$");
    }

    /**
     * Generates a project ID in external system format.
     * External system uses numeric strings like "4060741400384209073" (19 digits).
     * <p>
     * Format: 19-digit numeric string
     * Pattern: ^[0-9]{19}$
     *
     * @return 19-digit numeric string
     */
    public static String generateProjectId() {
        // Generate 19-digit numeric string
        // Format: starts with 406, followed by 16 random digits
        StringBuilder sb = new StringBuilder(19);
        sb.append("406"); // Prefix from external system format for projects
        for (int i = 0; i < 16; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Generates a customer ID in external system format.
     * External system uses numeric strings like "5060741400384209073" (19 digits).
     * <p>
     * Format: 19-digit numeric string
     * Pattern: ^[0-9]{19}$
     *
     * @return 19-digit numeric string
     */
    public static String generateCustomerId() {
        // Generate 19-digit numeric string
        // Format: starts with 506, followed by 16 random digits
        StringBuilder sb = new StringBuilder(19);
        sb.append("506"); // Prefix from external system format for customers
        for (int i = 0; i < 16; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
