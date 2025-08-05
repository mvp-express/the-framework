package express.mvp.codegen;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Main orchestrator for MVP.Express code generation.
 * Coordinates parsing of .mvpe.yaml files and generation of Java code.
 */
public class CodegenOrchestrator {
    
    private final MvpeSchemaParser parser;
    private final JavaCodeGenerator javaGenerator;
    
    public CodegenOrchestrator(String basePackage) {
        this.parser = new MvpeSchemaParser();
        this.javaGenerator = new JavaCodeGenerator(basePackage);
    }
    
    /**
     * Generates Java code from a .mvpe.yaml schema file.
     * 
     * @param schemaFile Path to the .mvpe.yaml file
     * @param outputDir Output directory for generated Java files
     * @throws IOException if file operations fail
     * @throws IllegalArgumentException if schema validation fails
     */
    public void generateFromFile(Path schemaFile, Path outputDir) throws IOException {
        // Parse the schema
        ServiceSchema schema = parser.parseSchema(schemaFile);
        
        // Validate the schema
        parser.validateSchema(schema);
        
        // Generate Java code
        javaGenerator.generateCode(schema, outputDir);
        
        System.out.println("Successfully generated code for service: " + schema.getService());
        System.out.println("Output directory: " + outputDir.toAbsolutePath());
    }
    
    /**
     * Generates Java code from YAML content string.
     * 
     * @param yamlContent YAML schema content
     * @param outputDir Output directory for generated Java files
     * @throws IOException if file operations fail
     * @throws IllegalArgumentException if schema validation fails
     */
    public void generateFromString(String yamlContent, Path outputDir) throws IOException {
        // Parse the schema
        ServiceSchema schema = parser.parseSchema(yamlContent);
        
        // Validate the schema
        parser.validateSchema(schema);
        
        // Generate Java code
        javaGenerator.generateCode(schema, outputDir);
        
        System.out.println("Successfully generated code for service: " + schema.getService());
        System.out.println("Output directory: " + outputDir.toAbsolutePath());
    }
    
    /**
     * Validates a schema file without generating code.
     * 
     * @param schemaFile Path to the .mvpe.yaml file
     * @return true if valid, throws exception if invalid
     * @throws IOException if file operations fail
     * @throws IllegalArgumentException if schema validation fails
     */
    public boolean validateSchema(Path schemaFile) throws IOException {
        ServiceSchema schema = parser.parseSchema(schemaFile);
        parser.validateSchema(schema);
        System.out.println("Schema validation successful for: " + schema.getService());
        return true;
    }
    
    /**
     * Prints schema information without generating code.
     * 
     * @param schemaFile Path to the .mvpe.yaml file
     * @throws IOException if file operations fail
     */
    public void printSchemaInfo(Path schemaFile) throws IOException {
        ServiceSchema schema = parser.parseSchema(schemaFile);
        
        System.out.println("=== Schema Information ===");
        System.out.println("Service: " + schema.getService());
        System.out.println("ID: " + schema.getId());
        System.out.println("Methods: " + (schema.getMethods() != null ? schema.getMethods().size() : 0));
        
        if (schema.getMethods() != null) {
            for (MethodDef method : schema.getMethods()) {
                System.out.println("  - " + method.getName() + " (ID: " + method.getId() + ")");
                System.out.println("    Request: " + method.getRequest());
                System.out.println("    Response: " + method.getResponse());
            }
        }
        
        System.out.println("Messages: " + (schema.getMessages() != null ? schema.getMessages().size() : 0));
        
        if (schema.getMessages() != null) {
            for (MessageDef message : schema.getMessages()) {
                System.out.println("  - " + message.getName());
                if (message.getFields() != null) {
                    for (FieldDef field : message.getFields()) {
                        System.out.println("    * " + field.getName() + ": " + field.getType());
                    }
                }
            }
        }
    }
}