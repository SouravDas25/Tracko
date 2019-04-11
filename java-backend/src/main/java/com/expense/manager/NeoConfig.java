package com.expense.manager;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;

import javax.naming.NamingException;
import javax.sql.DataSource;

@Configuration
@Profile({"neo"})
public class NeoConfig
{
    @Bean(destroyMethod="")
    public DataSource jndiDataSource() throws IllegalArgumentException, NamingException
    {
        JndiDataSourceLookup dataSourceLookup = new JndiDataSourceLookup();

        DataSource ds = dataSourceLookup.getDataSource("java:comp/env/jdbc/DefaultDB");

        return ds;
    }
}
