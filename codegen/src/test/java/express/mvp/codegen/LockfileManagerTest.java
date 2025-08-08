package express.mvp.codegen;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for LockfileManager functionality.
 */
public class LockfileManagerTest {
    
    @TempDir
    Path tempDir;
    
    private Path lockfilePath;
    
    @BeforeEach
    void setUp() {
        lockfilePath = tempDir.resolve("test.lock");
    }
    
    @Test
    void testDefaultLockfileConstant() {
        assertEquals(".mvpe.ids.lock", LockfileManager.DEFAULT_LOCKFILE);
    }
    
    @Test
    void testLoadNonExistentFile() throws IOException {
        Path nonExistentPath = tempDir.resolve("nonexistent.lock");
        
        IdsLock lock = LockfileManager.load(nonExistentPath);
        
        assertNotNull(lock);
        assertEquals(1, lock.version); // Default version
        assertTrue(lock.services.isEmpty());
        assertTrue(lock.methods.isEmpty());
        assertTrue(lock.messages.isEmpty());
        assertTrue(lock.tombstoneServices.isEmpty());
        assertTrue(lock.tombstoneMessages.isEmpty());
        assertTrue(lock.tombstoneMethods.isEmpty());
        assertTrue(lock.aliasServices.isEmpty());
        assertTrue(lock.aliasMessages.isEmpty());
    }
    
    @Test
    void testLoadNullPath() throws IOException {
        IdsLock lock = LockfileManager.load(null);
        
        assertNotNull(lock);
        assertEquals(1, lock.version);
        assertTrue(lock.services.isEmpty());
    }
    
    @Test
    void testSaveAndLoadBasicData() throws IOException {
        // Create a lock with basic data
        IdsLock originalLock = new IdsLock();
        originalLock.version = 2;
        originalLock.services.put("TestService", 100);
        originalLock.methods.put("TestService.testMethod", 50);
        originalLock.messages.put("TestMessage", 200);
        
        // Save to file
        LockfileManager.save(originalLock, lockfilePath);
        assertTrue(Files.exists(lockfilePath));
        
        // Load from file
        IdsLock loadedLock = LockfileManager.load(lockfilePath);
        
        assertEquals(2, loadedLock.version);
        assertEquals(100, loadedLock.services.get("TestService"));
        assertEquals(50, loadedLock.methods.get("TestService.testMethod"));
        assertEquals(200, loadedLock.messages.get("TestMessage"));
    }
    
    @Test
    void testSaveAndLoadTombstones() throws IOException {
        IdsLock originalLock = new IdsLock();
        originalLock.tombstoneServices.add(10);
        originalLock.tombstoneServices.add(20);
        originalLock.tombstoneMessages.add(100);
        originalLock.tombstoneMessages.add(200);
        
        // Add method tombstones for a service
        Set<Integer> methodTombstones = originalLock.tombstonesForService("TestService");
        methodTombstones.add(5);
        methodTombstones.add(15);
        
        LockfileManager.save(originalLock, lockfilePath);
        IdsLock loadedLock = LockfileManager.load(lockfilePath);
        
        assertTrue(loadedLock.tombstoneServices.contains(10));
        assertTrue(loadedLock.tombstoneServices.contains(20));
        assertTrue(loadedLock.tombstoneMessages.contains(100));
        assertTrue(loadedLock.tombstoneMessages.contains(200));
        
        Set<Integer> loadedMethodTombstones = loadedLock.tombstonesForService("TestService");
        assertTrue(loadedMethodTombstones.contains(5));
        assertTrue(loadedMethodTombstones.contains(15));
    }
    
    @Test
    void testSaveAndLoadAliases() throws IOException {
        IdsLock originalLock = new IdsLock();
        originalLock.aliasServices.put("OldServiceName", "NewServiceName");
        originalLock.aliasMessages.put("OldMessageName", "NewMessageName");
        
        LockfileManager.save(originalLock, lockfilePath);
        IdsLock loadedLock = LockfileManager.load(lockfilePath);
        
        assertEquals("NewServiceName", loadedLock.aliasServices.get("OldServiceName"));
        assertEquals("NewMessageName", loadedLock.aliasMessages.get("OldMessageName"));
    }
    
    @Test
    void testEscapingSpecialCharacters() throws IOException {
        IdsLock originalLock = new IdsLock();
        
        // Names with dots and percent signs
        String serviceName = "com.example.Service%Test";
        String methodName = "com.example.Service%Test.method.with.dots";
        String messageName = "com.example.Message%Test";
        
        originalLock.services.put(serviceName, 100);
        originalLock.methods.put(methodName, 50);
        originalLock.messages.put(messageName, 200);
        
        LockfileManager.save(originalLock, lockfilePath);
        IdsLock loadedLock = LockfileManager.load(lockfilePath);
        
        assertEquals(100, loadedLock.services.get(serviceName));
        assertEquals(50, loadedLock.methods.get(methodName));
        assertEquals(200, loadedLock.messages.get(messageName));
    }
    
    @Test
    void testEmptyTombstoneHandling() throws IOException {
        IdsLock originalLock = new IdsLock();
        // Leave tombstones empty
        
        LockfileManager.save(originalLock, lockfilePath);
        IdsLock loadedLock = LockfileManager.load(lockfilePath);
        
        assertTrue(loadedLock.tombstoneServices.isEmpty());
        assertTrue(loadedLock.tombstoneMessages.isEmpty());
        assertTrue(loadedLock.tombstoneMethods.isEmpty());
    }
    
    @Test
    void testComplexLockfileStructure() throws IOException {
        IdsLock originalLock = new IdsLock();
        originalLock.version = 3;
        
        // Multiple services
        originalLock.services.put("AccountService", 42);
        originalLock.services.put("PaymentService", 43);
        originalLock.services.put("UserService", 44);
        
        // Multiple methods
        originalLock.methods.put("AccountService.GetBalance", 1);
        originalLock.methods.put("AccountService.TransferFunds", 2);
        originalLock.methods.put("PaymentService.ProcessPayment", 1);
        originalLock.methods.put("UserService.GetUser", 1);
        
        // Multiple messages
        originalLock.messages.put("GetBalanceRequest", 101);
        originalLock.messages.put("GetBalanceResponse", 102);
        originalLock.messages.put("TransferFundsRequest", 103);
        originalLock.messages.put("TransferFundsResponse", 104);
        
        // Tombstones
        originalLock.tombstoneServices.add(40);
        originalLock.tombstoneServices.add(41);
        originalLock.tombstoneMessages.add(99);
        originalLock.tombstoneMessages.add(100);
        
        // Method tombstones for different services
        originalLock.tombstonesForService("AccountService").add(10);
        originalLock.tombstonesForService("PaymentService").add(15);
        
        // Aliases
        originalLock.aliasServices.put("Accounts", "AccountService");
        originalLock.aliasMessages.put("BalanceReq", "GetBalanceRequest");
        
        LockfileManager.save(originalLock, lockfilePath);
        IdsLock loadedLock = LockfileManager.load(lockfilePath);
        
        // Verify all data is preserved
        assertEquals(3, loadedLock.version);
        
        assertEquals(42, loadedLock.services.get("AccountService"));
        assertEquals(43, loadedLock.services.get("PaymentService"));
        assertEquals(44, loadedLock.services.get("UserService"));
        
        assertEquals(1, loadedLock.methods.get("AccountService.GetBalance"));
        assertEquals(2, loadedLock.methods.get("AccountService.TransferFunds"));
        assertEquals(1, loadedLock.methods.get("PaymentService.ProcessPayment"));
        assertEquals(1, loadedLock.methods.get("UserService.GetUser"));
        
        assertEquals(101, loadedLock.messages.get("GetBalanceRequest"));
        assertEquals(102, loadedLock.messages.get("GetBalanceResponse"));
        assertEquals(103, loadedLock.messages.get("TransferFundsRequest"));
        assertEquals(104, loadedLock.messages.get("TransferFundsResponse"));
        
        assertTrue(loadedLock.tombstoneServices.contains(40));
        assertTrue(loadedLock.tombstoneServices.contains(41));
        assertTrue(loadedLock.tombstoneMessages.contains(99));
        assertTrue(loadedLock.tombstoneMessages.contains(100));
        
        assertTrue(loadedLock.tombstonesForService("AccountService").contains(10));
        assertTrue(loadedLock.tombstonesForService("PaymentService").contains(15));
        
        assertEquals("AccountService", loadedLock.aliasServices.get("Accounts"));
        assertEquals("GetBalanceRequest", loadedLock.aliasMessages.get("BalanceReq"));
    }
    
    @Test
    void testSaveToNullPath() throws IOException {
        IdsLock lock = new IdsLock();
        lock.services.put("TestService", 100);
        
        // Should not throw exception when saving to null path
        assertDoesNotThrow(() -> LockfileManager.save(lock, null));
    }
    
    @Test
    void testFileContentFormat() throws IOException {
        IdsLock lock = new IdsLock();
        lock.version = 2;
        lock.services.put("TestService", 100);
        lock.methods.put("TestService.testMethod", 50);
        lock.messages.put("TestMessage", 200);
        lock.tombstoneServices.add(10);
        lock.tombstoneMessages.add(99);
        lock.aliasServices.put("OldName", "NewName");
        
        LockfileManager.save(lock, lockfilePath);
        
        String content = Files.readString(lockfilePath);
        
        // Verify file contains expected properties
        assertTrue(content.contains("version=2"));
        assertTrue(content.contains("services.TestService=100"));
        assertTrue(content.contains("methods.TestService.testMethod=50"));
        assertTrue(content.contains("messages.TestMessage=200"));
        assertTrue(content.contains("tombstones.services=10"));
        assertTrue(content.contains("tombstones.messages=99"));
        assertTrue(content.contains("aliases.services.OldName=NewName"));
    }
    
    @Test
    void testMultipleTombstonesFormatting() throws IOException {
        IdsLock lock = new IdsLock();
        lock.tombstoneServices.add(30);
        lock.tombstoneServices.add(10);
        lock.tombstoneServices.add(20); // Add in non-sorted order
        
        LockfileManager.save(lock, lockfilePath);
        
        String content = Files.readString(lockfilePath);
        
        // Should be sorted in the output
        assertTrue(content.contains("tombstones.services=10,20,30"));
    }
    
    @Test
    void testRoundTripConsistency() throws IOException {
        // Create a complex lock
        IdsLock originalLock = new IdsLock();
        originalLock.version = 5;
        originalLock.services.put("Service.With.Dots", 100);
        originalLock.services.put("Service%With%Percent", 200);
        originalLock.methods.put("Service.With.Dots.method", 10);
        originalLock.messages.put("Message%With%Special.Chars", 300);
        originalLock.tombstoneServices.add(1);
        originalLock.tombstoneServices.add(2);
        originalLock.tombstoneMessages.add(99);
        originalLock.tombstonesForService("Service.With.Dots").add(5);
        originalLock.aliasServices.put("Old.Service", "New.Service");
        originalLock.aliasMessages.put("Old%Message", "New%Message");
        
        // Save and load multiple times
        for (int i = 0; i < 3; i++) {
            LockfileManager.save(originalLock, lockfilePath);
            originalLock = LockfileManager.load(lockfilePath);
        }
        
        // Verify data integrity after multiple round trips
        assertEquals(5, originalLock.version);
        assertEquals(100, originalLock.services.get("Service.With.Dots"));
        assertEquals(200, originalLock.services.get("Service%With%Percent"));
        assertEquals(10, originalLock.methods.get("Service.With.Dots.method"));
        assertEquals(300, originalLock.messages.get("Message%With%Special.Chars"));
        assertTrue(originalLock.tombstoneServices.contains(1));
        assertTrue(originalLock.tombstoneServices.contains(2));
        assertTrue(originalLock.tombstoneMessages.contains(99));
        assertTrue(originalLock.tombstonesForService("Service.With.Dots").contains(5));
        assertEquals("New.Service", originalLock.aliasServices.get("Old.Service"));
        assertEquals("New%Message", originalLock.aliasMessages.get("Old%Message"));
    }
}