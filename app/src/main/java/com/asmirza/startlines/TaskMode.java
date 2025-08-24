package com.asmirza.startlines;

public class TaskMode {
    private static boolean taskAdded = false;
    private static boolean taskComplete = false;

    public static void resetTaskAdded() {
        taskAdded = false;
    }

    public static void markTaskAdded() {
        taskAdded = true;
    }

    public static void resetTaskComplete() {
        taskComplete = false;
    }

    public static void markTaskComplete() {
        taskComplete = true;
    }

    public static boolean wasTaskAdded() {
        return taskAdded;
    }

    public static boolean wasTaskCompleted() {
        return taskComplete;
    }
}
