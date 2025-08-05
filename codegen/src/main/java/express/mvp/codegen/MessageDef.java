package express.mvp.codegen;

import java.util.List;

/**
 * Represents a message definition in a .mvpe.yaml file.
 */
public class MessageDef {
    private String name;
    private List<FieldDef> fields;
    
    public MessageDef() {
    }
    
    public MessageDef(String name, List<FieldDef> fields) {
        this.name = name;
        this.fields = fields;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<FieldDef> getFields() {
        return fields;
    }
    
    public void setFields(List<FieldDef> fields) {
        this.fields = fields;
    }
    
    @Override
    public String toString() {
        return "MessageDef{" +
                "name='" + name + '\'' +
                ", fields=" + fields +
                '}';
    }
}