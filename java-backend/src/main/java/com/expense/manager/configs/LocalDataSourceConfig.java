package com.expense.manager.configs;


import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
@Profile("standalone")
public class LocalDataSourceConfig {

    @Bean
    public DataSource getlocalDataSource() {
        DataSource ds = DataSourceBuilder.create().url("jdbc:mysql://localhost:3306/test")
                .username("root").password("").driverClassName("com.mysql.jdbc.Driver").build();
        return ds;
    }

}
