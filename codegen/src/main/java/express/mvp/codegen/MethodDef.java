package express.mvp.codegen;

/**
 * Represents a method definition in a .mvpe.yaml file.
 */
public class MethodDef {
    private String name;
    private int id;
    private String request;
    private String response;
    
    public MethodDef() {
    }
    
    public MethodDef(String name, int id, String request, String response) {
        this.name = name;
        this.id = id;
        this.request = request;
        this.response = response;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getRequest() {
        return request;
    }
    
    public void setRequest(String request) {
        this.request = request;
    }
    
    public String getResponse() {
        return response;
    }
    
    public void setResponse(String response) {
        this.response = response;
    }
    
    @Override
    public String toString() {
        return "MethodDef{" +
                "name='" + name + '\'' +
                ", id=" + id +
                ", request='" + request + '\'' +
                ", response='" + response + '\'' +
                '}';
    }
}