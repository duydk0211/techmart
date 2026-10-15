package com.techmart.core.bus.command.storage;

import com.techmart.core.bus.Command;

public record DeleteFileCommand(String fileName, String bucket) implements Command<Void> {
}
