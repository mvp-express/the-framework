# mvp.express: A Modern, High-Performance RPC Framework for the JVM

**_A Technical White Paper_**

**MVP.EXPRESS - Myra Virtual Procedure on an eXpress link**

## Abstract
**MVP.EXPRESS** is a next-generation, brokerless RPC framework designed from the ground up for the modern Java ecosystem (JDK 24+). It provides a complete, "batteries-included" solution for building ultra-low latency, high-throughput microservices without relying on `sun.misc.Unsafe` or heavyweight container orchestration systems like Kubernetes. The framework's architecture is centered on a custom zero-copy binary codec (MYRA), a modular design, and a revolutionary native transport layer built on Linux's `io_uring` subsystem, accessed safely via Java's Foreign Function & Memory (FFM) API. This paper details the technical architecture, core components, and the advanced service-to-service communication model that enables **MVP.EXPRESS** to deliver elite performance with a superior developer experience.

1. Introduction
The landscape of distributed systems on the JVM has long been dominated by two paradigms: feature-rich but performance-heavy enterprise frameworks, and highly specialized but complex low-level messaging libraries. The former often introduce significant latency and GC pressure, while the latter present a steep learning curve and rely on unsafe, non-standard Java APIs.

**MVP.EXPRESS** is engineered to fill this gap. It provides a highly opinionated, vertically integrated solution that leverages the most advanced features of the modern JDK—including Project Loom's virtual threads and the FFM API—to offer unparalleled performance in a safe, maintainable, and developer-friendly package.

2. Core Philosophy & Design Goals
The design of **MVP.EXPRESS** is guided by four foundational principles:

Extreme Performance: To minimize latency and maximize throughput by controlling the entire stack, from binary encoding on the wire to the I/O scheduling model. The primary goal is to reduce CPU overhead and eliminate garbage collection pauses in the communication path.

Developer Experience (DX): To provide a simple, intuitive programming model. This is achieved through a clean YAML-based DSL, powerful command-line tooling for code generation, and APIs that embrace modern Java idioms like fluent builders and records.

Safety and Modernity: To operate entirely within the safety guarantees of the JVM. The framework has a strict no Unsafe policy, instead relying on the officially supported FFM API for all native memory operations.

Operational Simplicity: To provide the core benefits of a distributed environment (e.g., service discovery, resilience) as an integrated part of the framework, reducing the need for complex external infrastructure.

3. Architectural Overview
mvp.express is designed as a multi-module project, ensuring a clean separation of concerns. Each module has a distinct responsibility, allowing for independent development, testing, and optimization.

The five core modules are:

codec: Handles the binary serialization format (MYRA).

codegen: Parses the DSL and generates Java source files.

cli: Provides the command-line interface for developers.

runtime: Contains the high-level APIs and service orchestration logic.

transport: Manages low-level, point-to-point network communication.

These modules work in concert to provide a seamless experience, from service definition to deployment.

4. Modular Breakdown
4.1 codec: The MYRA Binary Protocol
The cornerstone of the framework's performance is MYRA (Memory yielded, Rapid Access), a custom binary codec.

Zero-Copy & GC-Free: MYRA operates exclusively on off-heap memory using Java's MemorySegment. All encoding and decoding operations read from and write to these segments directly, bypassing the JVM heap entirely. This eliminates both memory copy overhead and GC pressure from the serialization process.

Flyweight Pattern: Generated message objects are implemented as "flyweights." A Java record or class for a message type is simply a lightweight wrapper around a MemorySegment, containing VarHandles to access field data at specific memory offsets. There is no traditional deserialization step; fields are read on-demand directly from the underlying memory.

4.2 codegen & cli
Developer interaction with the framework begins with a simple YAML-based DSL. This DSL defines services, methods, and message types. The cli module provides the mvp-cli tool which invokes the codegen module to:

Parse & Validate: Read the .yaml service definition and validate its syntax.

Generate Source Code: Automatically generate all necessary Java source files, including:

Service interfaces.

Server skeletons for business logic implementation.

Client stubs with a fluent API for invoking remote methods.

MYRA message records (flyweights).

4.3 runtime
The runtime module is the brain of the framework. It orchestrates all the components but contains no networking logic itself.

Core APIs: Defines the primary MvpeServer and MvpeClient interfaces and their builders.

Service Orchestration: Manages the lifecycle of services, dispatches incoming requests to the correct service implementation, and integrates with the service discovery client.

Extensibility: Provides a simple Interceptor interface for implementing cross-cutting concerns like logging, metrics, authentication, and retry policies.

4.4 transport
The transport module is responsible for moving bytes over the network. It provides the low-level, high-performance engine that the runtime directs. Its canonical implementation is a native C library leveraging io_uring, as detailed in Section 6.

5. Service-to-Service Communication
mvp.express is designed for large-scale, distributed environments. Its communication model is brokerless, promoting low latency and high resilience.

5.1 Service Discovery with Consul
The framework uses HashiCorp Consul as its service registry to provide a dynamic, self-healing environment.

Registration: On startup, each instance of a service registers itself with the Consul agent, providing its service name, IP address, and port. It also registers a series of rich health checks (e.g., TCP, application-level).

Discovery: When a client service needs to communicate with a target service, its runtime queries Consul for a list of healthy instances of that target.

Health Checks: Consul continuously monitors the health of all registered instances. If an instance fails its health checks, Consul immediately removes it from the list of available services, preventing traffic from being sent to a dead or overloaded node.

5.2 Direct Point-to-Point Communication
After an instance discovers the address of a target instance via Consul, all subsequent communication is direct point-to-point TCP. The service registry is not in the data path, ensuring that there is no intermediary to add latency or become a bottleneck. The transport layer manages a pool of long-lived TCP connections to each target instance to amortize the cost of the TCP handshake.

6. The Native Transport Layer: A Deep Dive into io_uring and FFM
To achieve the absolute pinnacle of performance, mvp.express employs a custom transport layer written in C, leveraging Linux's io_uring interface. This native library is safely and efficiently integrated into the Java ecosystem using the FFM API.

6.1 Rationale for a Native Transport
Modern Java I/O (NIO) is based on epoll, which requires multiple syscalls per I/O operation. At extreme scale, this context switching becomes a significant CPU bottleneck. io_uring solves this by using two shared memory rings (Submission Queue and Completion Queue) to enable batching of I/O operations, drastically reducing syscall overhead and CPU usage under load.

6.2 Architecture
The transport is a hybrid model where the high-level logic resides in Java and the low-level I/O execution resides in a native C library.

Java transport Module: Contains the high-level logic for connection pooling, request lifecycle management, and callback handling.

FFM Bridge: The jextract tool is used to automatically generate Java bindings from a C header file. This provides a safe, statically-typed bridge to the native C library.

C mvp_transport Library: A small, highly optimized C library built on liburing. Its sole job is to manage the io_uring event loop, submit I/O requests to the kernel, and fire callbacks back up to Java when I/O operations complete.

Kernel io_uring Subsystem: The Linux kernel's I/O interface that performs the actual work.

6.3 The Tightly Coupled Codec and Transport: A Zero-Copy Data Path
The most significant performance gain comes from the tight coupling of the native transport with the MYRA codec. This integration enables a true zero-copy data path from the network card to the application logic.

The data flow for an incoming RPC request is as follows:

Frame Arrival: The C transport layer begins reading from a socket and identifies the 4-byte length prefix of an incoming MYRA message.

Memory Allocation Request (C -> Java): The C code does not allocate its own buffer. Instead, it makes an FFM "upcall" to a Java memory manager to request a MemorySegment of the exact size needed for the message payload.

Direct-to-Java-Memory Read: The C layer provides the memory address of this Java-owned MemorySegment to io_uring as the destination buffer for the network read operation. The kernel then writes the incoming packet data directly into memory managed by the Java runtime.

Completion Notification (C -> Java): Once the io_uring read completes, the C layer makes a final upcall to the Java runtime, notifying it that the message has been fully received in the provided MemorySegment.

Instantaneous "Deserialization": The Java runtime passes this MemorySegment to the MyraCodec. A flyweight message object is instantly wrapped around the segment. No data is moved or copied.

This process eliminates the intermediate buffer copy that is typical in decoupled systems, saving CPU cycles, reducing memory bandwidth, and improving CPU cache locality.

6.4 Pseudo-code Example
C-Side Event Loop (mvp_transport.c):

// C pseudo-code
void event_loop(java_callback_handler handler) {
    while (true) {
        io_uring_wait_cqe(ring, &cqe); // Block until I/O is done
        io_event*event = (io_event*)cqe->user_data;

        if (event->type == READ_FRAME_HEADER) {
            int payload_size = parse_header(event->buffer);
            // Upcall to Java to get a buffer
            MemoryAddress java_buffer = handler.request_buffer(payload_size);
            // Submit new read directly into Java's buffer
            submit_read_payload(ring, event->fd, java_buffer, payload_size);
        } else if (event->type == READ_PAYLOAD) {
            // Upcall to Java to process the complete message
            handler.on_message_received(event->fd, event->buffer);
        }
        io_uring_cqe_seen(ring, cqe);
    }
}

Java-Side Callback Handler (MvpeUpcallHandler.java):

// Java pseudo-code
class MvpeUpcallHandler {
    // Called from C
    MemorySegment requestBuffer(int size) {
        return memoryPool.acquire(size);
    }

    // Called from C when a full message is in the buffer
    void onMessageReceived(int connectionId, MemorySegment messageSegment) {
        // Wrap the segment in a flyweight, no copy!
        MyraMessage message = MyraMessage.wrap(messageSegment);
        // Dispatch to the application's business logic
        dispatcher.dispatch(connectionId, message);
    }
}

7. Conclusion
mvp.express represents a paradigm shift for high-performance computing on the JVM. By combining a modern, safe, and developer-friendly API with a deeply optimized native transport layer, it provides a solution that was previously unattainable without resorting to unsafe practices or the complexities of other languages. The tightly integrated architecture, from the MYRA codec to the io_uring transport, creates a holistic system designed for one purpose: to provide the lowest possible latency and highest possible throughput for mission-critical, service-to-service communication in the modern Java era.
