package com.eventportal.dao;

import java.sql.SQLException;

@FunctionalInterface
interface SqlFunction<T, R> {
    R apply(T value) throws SQLException;
}
