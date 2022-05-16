package com.dewdropper.config.loader;

import java.io.IOException;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

public class YamlPropertyLoaderFactory implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {

        String sourceName = resource.getResource().getFilename();

        if (StringUtils.isNotBlank(sourceName) && (sourceName.endsWith(".yml") || sourceName.endsWith(".yaml"))) {

            YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
            factory.setResources(resource.getResource());
            factory.afterPropertiesSet();
            Properties properties = factory.getObject();

            return new PropertiesPropertySource(sourceName, properties);
        }
        return null;
    }
}
