package express.mvp.codegen;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for IdAllocator functionality.
 */
public class IdAllocatorTest {
    
    @Test
    void testServiceIdConstants() {
        assertEquals(32, IdAllocator.SERVICE_MIN);
        assertEquals(64999, IdAllocator.SERVICE_MAX);
    }
    
    @Test
    void testMethodIdConstants() {
        assertEquals(16, IdAllocator.METHOD_MIN);
        assertEquals(239, IdAllocator.METHOD_MAX);
    }
    
    @Test
    void testMessageIdConstants() {
        assertEquals(32, IdAllocator.MESSAGE_MIN);
        assertEquals(64000, IdAllocator.MESSAGE_MAX);
    }
    
    @Test
    void testAssignServiceIdNewService() {
        Map<String, Integer> existingByName = new HashMap<>();
        Set<Integer> tombstones = new HashSet<>();
        
        int serviceId = IdAllocator.assignServiceId("TestService", existingByName, tombstones);
        
        assertTrue(serviceId >= IdAllocator.SERVICE_MIN);
        assertTrue(serviceId <= IdAllocator.SERVICE_MAX);
    }
    
    @Test
    void testAssignServiceIdExistingService() {
        Map<String, Integer> existingByName = new HashMap<>();
        existingByName.put("TestService", 100);
        Set<Integer> tombstones = new HashSet<>();
        
        int serviceId = IdAllocator.assignServiceId("TestService", existingByName, tombstones);
        
        assertEquals(100, serviceId);
    }
    
    @Test
    void testAssignServiceIdDeterministic() {
        Map<String, Integer> existingByName = new HashMap<>();
        Set<Integer> tombstones = new HashSet<>();
        
        // Same service name should always get same ID
        int id1 = IdAllocator.assignServiceId("DeterministicService", existingByName, tombstones);
        existingByName.clear(); // Clear to simulate fresh allocation
        int id2 = IdAllocator.assignServiceId("DeterministicService", existingByName, tombstones);
        
        assertEquals(id1, id2);
    }
    
    @Test
    void testAssignServiceIdExplicitValid() {
        Map<String, Integer> existingByName = new HashMap<>();
        Set<Integer> tombstones = new HashSet<>();
        
        int serviceId = IdAllocator.assignServiceIdExplicit(1000, existingByName, tombstones);
        
        assertEquals(1000, serviceId);
    }
    
    @Test
    void testAssignServiceIdExplicitOutOfRange() {
        Map<String, Integer> existingByName = new HashMap<>();
        Set<Integer> tombstones = new HashSet<>();
        
        // Below minimum
        assertThrows(IllegalArgumentException.class, () -> 
            IdAllocator.assignServiceIdExplicit(10, existingByName, tombstones));
        
        // Above maximum
        assertThrows(IllegalArgumentException.class, () -> 
            IdAllocator.assignServiceIdExplicit(70000, existingByName, tombstones));
    }
    
    @Test
    void testAssignServiceIdExplicitAlreadyUsed() {
        Map<String, Integer> existingByName = new HashMap<>();
        existingByName.put("ExistingService", 1000);
        Set<Integer> tombstones = new HashSet<>();
        
        assertThrows(IllegalArgumentException.class, () -> 
            IdAllocator.assignServiceIdExplicit(1000, existingByName, tombstones));
    }
    
    @Test
    void testAssignServiceIdExplicitTombstoned() {
        Map<String, Integer> existingByName = new HashMap<>();
        Set<Integer> tombstones = new HashSet<>();
        tombstones.add(1000);
        
        assertThrows(IllegalArgumentException.class, () -> 
            IdAllocator.assignServiceIdExplicit(1000, existingByName, tombstones));
    }
    
    @Test
    void testAssignMessageIdNewMessage() {
        Map<String, Integer> existingByName = new HashMap<>();
        Set<Integer> tombstones = new HashSet<>();
        
        int messageId = IdAllocator.assignMessageId("TestMessage", existingByName, tombstones);
        
        assertTrue(messageId >= IdAllocator.MESSAGE_MIN);
        assertTrue(messageId <= IdAllocator.MESSAGE_MAX);
    }
    
    @Test
    void testAssignMessageIdExistingMessage() {
        Map<String, Integer> existingByName = new HashMap<>();
        existingByName.put("TestMessage", 500);
        Set<Integer> tombstones = new HashSet<>();
        
        int messageId = IdAllocator.assignMessageId("TestMessage", existingByName, tombstones);
        
        assertEquals(500, messageId);
    }
    
    @Test
    void testAssignMessageIdDeterministic() {
        Map<String, Integer> existingByName = new HashMap<>();
        Set<Integer> tombstones = new HashSet<>();
        
        // Same message name should always get same ID
        int id1 = IdAllocator.assignMessageId("DeterministicMessage", existingByName, tombstones);
        existingByName.clear(); // Clear to simulate fresh allocation
        int id2 = IdAllocator.assignMessageId("DeterministicMessage", existingByName, tombstones);
        
        assertEquals(id1, id2);
    }
    
    @Test
    void testAssignMessageIdExplicitValid() {
        Map<String, Integer> existingByName = new HashMap<>();
        Set<Integer> tombstones = new HashSet<>();
        
        int messageId = IdAllocator.assignMessageIdExplicit(1000, existingByName, tombstones);
        
        assertEquals(1000, messageId);
    }
    
    @Test
    void testAssignMessageIdExplicitOutOfRange() {
        Map<String, Integer> existingByName = new HashMap<>();
        Set<Integer> tombstones = new HashSet<>();
        
        // Below minimum
        assertThrows(IllegalArgumentException.class, () -> 
            IdAllocator.assignMessageIdExplicit(10, existingByName, tombstones));
        
        // Above maximum
        assertThrows(IllegalArgumentException.class, () -> 
            IdAllocator.assignMessageIdExplicit(70000, existingByName, tombstones));
    }
    
    @Test
    void testAssignMessageIdExplicitAlreadyUsed() {
        Map<String, Integer> existingByName = new HashMap<>();
        existingByName.put("ExistingMessage", 1000);
        Set<Integer> tombstones = new HashSet<>();
        
        assertThrows(IllegalArgumentException.class, () -> 
            IdAllocator.assignMessageIdExplicit(1000, existingByName, tombstones));
    }
    
    @Test
    void testAssignMessageIdExplicitTombstoned() {
        Map<String, Integer> existingByName = new HashMap<>();
        Set<Integer> tombstones = new HashSet<>();
        tombstones.add(1000);
        
        assertThrows(IllegalArgumentException.class, () -> 
            IdAllocator.assignMessageIdExplicit(1000, existingByName, tombstones));
    }
    
    @Test
    void testAssignMethodIdNewMethod() {
        Map<String, Integer> methodIdsByQualifiedName = new HashMap<>();
        Set<Integer> methodTombstones = new HashSet<>();
        
        int methodId = IdAllocator.assignMethodId("TestService", "testMethod", 
                                                  methodIdsByQualifiedName, methodTombstones);
        
        assertTrue(methodId >= IdAllocator.METHOD_MIN);
        assertTrue(methodId <= IdAllocator.METHOD_MAX);
    }
    
    @Test
    void testAssignMethodIdExistingMethod() {
        Map<String, Integer> methodIdsByQualifiedName = new HashMap<>();
        methodIdsByQualifiedName.put("TestService.testMethod", 50);
        Set<Integer> methodTombstones = new HashSet<>();
        
        int methodId = IdAllocator.assignMethodId("TestService", "testMethod", 
                                                  methodIdsByQualifiedName, methodTombstones);
        
        assertEquals(50, methodId);
    }
    
    @Test
    void testAssignMethodIdDeterministic() {
        Map<String, Integer> methodIdsByQualifiedName = new HashMap<>();
        Set<Integer> methodTombstones = new HashSet<>();
        
        // Same method should always get same ID
        int id1 = IdAllocator.assignMethodId("TestService", "deterministicMethod", 
                                             methodIdsByQualifiedName, methodTombstones);
        methodIdsByQualifiedName.clear(); // Clear to simulate fresh allocation
        int id2 = IdAllocator.assignMethodId("TestService", "deterministicMethod", 
                                             methodIdsByQualifiedName, methodTombstones);
        
        assertEquals(id1, id2);
    }
    
    @Test
    void testAssignMethodIdAvoidCollisions() {
        Map<String, Integer> methodIdsByQualifiedName = new HashMap<>();
        Set<Integer> methodTombstones = new HashSet<>();
        
        // Assign first method
        int id1 = IdAllocator.assignMethodId("TestService", "method1", 
                                             methodIdsByQualifiedName, methodTombstones);
        methodIdsByQualifiedName.put("TestService.method1", id1);
        
        // Assign second method - should get different ID
        int id2 = IdAllocator.assignMethodId("TestService", "method2", 
                                             methodIdsByQualifiedName, methodTombstones);
        
        assertNotEquals(id1, id2);
        assertTrue(id2 >= IdAllocator.METHOD_MIN);
        assertTrue(id2 <= IdAllocator.METHOD_MAX);
    }
    
    @Test
    void testAssignMethodIdDifferentServices() {
        Map<String, Integer> methodIdsByQualifiedName = new HashMap<>();
        Set<Integer> methodTombstones = new HashSet<>();
        
        // Methods in different services can have same ID
        int id1 = IdAllocator.assignMethodId("Service1", "sameMethod", 
                                             methodIdsByQualifiedName, methodTombstones);
        methodIdsByQualifiedName.put("Service1.sameMethod", id1);
        
        int id2 = IdAllocator.assignMethodId("Service2", "sameMethod", 
                                             methodIdsByQualifiedName, methodTombstones);
        
        // IDs can be the same since they're in different services
        assertTrue(id2 >= IdAllocator.METHOD_MIN);
        assertTrue(id2 <= IdAllocator.METHOD_MAX);
    }
    
    @Test
    void testAssignMethodIdExplicitValid() {
        Map<String, Integer> methodIdsByQualifiedName = new HashMap<>();
        Set<Integer> methodTombstones = new HashSet<>();
        
        int methodId = IdAllocator.assignMethodIdExplicit(100, "TestService", 
                                                          methodIdsByQualifiedName, methodTombstones);
        
        assertEquals(100, methodId);
    }
    
    @Test
    void testAssignMethodIdExplicitOutOfRange() {
        Map<String, Integer> methodIdsByQualifiedName = new HashMap<>();
        Set<Integer> methodTombstones = new HashSet<>();
        
        // Below minimum
        assertThrows(IllegalArgumentException.class, () -> 
            IdAllocator.assignMethodIdExplicit(10, "TestService", methodIdsByQualifiedName, methodTombstones));
        
        // Above maximum
        assertThrows(IllegalArgumentException.class, () -> 
            IdAllocator.assignMethodIdExplicit(300, "TestService", methodIdsByQualifiedName, methodTombstones));
    }
    
    @Test
    void testAssignMethodIdExplicitAlreadyUsed() {
        Map<String, Integer> methodIdsByQualifiedName = new HashMap<>();
        methodIdsByQualifiedName.put("TestService.existingMethod", 100);
        Set<Integer> methodTombstones = new HashSet<>();
        
        assertThrows(IllegalArgumentException.class, () -> 
            IdAllocator.assignMethodIdExplicit(100, "TestService", methodIdsByQualifiedName, methodTombstones));
    }
    
    @Test
    void testAssignMethodIdExplicitTombstoned() {
        Map<String, Integer> methodIdsByQualifiedName = new HashMap<>();
        Set<Integer> methodTombstones = new HashSet<>();
        methodTombstones.add(100);
        
        assertThrows(IllegalArgumentException.class, () -> 
            IdAllocator.assignMethodIdExplicit(100, "TestService", methodIdsByQualifiedName, methodTombstones));
    }
    
    @Test
    void testCollisionAvoidanceWithTombstones() {
        Map<String, Integer> existingByName = new HashMap<>();
        Set<Integer> tombstones = new HashSet<>();
        
        // Fill up some IDs to force collision avoidance
        for (int i = IdAllocator.SERVICE_MIN; i < IdAllocator.SERVICE_MIN + 10; i++) {
            tombstones.add(i);
        }
        
        int serviceId = IdAllocator.assignServiceId("TestService", existingByName, tombstones);
        
        assertTrue(serviceId >= IdAllocator.SERVICE_MIN + 10);
        assertTrue(serviceId <= IdAllocator.SERVICE_MAX);
        assertFalse(tombstones.contains(serviceId));
    }
    
    @Test
    void testMultipleServiceAssignments() {
        Map<String, Integer> existingByName = new HashMap<>();
        Set<Integer> tombstones = new HashSet<>();
        
        // Assign multiple services
        int id1 = IdAllocator.assignServiceId("Service1", existingByName, tombstones);
        existingByName.put("Service1", id1);
        
        int id2 = IdAllocator.assignServiceId("Service2", existingByName, tombstones);
        existingByName.put("Service2", id2);
        
        int id3 = IdAllocator.assignServiceId("Service3", existingByName, tombstones);
        existingByName.put("Service3", id3);
        
        // All IDs should be different
        assertNotEquals(id1, id2);
        assertNotEquals(id1, id3);
        assertNotEquals(id2, id3);
        
        // All should be in valid range
        assertTrue(id1 >= IdAllocator.SERVICE_MIN && id1 <= IdAllocator.SERVICE_MAX);
        assertTrue(id2 >= IdAllocator.SERVICE_MIN && id2 <= IdAllocator.SERVICE_MAX);
        assertTrue(id3 >= IdAllocator.SERVICE_MIN && id3 <= IdAllocator.SERVICE_MAX);
    }
    
    @Test
    void testMultipleMessageAssignments() {
        Map<String, Integer> existingByName = new HashMap<>();
        Set<Integer> tombstones = new HashSet<>();
        
        // Assign multiple messages
        int id1 = IdAllocator.assignMessageId("Message1", existingByName, tombstones);
        existingByName.put("Message1", id1);
        
        int id2 = IdAllocator.assignMessageId("Message2", existingByName, tombstones);
        existingByName.put("Message2", id2);
        
        int id3 = IdAllocator.assignMessageId("Message3", existingByName, tombstones);
        existingByName.put("Message3", id3);
        
        // All IDs should be different
        assertNotEquals(id1, id2);
        assertNotEquals(id1, id3);
        assertNotEquals(id2, id3);
        
        // All should be in valid range
        assertTrue(id1 >= IdAllocator.MESSAGE_MIN && id1 <= IdAllocator.MESSAGE_MAX);
        assertTrue(id2 >= IdAllocator.MESSAGE_MIN && id2 <= IdAllocator.MESSAGE_MAX);
        assertTrue(id3 >= IdAllocator.MESSAGE_MIN && id3 <= IdAllocator.MESSAGE_MAX);
    }
    
    @Test
    void testMethodAssignmentWithinService() {
        Map<String, Integer> methodIdsByQualifiedName = new HashMap<>();
        Set<Integer> methodTombstones = new HashSet<>();
        
        // Assign multiple methods within same service
        int id1 = IdAllocator.assignMethodId("TestService", "method1", 
                                             methodIdsByQualifiedName, methodTombstones);
        methodIdsByQualifiedName.put("TestService.method1", id1);
        
        int id2 = IdAllocator.assignMethodId("TestService", "method2", 
                                             methodIdsByQualifiedName, methodTombstones);
        methodIdsByQualifiedName.put("TestService.method2", id2);
        
        int id3 = IdAllocator.assignMethodId("TestService", "method3", 
                                             methodIdsByQualifiedName, methodTombstones);
        
        // All IDs should be different within the service
        assertNotEquals(id1, id2);
        assertNotEquals(id1, id3);
        assertNotEquals(id2, id3);
        
        // All should be in valid range
        assertTrue(id1 >= IdAllocator.METHOD_MIN && id1 <= IdAllocator.METHOD_MAX);
        assertTrue(id2 >= IdAllocator.METHOD_MIN && id2 <= IdAllocator.METHOD_MAX);
        assertTrue(id3 >= IdAllocator.METHOD_MIN && id3 <= IdAllocator.METHOD_MAX);
    }
    
    @Test
    void testNullTombstonesHandling() {
        Map<String, Integer> existingByName = new HashMap<>();
        
        // Should work with null tombstones
        assertDoesNotThrow(() -> {
            IdAllocator.assignServiceId("TestService", existingByName, null);
            IdAllocator.assignMessageId("TestMessage", existingByName, null);
            IdAllocator.assignServiceIdExplicit(1000, existingByName, null);
            IdAllocator.assignMessageIdExplicit(1000, existingByName, null);
        });
        
        Map<String, Integer> methodIds = new HashMap<>();
        assertDoesNotThrow(() -> {
            IdAllocator.assignMethodId("TestService", "testMethod", methodIds, null);
            IdAllocator.assignMethodIdExplicit(100, "TestService", methodIds, null);
        });
    }
}