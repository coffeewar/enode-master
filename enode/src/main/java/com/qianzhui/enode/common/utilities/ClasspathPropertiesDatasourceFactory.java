package com.qianzhui.enode.common.utilities;

import org.apache.commons.dbcp2.BasicDataSourceFactory;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by junbo_xu on 2016/6/6.
 */
public class ClasspathPropertiesDatasourceFactory {
    public static DataSource createDataSource(String propertiesResource) {
        InputStream in = ClasspathPropertiesDatasourceFactory.class.getClassLoader().getResourceAsStream(propertiesResource);
        Properties properties = new Properties();

        try {
            properties.load(in);

            return BasicDataSourceFactory.createDataSource(properties);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
