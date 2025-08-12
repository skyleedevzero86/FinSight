package com.sleekydz86.finsight.batch.config.module;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link YamlPropertySourceFactory}.
 *
 * Test framework: JUnit 5 (org.junit.jupiter).
 * These tests exercise both happy-path YAML loading and edge/failure behaviors,
 * including filename handling and invalid YAML content propagation.
 */
class YamlPropertySourceFactoryTest {

    private final YamlPropertySourceFactory factory = new YamlPropertySourceFactory();

    @Nested
    @DisplayName("createPropertySource - ClassPathResource (has filename)")
    class ClassPathResourceTests {

        @Test
        @DisplayName("Loads YAML properties and uses the resource filename as source name")
        void loadsYamlFromClasspathAndUsesFilename() {
            // Given a classpath YAML file
            ClassPathResource cpr = new ClassPathResource("test-sample.yml");
            assertTrue(cpr.exists(), "Expected classpath resource test-sample.yml to exist");
            EncodedResource encoded = new EncodedResource(cpr);

            // When
            PropertiesPropertySource pps = factory.createPropertySource(null, encoded);

            // Then
            assertNotNull(pps, "PropertiesPropertySource should not be null");
            assertEquals("test-sample.yml", pps.getName(), "Source name should be the resource filename");

            Properties props = (Properties) pps.getSource();
            assertNotNull(props, "Backed Properties should not be null");
            assertFalse(props.isEmpty(), "Properties should not be empty for valid YAML");

            // Verify a selection of flattened keys produced by YamlPropertiesFactoryBean
            // Spring flattens nested YAML to dotted properties.
            assertEquals("FinSight", props.getProperty("app.name"));
            assertEquals("1.2.3", props.getProperty("app.version"));
            assertEquals("localhost", props.getProperty("database.host"));
            assertEquals("5432", props.getProperty("database.port"));
            assertEquals("true", props.getProperty("database.enabled"));

            // Array/list flattening: indexes are used for list elements
            assertEquals("import", props.getProperty("features[0]"));
            assertEquals("export", props.getProperty("features[1]"));
            assertEquals("analytics", props.getProperty("features[2]"));

            assertEquals("a", props.getProperty("list.nested[0]"));
            assertEquals("b", props.getProperty("list.nested[1]"));
            assertEquals("c", props.getProperty("list.nested[2]"));
        }

        @Test
        @DisplayName("Produces non-null (possibly empty) Properties for empty YAML content")
        void emptyYamlProducesEmptyProperties() {
            // Given an empty YAML resource (still with filename via ClassPathResource approach)
            byte[] emptyBytes = new byte[0];
            ByteArrayResource bar = new ByteArrayResource(emptyBytes) {
                @Override
                public String getFilename() {
                    // Simulate a filename-bearing resource
                    return "empty.yml";
                }
            };
            EncodedResource encoded = new EncodedResource(bar, StandardCharsets.UTF_8);

            PropertiesPropertySource pps = factory.createPropertySource(null, encoded);
            assertNotNull(pps, "PropertiesPropertySource should not be null");
            assertEquals("empty.yml", pps.getName(), "Expected provided filename to be used as source name");

            Properties props = (Properties) pps.getSource();
            assertNotNull(props, "Backed Properties should not be null even for empty YAML");
            assertTrue(props.isEmpty(), "Empty YAML should yield empty properties");
        }
    }

    @Nested
    @DisplayName("createPropertySource - ByteArrayResource (no filename)")
    class ByteArrayResourceTests {

        @Test
        @DisplayName("Uses 'unknown' as source name when resource filename is null")
        void usesUnknownWhenFilenameIsNull() {
            String yaml = "service:\n  id: svc-123\n";
            ByteArrayResource bar = new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8)) {
                @Override
                public String getFilename() {
                    // Simulate missing filename
                    return null;
                }
            };
            EncodedResource encoded = new EncodedResource(bar, StandardCharsets.UTF_8);

            PropertiesPropertySource pps = factory.createPropertySource(null, encoded);
            assertNotNull(pps, "PropertiesPropertySource should not be null");
            assertEquals("unknown", pps.getName(), "Expected 'unknown' name when no filename is available");

            Properties props = (Properties) pps.getSource();
            assertNotNull(props, "Backed Properties should not be null");
            assertEquals("svc-123", props.getProperty("service.id"), "YAML content should be parsed");
        }

        @Test
        @DisplayName("Propagates exception for invalid YAML content")
        void propagatesExceptionForInvalidYaml() {
            // Missing closing bracket makes this invalid YAML
            String invalidYaml = "bad:\n  list: [one, two\n";
            ByteArrayResource bar = new ByteArrayResource(invalidYaml.getBytes(StandardCharsets.UTF_8)) {
                @Override
                public String getFilename() {
                    return null;
                }
            };
            EncodedResource encoded = new EncodedResource(bar, StandardCharsets.UTF_8);

            // The underlying YamlPropertiesFactoryBean/SnakeYAML may throw a runtime exception.
            assertThrows(RuntimeException.class,
                    () -> factory.createPropertySource(null, encoded),
                    "Invalid YAML should result in a runtime exception being propagated");
        }
    }
}