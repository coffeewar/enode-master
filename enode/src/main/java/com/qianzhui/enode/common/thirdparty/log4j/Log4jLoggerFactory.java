package com.qianzhui.enode.common.thirdparty.log4j;

import com.qianzhui.enode.common.logging.ILogger;
import com.qianzhui.enode.common.logging.ILoggerFactory;
import org.apache.log4j.*;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by junbo_xu on 2016/3/2.
 */
public class Log4jLoggerFactory implements ILoggerFactory {
    public Log4jLoggerFactory(String configFile) {
        if(configFile != null){
            File file = new File(configFile);
            if (!file.exists()) {
                URL resource = this.getClass().getClassLoader().getResource(configFile);
                if (resource != null) {

                    try {
                        file = new File(resource.toURI());
                    } catch (URISyntaxException e) {
                        //ignore
                    }
                }
            }

            if(file.exists()){
                PropertyConfigurator.configureAndWatch(file.getAbsolutePath());
                return;
            }
        }

        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%d %p [%c] - <%m>%n")));
    }

    @Override
    public ILogger create(String name) {
        return new Log4jLogger(Logger.getLogger(name));
    }

    @Override
    public ILogger create(Class type) {
        return new Log4jLogger(Logger.getLogger(type));
    }
}
