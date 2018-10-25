package org.apereo.cas.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyNameException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test additional metadata validity.
 *
 * @since 6.0
 */
@SpringBootTest(classes = AopAutoConfiguration.class)
public class AdditionalMetadataVerificationTests {

    @Autowired
    private ResourceLoader resourceLoader;

    /**
     * Make sure the property names are canonical (not camel case) otherwise app won't start.
     * Spring boot {@link org.springframework.boot.context.properties.migrator.PropertiesMigrationListener}
     * will prevent startup if property names aren't valid.
     *
     * @throws IOException if additional property file is missing
     */
    @Test
    @SuppressWarnings("unchecked")
    public void verifyMetaData() throws IOException {
        val mapper = new ObjectMapper().findAndRegisterModules();
        val jsonFile = resourceLoader.getResource(
            CasConfigurationProperties.class.getClassLoader().getResource("META-INF/additional-spring-configuration-metadata.json").toString()
        );
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        val values = new TypeReference<Map<String, Set<ConfigurationMetadataProperty>>>() {
        };
        val jsonMap = (Map<String, Set<ConfigurationMetadataProperty>>) mapper.readValue(jsonFile.getURL(), values);
        val props = jsonMap.get("properties");
        for (val prop : props) {
            try {
                ConfigurationPropertyName.of(prop.getName());
                val deprecation = prop.getDeprecation();
                if (deprecation != null && StringUtils.isNotBlank(deprecation.getReplacement())) {
                    ConfigurationPropertyName.of(deprecation.getReplacement());
                    if (deprecation.getReplacement().contains("[0]")) {
                        fail("No array references allowed in replacement value.");
                    }
                }
            } catch (final InvalidConfigurationPropertyNameException e) {
                fail(e.getMessage());
            }
        }
    }
}
