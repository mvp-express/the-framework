# MVP.Express Schema Design

This document outlines the structure and purpose of the `.mvpe.yaml` schema file used in MVP.Express to define RPC
services, methods, and binary message schemas — inspired by Protocol Buffers and gRPC, but designed to work directly
with SBE (Simple Binary Encoding).

---

## 🧠 Philosophy

Unlike gRPC which uses Protobuf to define services and messages, MVP.Express defines a compact YAML DSL that supports:

- Service and method declaration
- Strong typing via primitive and complex types
- Message schemas for request/response
- Explicit message IDs (for routing and encoding)
- Compatibility with SBE XML schema generation

MVP.Express avoids text-based serialization altogether — focusing on **binary-first, high-performance, low-latency RPC
**.

---

## 📘 Schema Structure

### Top-level keys:

```yaml
service: AccountService         # Logical name of the service
id: 42                          # Unique numeric service ID

methods:
  - name: GetBalance
    id: 1
    request: GetBalanceRequest
    response: GetBalanceResponse

messages:
  - name: GetBalanceRequest
    fields:
      - name: accountId
        type: string

  - name: GetBalanceResponse
    fields:
      - name: balance
        type: int64
```

🧩 Field Types

| Type      | Description                 |
|-----------|-----------------------------|
| `string`  | UTF-8 encoded, fixed or var |
| `int64`   | 64-bit signed integer       |
| `int32`   | 32-bit signed integer       |
| `boolean` | 1-byte boolean              |
| `float`   | 32-bit IEEE float           |
| `double`  | 64-bit IEEE float           |
| `bytes`   | Binary blob (byte\[])       |

Support for:

- Optional fields (via nullability config)
- Repeated fields
- Enum support

## Service and Method Design

Each service defines:

- A unique numeric `service id` (used in framing)
- List of `methods`:
    - Unique `method id` (1–255 per service)
    - Input and output message names

Messages must be defined under `messages:` with each message having:

- A unique name
- List of fields with name + type

📦 Codegen Output

1. Interface (Java)
   ```java
   public interface AccountService {
    GetBalanceResponse getBalance(GetBalanceRequest req);
   }

2. DTOs
    ```java
   public record GetBalanceRequest(String accountId) {}
   public record GetBalanceResponse(long balance) {}

3. SBE XML Schema

   ```xml
   <message name="GetBalanceRequest" id="101">
   <field name="accountId" type="string" />
   </message>

4. Dispatcher/Proxy
   ```java
   // Service dispatcher and dynamic client proxy code generated

📑 DSL File Naming Convention

By default, each service schema lives in its own file like:

```sh
    account.mvpe.yaml
    user.mvpe.yaml
    inventory.mvpe.yaml
```

Or use one file with multiple services (planned support).

🧰 CLI Usage

```bash
    ./gradlew mvpeGenerate
    # Or:
    java -jar mvpe-cli.jar generate account.mvpe.yaml -o src/main/java
```

🧠 Why not Protobuf?

- SBE is significantly faster (zero copy, fixed layout)
- MVP.Express targets brokerless, high-throughput internal microservices
- We aim to minimize runtime encoding/decoding costs
- Schema-to-wire mapping is extremely predictable

🔮 Future Extensions

- options: section for field-level tuning (e.g. maxLength, nullability)
- Custom types & aliases
- enum and repeated field support
- Imports and schema composition
- Plugin hooks for codegen

✅ Summary

| Feature                | Supported |
|------------------------|-----------|
| Service + Method DSL   | ✅         |
| Request/Response types | ✅         |
| Message ID routing     | ✅         |
| Codegen (Java)         | ✅         |
| SBE schema emit        | ✅         |
| Enums                  | ✅         |
| Repeated fields        | ✅         |
| Streaming              | 🚧        |

**MVP.Express gives you gRPC-like developer ergonomics — without protobuf or brokers — running over blazing-fast binary
channels powered by SBE.**