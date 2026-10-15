package com.techmart.core.bus;

public interface CommandHandler<C extends Command<R>, R> {
    R handle(C command);
}
