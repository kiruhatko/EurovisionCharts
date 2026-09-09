package com.eurovision.analytics.telegram;

public interface Command {

    /** Command word without the leading slash, e.g. {@code "track"}. */
    String name();

    void handle(CommandContext ctx);

    default boolean requiresAdmin() {
        return false;
    }
}
