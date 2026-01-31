package com.trako.configs;

import com.trako.util.DBInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.SQLException;

@Configuration
@Profile({"neo"})
public class NeoConfig 
{
	private static final Logger log = LoggerFactory.getLogger(NeoConfig.class);
	
	@Bean(destroyMethod="")
	public DataSource jndiDataSource() throws IllegalArgumentException, NamingException, SQLException
	{
		JndiDataSourceLookup dataSourceLookup = new JndiDataSourceLookup();
		
		DataSource ds = dataSourceLookup.getDataSource("java:comp/env/jdbc/DefaultDB");
		
		DBInformation dbInfo = new DBInformation(ds.getConnection().getMetaData());
		log.info(dbInfo.toString());
		
		return ds;
	}
}