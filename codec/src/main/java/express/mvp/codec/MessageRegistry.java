package express.mvp.codec;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Registry for mapping message IDs to message types and handlers.
 * Provides thread-safe registration and lookup of message types.
 */
public class MessageRegistry {
    
    private final Map<Integer, MessageTypeInfo> messageTypes = new ConcurrentHashMap<>();
    private final Map<Class<?>, Integer> classToId = new ConcurrentHashMap<>();
    
    /**
     * Information about a registered message type.
     */
    public static class MessageTypeInfo {
        private final int messageId;
        private final Class<?> messageClass;
        private final String messageName;
        
        public MessageTypeInfo(int messageId, Class<?> messageClass, String messageName) {
            this.messageId = messageId;
            this.messageClass = messageClass;
            this.messageName = messageName;
        }
        
        public int getMessageId() {
            return messageId;
        }
        
        public Class<?> getMessageClass() {
            return messageClass;
        }
        
        public String getMessageName() {
            return messageName;
        }
    }
    
    /**
     * Registers a message type with its ID and class.
     */
    public void registerMessage(int messageId, Class<?> messageClass) {
        registerMessage(messageId, messageClass, messageClass.getSimpleName());
    }
    
    /**
     * Registers a message type with its ID, class, and custom name.
     */
    public void registerMessage(int messageId, Class<?> messageClass, String messageName) {
        if (messageTypes.containsKey(messageId)) {
            throw new IllegalArgumentException("Message ID " + messageId + " is already registered");
        }
        
        if (classToId.containsKey(messageClass)) {
            throw new IllegalArgumentException("Message class " + messageClass.getName() + " is already registered");
        }
        
        MessageTypeInfo info = new MessageTypeInfo(messageId, messageClass, messageName);
        messageTypes.put(messageId, info);
        classToId.put(messageClass, messageId);
    }
    
    /**
     * Gets message type information by message ID.
     */
    public MessageTypeInfo getMessageType(int messageId) {
        return messageTypes.get(messageId);
    }
    
    /**
     * Gets message ID by message class.
     */
    public Integer getMessageId(Class<?> messageClass) {
        return classToId.get(messageClass);
    }
    
    /**
     * Gets message ID by message instance.
     */
    public Integer getMessageId(Object message) {
        return getMessageId(message.getClass());
    }
    
    /**
     * Checks if a message ID is registered.
     */
    public boolean isRegistered(int messageId) {
        return messageTypes.containsKey(messageId);
    }
    
    /**
     * Checks if a message class is registered.
     */
    public boolean isRegistered(Class<?> messageClass) {
        return classToId.containsKey(messageClass);
    }
    
    /**
     * Gets the number of registered message types.
     */
    public int size() {
        return messageTypes.size();
    }
    
    /**
     * Clears all registered message types.
     */
    public void clear() {
        messageTypes.clear();
        classToId.clear();
    }
    
    /**
     * Gets all registered message IDs.
     */
    public java.util.Set<Integer> getRegisteredIds() {
        return messageTypes.keySet();
    }
    
    /**
     * Gets all registered message classes.
     */
    public java.util.Set<Class<?>> getRegisteredClasses() {
        return classToId.keySet();
    }
}