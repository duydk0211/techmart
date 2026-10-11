package com.techmart.core.bus.command.user;

import com.techmart.core.bus.Command;

import java.util.UUID;

public record DeleteStaffCommand(UUID staffId) implements Command<Void> {}
