package com.techmart.core.bus.impl;

import com.techmart.core.bus.Command;
import com.techmart.core.bus.CommandHandler;
import com.techmart.core.bus.Query;
import com.techmart.core.bus.QueryHandler;
import com.techmart.core.bus.SpringBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SpringBusImpl implements SpringBus {

    private static final Logger log = LoggerFactory.getLogger(SpringBusImpl.class);

    private final Map<Class<? extends Command<?>>, CommandHandler<?, ?>> commandHandlers = new HashMap<>();
    private final Map<Class<? extends Query<?>>, QueryHandler<?, ?>> queryHandlers = new HashMap<>();

    @SuppressWarnings("rawtypes")
    public SpringBusImpl(ApplicationContext applicationContext) {
        Map<String, CommandHandler> cmdBeans = applicationContext.getBeansOfType(CommandHandler.class);
        for (CommandHandler handler : cmdBeans.values()) {
            Class<?>[] generics = GenericTypeResolver.resolveTypeArguments(handler.getClass(), CommandHandler.class);
            if (generics != null && generics.length > 0) {
                @SuppressWarnings("unchecked")
                Class<? extends Command<?>> cmdClass = (Class<? extends Command<?>>) generics[0];
                commandHandlers.put(cmdClass, handler);
                log.info("[SpringBusImpl][init] Registered CommandHandler {} for command {}", handler.getClass().getSimpleName(), cmdClass.getSimpleName());
            }
        }

        Map<String, QueryHandler> queryBeans = applicationContext.getBeansOfType(QueryHandler.class);
        for (QueryHandler handler : queryBeans.values()) {
            Class<?>[] generics = GenericTypeResolver.resolveTypeArguments(handler.getClass(), QueryHandler.class);
            if (generics != null && generics.length > 0) {
                @SuppressWarnings("unchecked")
                Class<? extends Query<?>> qClass = (Class<? extends Query<?>>) generics[0];
                queryHandlers.put(qClass, handler);
                log.info("[SpringBusImpl][init] Registered QueryHandler {} for query {}", handler.getClass().getSimpleName(), qClass.getSimpleName());
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R execute(Command<R> command) {
        CommandHandler<Command<R>, R> handler = (CommandHandler<Command<R>, R>) commandHandlers.get(command.getClass());
        if (handler == null) {
            log.error("[SpringBusImpl][execute] No CommandHandler registered for command: {}", command.getClass().getName());
            throw new IllegalStateException("No CommandHandler registered for " + command.getClass().getName());
        }
        return handler.handle(command);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R query(Query<R> query) {
        QueryHandler<Query<R>, R> handler = (QueryHandler<Query<R>, R>) queryHandlers.get(query.getClass());
        if (handler == null) {
            log.error("[SpringBusImpl][query] No QueryHandler registered for query: {}", query.getClass().getName());
            throw new IllegalStateException("No QueryHandler registered for " + query.getClass().getName());
        }
        return handler.handle(query);
    }
}
