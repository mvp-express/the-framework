package express.mvp.codegen;

import java.util.List;

/**
 * Represents the root schema of a .mvpe.yaml file.
 */
public class ServiceSchema {
    private String service;
    private int id;
    private List<MethodDef> methods;
    private List<MessageDef> messages;
    
    public ServiceSchema() {
    }
    
    public ServiceSchema(String service, int id, List<MethodDef> methods, List<MessageDef> messages) {
        this.service = service;
        this.id = id;
        this.methods = methods;
        this.messages = messages;
    }
    
    public String getService() {
        return service;
    }
    
    public void setService(String service) {
        this.service = service;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public List<MethodDef> getMethods() {
        return methods;
    }
    
    public void setMethods(List<MethodDef> methods) {
        this.methods = methods;
    }
    
    public List<MessageDef> getMessages() {
        return messages;
    }
    
    public void setMessages(List<MessageDef> messages) {
        this.messages = messages;
    }
    
    @Override
    public String toString() {
        return "ServiceSchema{" +
                "service='" + service + '\'' +
                ", id=" + id +
                ", methods=" + methods +
                ", messages=" + messages +
                '}';
    }
}