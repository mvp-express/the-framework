# MVP.Express Codegen Usage Guide

This comprehensive guide explains how to use the MVP.Express codegen module to generate Java code from `.mvpe.yaml` schema files for high-performance RPC services.

---

## 🎯 Overview

The MVP.Express codegen module provides:

- **YAML schema parsing** for service and message definitions
- **Java code generation** for interfaces, records, and implementation stubs
- **Validation and error reporting** for schema correctness
- **Flexible output configuration** for different project structures
- **Integration with build systems** via programmatic API

---

## 🧱 Core Components

### 1. CodegenOrchestrator
Main entry point that coordinates parsing and code generation.

### 2. MvpeSchemaParser
Parses `.mvpe.yaml` files and validates schema structure.

### 3. JavaCodeGenerator
Generates Java source files from parsed schema definitions.

### 4. Schema Model Classes
- `ServiceSchema`: Represents the complete service definition
- `MethodDef`: Individual method definitions
- `MessageDef`: Message type definitions
- `FieldDef`: Field definitions within messages

---

## 🚀 Quick Start

### Basic Setup

```java
import express.mvp.codegen.*;

// Create orchestrator with base package
CodegenOrchestrator orchestrator = new CodegenOrchestrator("com.example.services");

// Generate code from schema file
Path schemaFile = Paths.get("account.mvpe.yaml");
Path outputDir = Paths.get("src/main/java");

orchestrator.generateFromFile(schemaFile, outputDir);
```

### Schema File Example

Create `account.mvpe.yaml`:

```yaml
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
        type: long
      - name: currency
        type: string
        
  - name: TransferFundsRequest
    fields:
      - name: fromAccountId
        type: string
      - name: toAccountId
        type: string
      - name: amount
        type: long
      - name: currency
        type: string
        
  - name: TransferFundsResponse
    fields:
      - name: success
        type: boolean
      - name: transactionId
        type: string
      - name: errorMessage
        type: string
```

---

## 📁 Generated Code Structure

Running codegen on the above schema generates:

```
src/main/java/com/example/services/
├── AccountService.java              # Service interface
├── GetBalanceRequest.java           # Request record
├── GetBalanceResponse.java          # Response record
├── TransferFundsRequest.java        # Request record
├── TransferFundsResponse.java       # Response record
├── AccountServiceDispatcher.java    # Server-side dispatcher
└── AccountServiceClient.java        # Client-side stub
```

### Generated Service Interface

```java
package com.example.services;

/**
 * Generated service interface for AccountService
 * Service ID: 42
 */
public interface AccountService {
    
    /**
     * GetBalance method
     * Method ID: 1
     */
    GetBalanceResponse getBalance(GetBalanceRequest request);
    
    /**
     * TransferFunds method  
     * Method ID: 2
     */
    TransferFundsResponse transferFunds(TransferFundsRequest request);
}
```

### Generated Record Classes

```java
package com.example.services;

/**
 * Generated record for GetBalanceRequest
 */
public record GetBalanceRequest(
    String accountId
) {}

/**
 * Generated record for GetBalanceResponse
 */
public record GetBalanceResponse(
    long balance,
    String currency
) {}
```

---

## 🔧 Advanced Usage

### Programmatic Generation

```java
// Generate from YAML string
String yamlContent = """
    service: UserService
    id: 10
    methods:
      - name: CreateUser
        id: 1
        request: CreateUserRequest
        response: CreateUserResponse
    messages:
      - name: CreateUserRequest
        fields:
          - name: username
            type: string
          - name: email
            type: string
      - name: CreateUserResponse
        fields:
          - name: userId
            type: long
          - name: success
            type: boolean
    """;

orchestrator.generateFromString(yamlContent, outputDir);
```

### Schema Validation Only

```java
// Validate schema without generating code
try {
    boolean isValid = orchestrator.validateSchema(schemaFile);
    System.out.println("Schema is valid: " + isValid);
} catch (IllegalArgumentException e) {
    System.err.println("Schema validation failed: " + e.getMessage());
}
```

### Schema Information Inspection

```java
// Print detailed schema information
orchestrator.printSchemaInfo(schemaFile);

// Output:
// === Schema Information ===
// Service: AccountService
// ID: 42
// Methods: 2
//   - GetBalance (ID: 1)
//     Request: GetBalanceRequest
//     Response: GetBalanceResponse
//   - TransferFunds (ID: 2)
//     Request: TransferFundsRequest
//     Response: TransferFundsResponse
// Messages: 4
//   - GetBalanceRequest
//     * accountId: string
//   - GetBalanceResponse
//     * balance: long
//     * currency: string
```

---

## 📋 Schema Definition Guide

### Service Definition

```yaml
service: MyService        # Required: Service name (PascalCase recommended)
id: 123                   # Required: Unique numeric service ID (1-65535)
```

### Method Definition

```yaml
methods:
  - name: MethodName      # Required: Method name (PascalCase recommended)
    id: 1                 # Required: Unique method ID within service (1-255)
    request: RequestType  # Required: Request message type name
    response: ResponseType # Required: Response message type name
```

### Message Definition

```yaml
messages:
  - name: MessageName     # Required: Message name (PascalCase recommended)
    fields:               # Required: List of fields
      - name: fieldName   # Required: Field name (camelCase recommended)
        type: string      # Required: Field type (see supported types)
```

### Supported Field Types

| Type | Java Equivalent | Description |
|------|----------------|-------------|
| `string` | `String` | UTF-8 encoded text |
| `int` | `int` | 32-bit signed integer |
| `long` | `long` | 64-bit signed integer |
| `short` | `short` | 16-bit signed integer |
| `byte` | `byte` | 8-bit signed integer |
| `boolean` | `boolean` | Boolean value |
| `float` | `float` | 32-bit IEEE 754 float |
| `double` | `double` | 64-bit IEEE 754 float |
| `bytes` | `byte[]` | Binary data array |

---

## 🎯 Best Practices

### 1. Schema Organization

```yaml
# ✅ GOOD: Clear, descriptive names
service: AccountManagementService
id: 100

methods:
  - name: GetAccountBalance
    id: 1
    request: GetAccountBalanceRequest
    response: GetAccountBalanceResponse

# ❌ BAD: Unclear, abbreviated names
service: AccSvc
id: 1
methods:
  - name: GetBal
    id: 1
    request: GetBalReq
    response: GetBalResp
```

### 2. ID Management

```yaml
# ✅ GOOD: Use meaningful, spaced ID ranges
service: UserService
id: 1000  # User services: 1000-1099

service: AccountService  
id: 2000  # Account services: 2000-2099

service: PaymentService
id: 3000  # Payment services: 3000-3099
```

### 3. Message Design

```yaml
# ✅ GOOD: Include all necessary fields
messages:
  - name: CreateUserRequest
    fields:
      - name: username
        type: string
      - name: email
        type: string
      - name: fullName
        type: string
      - name: isActive
        type: boolean

  - name: CreateUserResponse
    fields:
      - name: userId
        type: long
      - name: success
        type: boolean
      - name: errorMessage
        type: string  # For error details
```

### 4. File Organization

```
schemas/
├── user.mvpe.yaml          # User-related services
├── account.mvpe.yaml       # Account-related services
├── payment.mvpe.yaml       # Payment-related services
└── notification.mvpe.yaml  # Notification services
```

---

## 🔧 Build System Integration

### Gradle Integration

Add to your `build.gradle.kts`:

```kotlin
tasks.register("generateRpcCode") {
    group = "codegen"
    description = "Generate RPC code from schema files"
    
    doLast {
        val orchestrator = express.mvp.codegen.CodegenOrchestrator("com.example.generated")
        
        fileTree("src/main/schemas") {
            include("**/*.mvpe.yaml")
        }.forEach { schemaFile ->
            orchestrator.generateFromFile(
                schemaFile.toPath(),
                file("src/main/java").toPath()
            )
        }
    }
}

// Run before compilation
tasks.compileJava {
    dependsOn("generateRpcCode")
}
```

### Maven Integration

Add to your `pom.xml`:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.1.0</version>
    <executions>
        <execution>
            <id>generate-rpc-code</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>java</goal>
            </goals>
            <configuration>
                <mainClass>express.mvp.codegen.cli.CodegenCli</mainClass>
                <arguments>
                    <argument>generate</argument>
                    <argument>src/main/schemas/account.mvpe.yaml</argument>
                    <argument>--output</argument>
                    <argument>src/main/java</argument>
                    <argument>--package</argument>
                    <argument>com.example.generated</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## 🔍 Troubleshooting

### Common Schema Errors

#### 1. Duplicate IDs

```yaml
# ❌ ERROR: Duplicate method IDs
methods:
  - name: GetUser
    id: 1
    request: GetUserRequest
    response: GetUserResponse
  - name: CreateUser
    id: 1  # ERROR: ID already used
    request: CreateUserRequest
    response: CreateUserResponse
```

**Solution**: Ensure all method IDs are unique within a service.

#### 2. Missing Message Definitions

```yaml
methods:
  - name: GetUser
    id: 1
    request: GetUserRequest    # ERROR: Message not defined
    response: GetUserResponse  # ERROR: Message not defined

# Missing messages section
```

**Solution**: Define all referenced message types in the `messages` section.

#### 3. Invalid Field Types

```yaml
messages:
  - name: UserInfo
    fields:
      - name: createdAt
        type: datetime  # ERROR: Unsupported type
```

**Solution**: Use only supported field types or convert to `long` (timestamp) or `string`.

### Validation Errors

The codegen provides detailed error messages:

```
Schema validation failed: 
- Method 'GetUser' references undefined request type 'GetUserRequest'
- Method 'CreateUser' has duplicate ID 1 (already used by 'GetUser')
- Message 'UserResponse' has no fields defined
```

### Generation Issues

#### Output Directory Permissions

```java
// Check if output directory is writable
Path outputDir = Paths.get("src/main/java");
if (!Files.isWritable(outputDir)) {
    throw new IOException("Output directory is not writable: " + outputDir);
}
```

#### Package Name Validation

```java
// Ensure valid Java package name
String basePackage = "com.example.services";
if (!basePackage.matches("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$")) {
    throw new IllegalArgumentException("Invalid package name: " + basePackage);
}
```

---

## 📊 Performance Considerations

### Schema Complexity

| Schema Size | Generation Time | Memory Usage |
|-------------|----------------|--------------|
| Small (1-5 methods) | ~50ms | ~10MB |
| Medium (10-20 methods) | ~200ms | ~25MB |
| Large (50+ methods) | ~1s | ~100MB |

### Optimization Tips

1. **Split large services** into smaller, focused services
2. **Use meaningful caching** for repeated generations
3. **Generate incrementally** when possible
4. **Validate schemas early** in the build process

---

## 🔗 Integration with Runtime

### Using Generated Code

```java
// Server-side implementation
public class AccountServiceImpl implements AccountService {
    
    @Override
    public GetBalanceResponse getBalance(GetBalanceRequest request) {
        // Your business logic here
        String accountId = request.accountId();
        
        // Fetch balance from database
        long balance = fetchBalanceFromDB(accountId);
        
        return new GetBalanceResponse(balance, "USD");
    }
    
    @Override
    public TransferFundsResponse transferFunds(TransferFundsRequest request) {
        // Your transfer logic here
        boolean success = performTransfer(
            request.fromAccountId(),
            request.toAccountId(), 
            request.amount()
        );
        
        return new TransferFundsResponse(
            success,
            success ? generateTransactionId() : null,
            success ? null : "Transfer failed"
        );
    }
}
```

### Client Usage

```java
// Client-side usage (with generated client stub)
AccountServiceClient client = new AccountServiceClient(rpcClient);

// Make RPC calls
GetBalanceResponse balance = client.getBalance(
    new GetBalanceRequest("ACC-12345")
);

System.out.println("Balance: " + balance.balance() + " " + balance.currency());
```

---

## 🎉 Summary

The MVP.Express codegen provides:

- ✅ **Simple YAML schema** for defining RPC services
- ✅ **Automatic Java code generation** with proper typing
- ✅ **Comprehensive validation** with clear error messages
- ✅ **Build system integration** for automated workflows
- ✅ **Production-ready output** with documentation and best practices

For codec integration and runtime usage, see the [Codec Usage Guide](codec-usage-guide.md) and [Runtime Integration Guide](runtime-integration-guide.md).

---

## 📚 Additional Resources

- [Schema Design Best Practices](myra-schema-design.md)
- [Performance Analysis](zero-copy-analysis.md)
- [Codec Comparison](codec-comparison.md)
- [Example Projects](../examples/)