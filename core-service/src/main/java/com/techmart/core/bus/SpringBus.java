package com.techmart.core.bus;

public interface SpringBus {
    <R> R execute(Command<R> command);
    <R> R query(Query<R> query);
}
