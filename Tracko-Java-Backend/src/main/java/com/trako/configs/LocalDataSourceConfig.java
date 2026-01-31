package com.trako.configs;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
@Profile("standalone")
public class LocalDataSourceConfig {

    @Autowired
    Environment environment;

    @Bean
    public DataSource getlocalDataSource() {
        String db_host = environment.getProperty("DBHOST");
        String url = db_host != null ? "jdbc:postgresql://" + db_host + ":5432/expense_manager" :
                "jdbc:postgresql://localhost:5432/expense_manager";
        DataSource ds = DataSourceBuilder.create().url(url)
                .username("root").password("root").driverClassName("org.postgresql.Driver").build();
        return ds;
    }

}
