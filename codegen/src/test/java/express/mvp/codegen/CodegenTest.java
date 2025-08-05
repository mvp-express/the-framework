package express.mvp.codegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for MVP.Express code generation functionality.
 */
public class CodegenTest {
    
    @Test
    public void testAccountServiceCodeGeneration(@TempDir Path tempDir) throws IOException {
        // Create sample YAML content
        String yamlContent = """
            service: AccountService
            id: 42
            
            methods:
              - name: GetBalance
                id: 1
                request: GetBalanceRequest
                response: GetBalanceResponse
            
              - name: TransferFunds
                id: 2
                request: TransferFundsRequest
                response: TransferFundsResponse
            
            messages:
              - name: GetBalanceRequest
                fields:
                  - name: accountId
                    type: string
            
              - name: GetBalanceResponse
                fields:
                  - name: balance
                    type: int64
            
              - name: TransferFundsRequest
                fields:
                  - name: fromAccountId
                    type: string
                  - name: toAccountId
                    type: string
                  - name: amount
                    type: int64
            
              - name: TransferFundsResponse
                fields:
                  - name: success
                    type: boolean
                  - name: txnId
                    type: string
            """;
        
        // Create orchestrator
        CodegenOrchestrator orchestrator = new CodegenOrchestrator("express.mvp.generated");
        
        // Generate code
        orchestrator.generateFromString(yamlContent, tempDir);
        
        // Verify generated files exist
        Path packageDir = tempDir.resolve("express").resolve("mvp").resolve("generated");
        assertTrue(Files.exists(packageDir), "Package directory should exist");
        
        // Check service interface
        Path serviceFile = packageDir.resolve("AccountService.java");
        assertTrue(Files.exists(serviceFile), "Service interface should be generated");
        String serviceContent = Files.readString(serviceFile);
        assertTrue(serviceContent.contains("public interface AccountService"), "Should contain interface declaration");
        assertTrue(serviceContent.contains("GetBalanceResponse getBalance(GetBalanceRequest request)"), "Should contain getBalance method");
        assertTrue(serviceContent.contains("TransferFundsResponse transferFunds(TransferFundsRequest request)"), "Should contain transferFunds method");
        
        // Check DTO records
        Path requestFile = packageDir.resolve("GetBalanceRequest.java");
        assertTrue(Files.exists(requestFile), "Request DTO should be generated");
        String requestContent = Files.readString(requestFile);
        assertTrue(requestContent.contains("public record GetBalanceRequest("), "Should be a record");
        assertTrue(requestContent.contains("String accountId"), "Should contain accountId field");
        
        Path responseFile = packageDir.resolve("GetBalanceResponse.java");
        assertTrue(Files.exists(responseFile), "Response DTO should be generated");
        String responseContent = Files.readString(responseFile);
        assertTrue(responseContent.contains("public record GetBalanceResponse("), "Should be a record");
        assertTrue(responseContent.contains("long balance"), "Should contain balance field");
        
        // Check dispatcher
        Path dispatcherFile = packageDir.resolve("AccountServiceDispatcher.java");
        assertTrue(Files.exists(dispatcherFile), "Dispatcher should be generated");
        String dispatcherContent = Files.readString(dispatcherFile);
        assertTrue(dispatcherContent.contains("public class AccountServiceDispatcher"), "Should contain dispatcher class");
        assertTrue(dispatcherContent.contains("case 1 -> service.getBalance"), "Should contain method dispatch");
        assertTrue(dispatcherContent.contains("case 2 -> service.transferFunds"), "Should contain method dispatch");
        
        System.out.println("Code generation test passed successfully!");
    }
    
    @Test
    public void testSchemaValidation() {
        String invalidYaml = """
            service: ""
            id: -1
            methods: []
            """;
        
        CodegenOrchestrator orchestrator = new CodegenOrchestrator("test.package");
        
        assertThrows(IllegalArgumentException.class, () -> {
            orchestrator.generateFromString(invalidYaml, Path.of("/tmp"));
        }, "Should throw validation error for invalid schema");
    }
}