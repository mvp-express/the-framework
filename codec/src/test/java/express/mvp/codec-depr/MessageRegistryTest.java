package express.mvp.codec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for MessageRegistry functionality.
 */
public class MessageRegistryTest {
    
    private MessageRegistry registry;
    
    // Test message classes
    public static class TestMessage1 {
        private final String content;
        public TestMessage1(String content) { this.content = content; }
        public String getContent() { return content; }
    }
    
    public static class TestMessage2 {
        private final int value;
        public TestMessage2(int value) { this.value = value; }
        public int getValue() { return value; }
    }
    
    public record TestRecord(String name, int age) {}
    
    @BeforeEach
    void setUp() {
        registry = new MessageRegistry();
    }
    
    @Test
    void testBasicRegistration() {
        registry.registerMessage(1, TestMessage1.class);
        
        assertTrue(registry.isRegistered(1));
        assertTrue(registry.isRegistered(TestMessage1.class));
        assertEquals(1, registry.size());
        
        MessageRegistry.MessageTypeInfo info = registry.getMessageType(1);
        assertNotNull(info);
        assertEquals(1, info.getMessageId());
        assertEquals(TestMessage1.class, info.getMessageClass());
        assertEquals("TestMessage1", info.getMessageName());
    }
    
    @Test
    void testRegistrationWithCustomName() {
        registry.registerMessage(1, TestMessage1.class, "CustomTestMessage");
        
        MessageRegistry.MessageTypeInfo info = registry.getMessageType(1);
        assertNotNull(info);
        assertEquals("CustomTestMessage", info.getMessageName());
    }
    
    @Test
    void testMultipleRegistrations() {
        registry.registerMessage(1, TestMessage1.class);
        registry.registerMessage(2, TestMessage2.class);
        registry.registerMessage(3, TestRecord.class);
        
        assertEquals(3, registry.size());
        
        assertTrue(registry.isRegistered(1));
        assertTrue(registry.isRegistered(2));
        assertTrue(registry.isRegistered(3));
        
        assertTrue(registry.isRegistered(TestMessage1.class));
        assertTrue(registry.isRegistered(TestMessage2.class));
        assertTrue(registry.isRegistered(TestRecord.class));
    }
    
    @Test
    void testGetMessageIdByClass() {
        registry.registerMessage(42, TestMessage1.class);
        
        Integer messageId = registry.getMessageId(TestMessage1.class);
        assertNotNull(messageId);
        assertEquals(42, messageId.intValue());
        
        // Test unregistered class
        assertNull(registry.getMessageId(TestMessage2.class));
    }
    
    @Test
    void testGetMessageIdByInstance() {
        registry.registerMessage(100, TestMessage1.class);
        
        TestMessage1 instance = new TestMessage1("test");
        Integer messageId = registry.getMessageId(instance);
        assertNotNull(messageId);
        assertEquals(100, messageId.intValue());
        
        // Test unregistered instance
        TestMessage2 unregisteredInstance = new TestMessage2(42);
        assertNull(registry.getMessageId(unregisteredInstance));
    }
    
    @Test
    void testGetMessageTypeByUnregisteredId() {
        assertNull(registry.getMessageType(999));
    }
    
    @Test
    void testDuplicateIdRegistration() {
        registry.registerMessage(1, TestMessage1.class);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> registry.registerMessage(1, TestMessage2.class)
        );
        
        assertTrue(exception.getMessage().contains("Message ID 1 is already registered"));
    }
    
    @Test
    void testDuplicateClassRegistration() {
        registry.registerMessage(1, TestMessage1.class);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> registry.registerMessage(2, TestMessage1.class)
        );
        
        assertTrue(exception.getMessage().contains("Message class " + TestMessage1.class.getName() + " is already registered"));
    }
    
    @Test
    void testIsRegisteredMethods() {
        assertFalse(registry.isRegistered(1));
        assertFalse(registry.isRegistered(TestMessage1.class));
        
        registry.registerMessage(1, TestMessage1.class);
        
        assertTrue(registry.isRegistered(1));
        assertTrue(registry.isRegistered(TestMessage1.class));
        assertFalse(registry.isRegistered(2));
        assertFalse(registry.isRegistered(TestMessage2.class));
    }
    
    @Test
    void testClear() {
        registry.registerMessage(1, TestMessage1.class);
        registry.registerMessage(2, TestMessage2.class);
        
        assertEquals(2, registry.size());
        assertTrue(registry.isRegistered(1));
        assertTrue(registry.isRegistered(TestMessage1.class));
        
        registry.clear();
        
        assertEquals(0, registry.size());
        assertFalse(registry.isRegistered(1));
        assertFalse(registry.isRegistered(TestMessage1.class));
        assertNull(registry.getMessageType(1));
        assertNull(registry.getMessageId(TestMessage1.class));
    }
    
    @Test
    void testGetRegisteredIds() {
        registry.registerMessage(10, TestMessage1.class);
        registry.registerMessage(20, TestMessage2.class);
        registry.registerMessage(30, TestRecord.class);
        
        Set<Integer> registeredIds = registry.getRegisteredIds();
        assertEquals(3, registeredIds.size());
        assertTrue(registeredIds.contains(10));
        assertTrue(registeredIds.contains(20));
        assertTrue(registeredIds.contains(30));
    }
    
    @Test
    void testGetRegisteredClasses() {
        registry.registerMessage(10, TestMessage1.class);
        registry.registerMessage(20, TestMessage2.class);
        registry.registerMessage(30, TestRecord.class);
        
        Set<Class<?>> registeredClasses = registry.getRegisteredClasses();
        assertEquals(3, registeredClasses.size());
        assertTrue(registeredClasses.contains(TestMessage1.class));
        assertTrue(registeredClasses.contains(TestMessage2.class));
        assertTrue(registeredClasses.contains(TestRecord.class));
    }
    
    @Test
    void testEmptyRegistry() {
        assertEquals(0, registry.size());
        assertTrue(registry.getRegisteredIds().isEmpty());
        assertTrue(registry.getRegisteredClasses().isEmpty());
        assertNull(registry.getMessageType(1));
        assertNull(registry.getMessageId(TestMessage1.class));
        assertFalse(registry.isRegistered(1));
        assertFalse(registry.isRegistered(TestMessage1.class));
    }
    
    @Test
    void testMessageTypeInfoProperties() {
        registry.registerMessage(42, TestMessage1.class, "CustomName");
        
        MessageRegistry.MessageTypeInfo info = registry.getMessageType(42);
        assertNotNull(info);
        
        assertEquals(42, info.getMessageId());
        assertEquals(TestMessage1.class, info.getMessageClass());
        assertEquals("CustomName", info.getMessageName());
    }
    
    @Test
    void testThreadSafety() throws InterruptedException {
        int numThreads = 10;
        int messagesPerThread = 100;
        Thread[] threads = new Thread[numThreads];
        
        // Each thread registers different message IDs
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < messagesPerThread; i++) {
                    int messageId = threadId * messagesPerThread + i;
                    // Use different classes for different threads to avoid conflicts
                    Class<?> messageClass = (threadId % 2 == 0) ? TestMessage1.class : TestMessage2.class;
                    
                    try {
                        if (threadId % 2 == 0) {
                            registry.registerMessage(messageId, TestMessage1.class, "Thread" + threadId + "Message" + i);
                        } else {
                            registry.registerMessage(messageId, TestMessage2.class, "Thread" + threadId + "Message" + i);
                        }
                    } catch (IllegalArgumentException e) {
                        // Expected for duplicate class registrations - ignore
                    }
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify that registry is in a consistent state
        assertTrue(registry.size() > 0);
        assertTrue(registry.isRegistered(TestMessage1.class) || registry.isRegistered(TestMessage2.class));
        
        // Verify that all registered IDs can be looked up
        for (Integer id : registry.getRegisteredIds()) {
            assertNotNull(registry.getMessageType(id));
        }
        
        // Verify that all registered classes can be looked up
        for (Class<?> clazz : registry.getRegisteredClasses()) {
            assertNotNull(registry.getMessageId(clazz));
        }
    }
    
    @Test
    void testNegativeMessageId() {
        // Should allow negative message IDs
        registry.registerMessage(-1, TestMessage1.class);
        
        assertTrue(registry.isRegistered(-1));
        MessageRegistry.MessageTypeInfo info = registry.getMessageType(-1);
        assertNotNull(info);
        assertEquals(-1, info.getMessageId());
    }
    
    @Test
    void testZeroMessageId() {
        // Should allow zero as message ID
        registry.registerMessage(0, TestMessage1.class);
        
        assertTrue(registry.isRegistered(0));
        MessageRegistry.MessageTypeInfo info = registry.getMessageType(0);
        assertNotNull(info);
        assertEquals(0, info.getMessageId());
    }
}