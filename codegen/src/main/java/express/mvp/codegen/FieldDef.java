package express.mvp.codegen;

/**
 * Represents a field definition in a message within a .mvpe.yaml file.
 */
public class FieldDef {
    private String name;
    private String type;
    private boolean optional;
    private String defaultValue;
    
    public FieldDef() {
    }
    
    public FieldDef(String name, String type) {
        this.name = name;
        this.type = type;
        this.optional = false;
    }
    
    public FieldDef(String name, String type, boolean optional, String defaultValue) {
        this.name = name;
        this.type = type;
        this.optional = optional;
        this.defaultValue = defaultValue;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public boolean isOptional() {
        return optional;
    }
    
    public void setOptional(boolean optional) {
        this.optional = optional;
    }
    
    public String getDefaultValue() {
        return defaultValue;
    }
    
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    @Override
    public String toString() {
        return "FieldDef{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", optional=" + optional +
                ", defaultValue='" + defaultValue + '\'' +
                '}';
    }
}