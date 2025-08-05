package express.mvp.codegen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Generates Java code from parsed .mvpe.yaml schemas.
 * Creates service interfaces, DTO records, and dispatcher classes.
 */
public class JavaCodeGenerator {
    
    private final String basePackage;
    
    public JavaCodeGenerator(String basePackage) {
        this.basePackage = basePackage;
    }
    
    /**
     * Generates all Java code for the given schema.
     */
    public void generateCode(ServiceSchema schema, Path outputDir) throws IOException {
        // Create package directory
        Path packageDir = createPackageDirectory(outputDir);
        
        // Generate service interface
        generateServiceInterface(schema, packageDir);
        
        // Generate DTO records for messages
        if (schema.getMessages() != null) {
            for (MessageDef message : schema.getMessages()) {
                generateMessageRecord(message, packageDir);
            }
        }
        
        // Generate dispatcher
        generateDispatcher(schema, packageDir);
    }
    
    private Path createPackageDirectory(Path outputDir) throws IOException {
        String[] packageParts = basePackage.split("\\.");
        Path packageDir = outputDir;
        for (String part : packageParts) {
            packageDir = packageDir.resolve(part);
        }
        Files.createDirectories(packageDir);
        return packageDir;
    }
    
    /**
     * Generates the service interface.
     */
    private void generateServiceInterface(ServiceSchema schema, Path packageDir) throws IOException {
        StringBuilder code = new StringBuilder();
        
        // Package declaration
        code.append("package ").append(basePackage).append(";\n\n");
        
        // Interface declaration
        code.append("/**\n");
        code.append(" * Generated service interface for ").append(schema.getService()).append("\n");
        code.append(" */\n");
        code.append("public interface ").append(schema.getService()).append(" {\n\n");
        
        // Generate methods
        for (MethodDef method : schema.getMethods()) {
            code.append("    /**\n");
            code.append("     * ").append(method.getName()).append(" method\n");
            code.append("     */\n");
            code.append("    ").append(method.getResponse()).append(" ");
            code.append(toLowerCamelCase(method.getName())).append("(");
            code.append(method.getRequest()).append(" request);\n\n");
        }
        
        code.append("}\n");
        
        // Write to file
        Path interfaceFile = packageDir.resolve(schema.getService() + ".java");
        Files.writeString(interfaceFile, code.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    /**
     * Generates a DTO record for a message.
     */
    private void generateMessageRecord(MessageDef message, Path packageDir) throws IOException {
        StringBuilder code = new StringBuilder();
        
        // Package declaration
        code.append("package ").append(basePackage).append(";\n\n");
        
        // Record declaration
        code.append("/**\n");
        code.append(" * Generated DTO record for ").append(message.getName()).append("\n");
        code.append(" */\n");
        code.append("public record ").append(message.getName()).append("(\n");
        
        // Generate fields
        List<FieldDef> fields = message.getFields();
        for (int i = 0; i < fields.size(); i++) {
            FieldDef field = fields.get(i);
            code.append("    ").append(mapTypeToJava(field.getType())).append(" ");
            code.append(field.getName());
            
            if (i < fields.size() - 1) {
                code.append(",");
            }
            code.append("\n");
        }
        
        code.append(") {}\n");
        
        // Write to file
        Path recordFile = packageDir.resolve(message.getName() + ".java");
        Files.writeString(recordFile, code.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    /**
     * Generates the service dispatcher.
     */
    private void generateDispatcher(ServiceSchema schema, Path packageDir) throws IOException {
        StringBuilder code = new StringBuilder();
        
        // Package declaration
        code.append("package ").append(basePackage).append(";\n\n");
        
        // Class declaration
        String dispatcherName = schema.getService() + "Dispatcher";
        code.append("/**\n");
        code.append(" * Generated dispatcher for ").append(schema.getService()).append("\n");
        code.append(" */\n");
        code.append("public class ").append(dispatcherName).append(" {\n\n");
        
        // Field for service implementation
        code.append("    private final ").append(schema.getService()).append(" service;\n\n");
        
        // Constructor
        code.append("    public ").append(dispatcherName).append("(").append(schema.getService()).append(" service) {\n");
        code.append("        this.service = service;\n");
        code.append("    }\n\n");
        
        // Dispatch method
        code.append("    public Object dispatch(int methodId, Object request) {\n");
        code.append("        return switch (methodId) {\n");
        
        for (MethodDef method : schema.getMethods()) {
            code.append("            case ").append(method.getId()).append(" -> service.");
            code.append(toLowerCamelCase(method.getName())).append("((");
            code.append(method.getRequest()).append(") request);\n");
        }
        
        code.append("            default -> throw new IllegalArgumentException(\"Unknown method ID: \" + methodId);\n");
        code.append("        };\n");
        code.append("    }\n");
        code.append("}\n");
        
        // Write to file
        Path dispatcherFile = packageDir.resolve(dispatcherName + ".java");
        Files.writeString(dispatcherFile, code.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    /**
     * Maps schema types to Java types.
     */
    private String mapTypeToJava(String schemaType) {
        return switch (schemaType.toLowerCase()) {
            case "string" -> "String";
            case "int32" -> "int";
            case "int64" -> "long";
            case "boolean" -> "boolean";
            case "float" -> "float";
            case "double" -> "double";
            case "bytes" -> "byte[]";
            default -> throw new IllegalArgumentException("Unsupported type: " + schemaType);
        };
    }
    
    /**
     * Converts PascalCase to lowerCamelCase.
     */
    private String toLowerCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return Character.toLowerCase(input.charAt(0)) + input.substring(1);
    }
}