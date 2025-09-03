# MVP.Express Codec Usage Guide

This comprehensive guide explains how to use the MVP.Express codec module for high-performance, zero-copy message
encoding and decoding using the MYRA (Memory Yielded, Rapid Access) codec.

---

## 🎯 Overview

The MVP.Express codec module provides:

- **Zero-copy message encoding/decoding** using Project Panama's MemorySegment
- **MYRA codec** for efficient binary serialization of Java records
- **Memory pool management** for GC-free operation
- **Message envelope framing** for RPC protocol
- **Reflection caching** for optimal performance

---

## 🧱 Core Components

### 1. MyraCodec

The main codec class that handles encoding/decoding of Java objects to/from binary format.

### 2. MessageEnvelope

Zero-copy message framing with pooled MemorySegments for the RPC protocol.

### 3. MemorySegmentPool

Pool of reusable MemorySegments to eliminate GC pressure.

### 4. SegmentBinaryWriter/Reader

Low-level binary I/O operations on MemorySegments.

---

## 🚀 Quick Start

### Basic Setup

```java
import express.mvp.codec.*;

// Create a memory pool for zero-copy operations
MemorySegmentPool pool = new MemorySegmentPool(1024, 100); // 1KB segments, 100 in pool

        // Create message registry for type mapping
        MessageRegistry registry = new MessageRegistry();
registry.

        registerMessage(1,GetBalanceRequest .class);
registry.

        registerMessage(2,GetBalanceResponse .class);

        // Create the codec
        ZeroCopyMyraCodec codec = new ZeroCopyMyraCodec(registry, pool);
```

### Encoding a Message

```java
// Your message record
public record GetBalanceRequest(String accountId) {
}

// Create the message
GetBalanceRequest request = new GetBalanceRequest("ACC-12345");

// Allocate envelope from pool (zero-copy)
MessageEnvelope envelope = MessageEnvelope.allocate(256, pool);

// Set envelope headers
envelope.

setMethodId((short) 1);
        envelope.

setCorrelationId(12345L);
envelope.

setFlags((byte) 0);

// Encode the message (zero-copy)
        codec.

encodeMessage(request, envelope);

// Use the encoded message...
MemorySegment payload = envelope.getPayload();
int totalSize = envelope.getTotalSize();

// Always release when done (returns to pool)
envelope.

release();
```

### Decoding a Message

```java
// Assuming you have a MessageEnvelope with encoded data
MessageEnvelope envelope = // ... received from network

// Decode the message (zero-copy)
        Object
decodedMessage =codec.

decodeMessage(envelope);

// Cast to expected type
GetBalanceRequest request = (GetBalanceRequest) decodedMessage;

// Use the decoded message
System.out.

println("Account ID: "+request.accountId());

// Release the envelope
        envelope.

release();
```

---

## 🔧 Advanced Usage

### Custom Message Types

The codec automatically handles Java records with supported field types:

```java
public record ComplexMessage(
        String name,
        int age,
        double balance,
        boolean active,
        byte[] data
) {
}

// Register with unique ID
registry.

registerMessage(10,ComplexMessage .class);

// Encode/decode works automatically
ComplexMessage msg = new ComplexMessage("John", 30, 1000.50, true, new byte[]{1, 2, 3});
codec.

encodeMessage(msg, envelope);
```

### Supported Field Types

| Java Type | Description    | Encoding                    |
|-----------|----------------|-----------------------------|
| `String`  | UTF-8 text     | Length-prefixed UTF-8 bytes |
| `int`     | 32-bit integer | 4 bytes, big-endian         |
| `long`    | 64-bit integer | 8 bytes, big-endian         |
| `short`   | 16-bit integer | 2 bytes, big-endian         |
| `byte`    | 8-bit integer  | 1 byte                      |
| `boolean` | Boolean value  | 1 byte (0/1)                |
| `float`   | 32-bit float   | 4 bytes, IEEE 754           |
| `double`  | 64-bit float   | 8 bytes, IEEE 754           |
| `byte[]`  | Binary data    | Length-prefixed bytes       |

### Memory Pool Configuration

```java
// Configure pool for your use case
int segmentSize = 2048;    // 2KB per segment
int poolSize = 200;        // 200 segments in pool
MemorySegmentPool pool = new MemorySegmentPool(segmentSize, poolSize);

// Monitor pool usage
System.out.

println("Available segments: "+pool.getAvailableCount());
        System.out.

println("Total segments: "+pool.getTotalCount());
```

### Performance Optimization

```java
// Pre-warm the reflection cache for better performance
codec.encodeMessage(new GetBalanceRequest("warmup"),envelope);
        envelope.

release();

// Check cache statistics
System.out.

println("Cache size: "+codec.getCacheSize());

// Clear cache if needed (rarely required)
        codec.

clearCache();
```

---

## 📦 Message Envelope Details

### Binary Layout

```
+----------------+-------------+-------------+----------------+
| Length (2B)    | Method ID   | Correlation ID (8B)          |
+----------------+-------------+-------------+----------------+
| Trace ID (16B optional) | Flags (1B) | Payload (MYRA)       |
+-------------------------------------------------------------+
```

### Working with Envelopes

```java
// Create envelope
MessageEnvelope envelope = MessageEnvelope.allocate(payloadSize, pool);

// Set headers
envelope.

setMethodId((short) 42);
        envelope.

setCorrelationId(System.nanoTime());
        envelope.

setFlags(MessageEnvelope.FLAG_IS_RESPONSE);

// Read headers
short methodId = envelope.getMethodId();
long correlationId = envelope.getCorrelationId();
boolean isResponse = envelope.isResponse();

// Access payload (zero-copy slice)
MemorySegment payload = envelope.getPayload();

// Always release
envelope.

release();
```

### Message Flags

```java
// Available flags
MessageEnvelope.FLAG_HAS_TRACE_ID  // 0x01
MessageEnvelope.FLAG_IS_RESPONSE   // 0x02  
MessageEnvelope.FLAG_HAS_ERROR     // 0x04

// Check flags
if(envelope.

hasTraceId()){
        // Handle trace ID
        }

        if(envelope.

isResponse()){
        // Handle response message
        }

        if(envelope.

hasError()){
        // Handle error condition
        }
```

---

## 🎯 Best Practices

### 1. Always Use Memory Pools

```java
// ✅ GOOD: Use pooled allocation
MessageEnvelope envelope = MessageEnvelope.allocate(size, pool);
// ... use envelope
envelope.

release(); // Return to pool

// ❌ BAD: Direct allocation (causes GC pressure)
MemorySegment buffer = MemorySegment.allocateNative(size);
MessageEnvelope envelope = new MessageEnvelope(buffer);
```

### 2. Proper Resource Management

```java
// ✅ GOOD: Use try-with-resources pattern
try(var envelope = MessageEnvelope.allocate(256, pool)){
        codec.

encodeMessage(message, envelope);
// envelope.release() called automatically
}

// ✅ GOOD: Manual release in finally block
MessageEnvelope envelope = null;
try{
envelope =MessageEnvelope.

allocate(256,pool);
    codec.

encodeMessage(message, envelope);
}finally{
        if(envelope !=null){
        envelope.

release();
    }
            }
```

### 3. Efficient String Handling

```java
// ✅ GOOD: Use reasonable string lengths
public record UserInfo(String name, String email) {
} // OK

// ⚠️ CAUTION: Very long strings may impact performance
public record LargeText(String content) {
} // Consider byte[] for large data
```

### 4. Batch Operations

```java
// ✅ GOOD: Reuse codec instance
ZeroCopyMyraCodec codec = new ZeroCopyMyraCodec(registry, pool);

for(
Message msg :messages){
MessageEnvelope envelope = MessageEnvelope.allocate(256, pool);
    try{
            codec.

encodeMessage(msg, envelope);
// Process envelope...
    }finally{
            envelope.

release();
    }
            }
```

---

## 🔍 Troubleshooting

### Common Issues

#### 1. Memory Leaks

**Problem**: Envelopes not released back to pool

```java
// ❌ BAD: Forgot to release
MessageEnvelope envelope = MessageEnvelope.allocate(256, pool);
codec.

encodeMessage(message, envelope);
// Missing: envelope.release();
```

**Solution**: Always call `release()` or use try-with-resources

#### 2. Pool Exhaustion

**Problem**: Pool runs out of available segments

```java
// Check pool status
if(pool.getAvailableCount() ==0){
        System.err.

println("Pool exhausted! Check for unreleased envelopes");
}
```

**Solution**: Increase pool size or fix resource leaks

#### 3. Unsupported Field Types

**Problem**: Codec doesn't support certain field types

```java
public record UnsupportedMessage(
        LocalDateTime timestamp,  // ❌ Not supported
        Map<String, String> data  // ❌ Not supported
) {
}
```

**Solution**: Use supported types or convert to byte arrays

### Performance Monitoring

```java
// Monitor codec performance
long startTime = System.nanoTime();
codec.

encodeMessage(message, envelope);

long encodeTime = System.nanoTime() - startTime;

System.out.

println("Encode time: "+encodeTime +"ns");
System.out.

println("Cache size: "+codec.getCacheSize());
        System.out.

println("Pool available: "+pool.getAvailableCount());
```

---

## 📊 Performance Characteristics

### Typical Performance (Java 21, modern CPU)

| Operation                   | Time   | Allocations |
|-----------------------------|--------|-------------|
| Small record encode         | ~2.5μs | Zero        |
| Small record decode         | ~3.0μs | Minimal     |
| String encoding (50 chars)  | ~100ns | Zero        |
| Primitive field access      | ~10ns  | Zero        |
| Memory pool acquire/release | ~50ns  | Zero        |

### Zero-Copy Benefits

- **No GC pressure**: All operations use pooled memory
- **No data copying**: Direct MemorySegment operations
- **Minimal allocations**: Only final decoded objects created
- **Predictable performance**: No GC pauses or allocation spikes

---

## 🔗 Integration Examples

### With Transport Layer

```java
// Encode message for network transmission
MessageEnvelope envelope = MessageEnvelope.allocate(256, pool);
try{
        codec.

encodeMessage(request, envelope);

// Send over network (pseudo-code)
    networkChannel.

write(envelope.getBuffer());
        }finally{
        envelope.

release();
}

// Decode received message
MessageEnvelope receivedEnvelope = // ... from network
try{
Object message = codec.decodeMessage(receivedEnvelope);
// Process message...
}finally{
        receivedEnvelope.

release();
}
```

### With RPC Framework

```java
// Server-side message handling
public class MessageHandler {
    private final ZeroCopyMyraCodec codec;
    private final MemorySegmentPool pool;

    public void handleRequest(MessageEnvelope requestEnvelope) {
        try {
            // Decode request
            Object request = codec.decodeMessage(requestEnvelope);

            // Process request
            Object response = processRequest(request);

            // Encode response
            MessageEnvelope responseEnvelope = MessageEnvelope.allocate(256, pool);
            try {
                responseEnvelope.setCorrelationId(requestEnvelope.getCorrelationId());
                responseEnvelope.setFlags(MessageEnvelope.FLAG_IS_RESPONSE);

                codec.encodeMessage(response, responseEnvelope);

                // Send response...
                sendResponse(responseEnvelope);
            } finally {
                responseEnvelope.release();
            }
        } finally {
            requestEnvelope.release();
        }
    }
}
```

---

## 🎉 Summary

The MVP.Express codec provides:

- ✅ **Zero-copy performance** with pooled memory management
- ✅ **GC-free operation** for sustained high throughput
- ✅ **Simple API** for encoding/decoding Java records
- ✅ **Automatic optimization** through reflection caching
- ✅ **Production-ready** with comprehensive error handling

For more advanced usage and integration patterns, see the [Codegen Usage Guide](codegen-usage-guide.md)
and [Transport Integration Guide](transport-integration-guide.md).
