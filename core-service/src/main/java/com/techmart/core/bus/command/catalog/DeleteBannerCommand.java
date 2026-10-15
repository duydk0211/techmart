package com.techmart.core.bus.command.catalog;

import com.techmart.core.bus.Command;

import java.util.UUID;

public record DeleteBannerCommand(UUID id) implements Command<Void> {
}
