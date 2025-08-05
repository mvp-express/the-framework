package express.mvp.codegen;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Parser for .mvpe.yaml schema files.
 * Converts YAML schema definitions into Java objects for code generation.
 */
public class MvpeSchemaParser {
    
    private final Yaml yaml;
    
    public MvpeSchemaParser() {
        this.yaml = new Yaml(new Constructor(ServiceSchema.class));
    }
    
    /**
     * Parses a .mvpe.yaml file and returns the service schema.
     */
    public ServiceSchema parseSchema(Path schemaFile) throws IOException {
        try (InputStream inputStream = new FileInputStream(schemaFile.toFile())) {
            return yaml.load(inputStream);
        }
    }
    
    /**
     * Parses a .mvpe.yaml file from string content.
     */
    public ServiceSchema parseSchema(String yamlContent) {
        return yaml.load(yamlContent);
    }
    
    /**
     * Validates the parsed schema for correctness.
     */
    public void validateSchema(ServiceSchema schema) {
        if (schema.getService() == null || schema.getService().trim().isEmpty()) {
            throw new IllegalArgumentException("Service name is required");
        }
        
        if (schema.getId() <= 0) {
            throw new IllegalArgumentException("Service ID must be positive");
        }
        
        if (schema.getMethods() == null || schema.getMethods().isEmpty()) {
            throw new IllegalArgumentException("At least one method is required");
        }
        
        // Validate methods
        for (MethodDef method : schema.getMethods()) {
            validateMethod(method);
        }
        
        // Validate messages if present
        if (schema.getMessages() != null) {
            for (MessageDef message : schema.getMessages()) {
                validateMessage(message);
            }
        }
    }
    
    private void validateMethod(MethodDef method) {
        if (method.getName() == null || method.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Method name is required");
        }
        
        if (method.getId() <= 0) {
            throw new IllegalArgumentException("Method ID must be positive: " + method.getName());
        }
        
        if (method.getRequest() == null || method.getRequest().trim().isEmpty()) {
            throw new IllegalArgumentException("Method request type is required: " + method.getName());
        }
        
        if (method.getResponse() == null || method.getResponse().trim().isEmpty()) {
            throw new IllegalArgumentException("Method response type is required: " + method.getName());
        }
    }
    
    private void validateMessage(MessageDef message) {
        if (message.getName() == null || message.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Message name is required");
        }
        
        if (message.getFields() == null || message.getFields().isEmpty()) {
            throw new IllegalArgumentException("Message must have at least one field: " + message.getName());
        }
        
        for (FieldDef field : message.getFields()) {
            validateField(field, message.getName());
        }
    }
    
    private void validateField(FieldDef field, String messageName) {
        if (field.getName() == null || field.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Field name is required in message: " + messageName);
        }
        
        if (field.getType() == null || field.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Field type is required: " + field.getName() + " in " + messageName);
        }
        
        // Validate supported types
        String type = field.getType().toLowerCase();
        if (!isSupportedType(type)) {
            throw new IllegalArgumentException("Unsupported field type: " + field.getType() + " for field " + field.getName());
        }
    }
    
    private boolean isSupportedType(String type) {
        return switch (type) {
            case "string", "int32", "int64", "boolean", "float", "double", "bytes" -> true;
            default -> false;
        };
    }
}