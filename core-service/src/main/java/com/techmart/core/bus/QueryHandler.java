package com.techmart.core.bus;

public interface QueryHandler<Q extends Query<R>, R> {
    R handle(Q query);
}
