JiaCache: Java In-memory Accelerator
A Modern, GC-Free, High-Performance Caching Solution for Java
Version: 1.1
Date: September 2, 2025
Authors: rohanray@gmail.com

1. Executive Summary
JiaCache represents a paradigm shift in Java-based caching, engineered specifically for the demands of high-throughput, low-latency systems like the mvp.express RPC framework. It leverages cutting-edge JDK 24+ features to deliver unprecedented performance. By utilizing the Foreign Function & Memory (FFM) API, Project Loom, and the Vector API, JiaCache provides a fully off-heap, GC-free caching layer that offers microsecond-level latency and massive concurrent scalability.

The core innovation of JiaCache is its serialized-form, off-heap storage model. Unlike traditional caches that store Java objects on the heap, JiaCache stores the pre-serialized binary representation of data. This eliminates the cost of deserialization on cache hits and, most critically, removes the cached data from the purview of the Garbage Collector, resulting in zero GC pressure from the cache itself.

Key Innovations:

GC-Free Operation: Fully off-heap storage of serialized data eliminates GC pauses and pressure.

Serialized-Form Caching: Caches MemorySegment payloads from the MYRA codec, enabling true zero-copy network responses.

Vectorized Processing: Leverages the Vector API for SIMD-accelerated index lookups, comparisons, and bulk operations.

Loom-Powered Concurrency: Utilizes virtual threads for non-blocking cache misses and highly concurrent background operations.

Lock-Free Design: Employs CAS-based data structures to ensure maximum throughput under extreme concurrent load.

2. Introduction and Motivation
2.1 Limitations of the Current Landscape
Modern Java applications, particularly in the microservices domain, face significant challenges with existing caching solutions:

GC Pressure: Heap-based caches are a primary contributor to GC pressure, leading to unpredictable "stop-the-world" pauses that are unacceptable in latency-sensitive services.

Serialization Overhead: Caching Java objects requires a costly serialization step before a network send. Caching the objects themselves means this cost is paid on every single RPC response.

Concurrency Bottlenecks: Traditional lock-based caches suffer from contention under high concurrent loads. Thread pool saturation becomes a real risk.

Lack of Hardware Acceleration: Standard caches are not designed to take advantage of modern CPU features like SIMD, leaving significant performance potential untapped.

2.2 Opportunities with the Modern JDK
JDK 24+ provides a suite of powerful, low-level features that make it possible to build a new class of high-performance libraries that rival native C++ or Rust implementations.

Foreign Function & Memory (FFM) API: Enables direct, safe, and efficient access to off-heap memory, forming the foundation of our GC-free storage.

Project Loom: Allows for millions of concurrent, non-blocking operations via virtual threads, perfect for handling cache misses without consuming expensive platform threads.

Vector API: Unlocks CPU-level data parallelism (SIMD) for operations like hashing, searching, and bulk data manipulation, providing an order-of-magnitude performance boost.

3. Architecture and Integration with mvp.express
JiaCache is designed as a vertically integrated component of the mvp.express ecosystem. Its primary function is to cache the MYRA-serialized MemorySegment payloads, not the Java records themselves.

3.1 High-Level Design
+-------------------------------------------------------------+
|                 mvp.express Service Logic                   |
+-------------------------------------------------------------+
|         JiaCache API (e.g., cache.getSerialized("key"))     |
+-------------------------------------------------------------+
|      Concurrency Layer (Project Loom Virtual Threads)       |
+-------------------------------------------------------------+
|    Index Layer (Lock-Free Hash Table + Vector API Search)   |
+-------------------------------------------------------------+
|    Storage Layer (Off-Heap Slab Allocator for Payloads)     |
+-------------------------------------------------------------+
|            Memory Layer (FFM MemorySegment Manager)         |
+-------------------------------------------------------------+

3.2 Core RPC Framework Use Case: The Zero-Copy Read Path
The tight integration between JiaCache and mvp.express enables an incredibly efficient read path that is both zero-copy and GC-free.

Flow Diagram:

[Client] ---> RPC Request ---> [mvp.express Server]
                                       |
                                       | 1. Query Cache("user:123")
                                       v
                                  [JiaCache]
                                       |
           +---------------------------+---------------------------+
           | 2a. HIT                                           | 2b. MISS
           v                                                   v
  [Off-Heap Index] -> 3a. Find Address                 [Service Logic] -> 4b. Fetch from DB
           |                                                   |
           v                                                   v
  [Off-Heap Storage] -> 4a. Get Pre-serialized       [MyraCodec] -> 5b. Serialize to Payload
           |                MemorySegment                      |
           |                                                   v
           +---------------------> 5a. Return Segment <--- [JiaCache] -> 6b. Put Payload
                                       |
                                       v
                         [Transport Layer] -> 6a. Direct Memory Copy to Network -> [Client]

Explanation:

An RPC request arrives at an mvp.express server.

The service logic queries JiaCache for a key.

On a Cache Hit: The off-heap index provides the memory address of the pre-serialized payload. This MemorySegment is returned to the transport layer.

On a Cache Miss: The service logic fetches data from the source of truth (e.g., a database), serializes it using MyraCodec, and puts the resulting MemorySegment payload into JiaCache for future requests.

In both cases, the final step is for the transport layer to perform a direct memory copy from the MemorySegment (either from the cache or the fresh serialization) to the network buffer. This flow completely bypasses on-heap object allocation and re-serialization on the hot path.

4. Deep Dive: Core Components
4.1 FFM-Based Memory Management
The storage layer is built on a slab allocation strategy to manage the off-heap memory efficiently and combat fragmentation.

Size Classes: Memory is divided into slabs for different size classes (e.g., 64B, 128B, 256B...) to accommodate varying MYRA payload sizes.

Allocation: When cache.put() is called, the allocator selects the appropriate slab for the payload size and copies the data into a free slot.

Defragmentation: A background virtual thread can periodically run a compaction process to move active entries together and free up entire slabs.

(Your existing MemoryLayouts and SlabAllocator pseudo-code from the original document is excellent and fits here perfectly.)

4.2 Lock-Free Indexing with Vector API
The index is a custom-built, lock-free hash table that maps a key to the long memory address of its corresponding value in the storage layer.

Data Structure: An array of atomic references (AtomicReferenceArray) where each element is the head of a collision chain. All mutations are handled via CAS loops.

Vector-Accelerated Lookups: For dense index segments, key comparisons can be accelerated. Instead of comparing one key at a time, the Vector API can load a batch of keys from the index and compare them against the target key in a single SIMD instruction, dramatically speeding up searches in collision-heavy scenarios.

(Your LockFreeHashTable pseudo-code is a great starting point. Vectorized comparison would be an enhancement for the get method's loop.)

4.3 Loom-Powered Concurrency
Project Loom is used to prevent blocking and scale to massive concurrency.

Asynchronous Loading: An API like cache.getAsync(key, loaderFunction) can be offered. If the key is missing, a new virtual thread is spawned to execute the loaderFunction (e.g., a database call). The future completes when the value is loaded and inserted into the cache, without blocking any platform threads.

Background Tasks: All maintenance tasks—expiration, defragmentation, statistics gathering—run on dedicated virtual threads, ensuring they have minimal impact on foreground application performance.

(Your ConcurrencyManager and StructuredTaskScope examples are perfectly suited for this.)

5. Advanced Features & Future Roadmap
5.1 Eviction Policies
A high-performance cache needs intelligent eviction.

Frequency-Based (LFRU): A combination of LFU and LRU. Evict entries that are both least frequently and least recently used. This can be implemented efficiently by packing access frequency and a timestamp into a single long in the index entry metadata.

TinyLFU: An advanced, scan-resistant algorithm that provides excellent hit ratios with a very small memory footprint, implemented using a compact Bloom filter and a simple recency-biased counter.

5.2 Tiered Storage
For extremely large datasets, a multi-level hierarchy can be employed:

L1 (DRAM, Off-Heap): The core JiaCache, for the hottest data.

L2 (Memory-Mapped File): An FFM-managed memory-mapped file on a high-speed NVMe drive for warm data. Access is slower but still avoids the network.

L3 (Distributed): Integration with a distributed key-value store or another mvp.express service for cold data.

5.3 Observability
Deep integration with monitoring tools is essential.

JFR Events: Custom JFR events for CacheHit, CacheMiss, CacheEviction, and CachePut will provide extremely low-overhead, detailed diagnostics for performance tuning.

Micrometer Metrics: Exposing metrics for hit ratio, cache size, average load penalty, and eviction counts for integration with Prometheus/Grafana dashboards.

6. Conclusion
JiaCache is not a general-purpose replacement for caches like Caffeine. It is a specialized, high-performance component designed to be an integral part of the mvp.express framework. Its GC-free, serialized-form architecture provides a level of performance and efficiency that is currently unachievable with on-heap caches.

By integrating JiaCache, mvp.express can offer a complete, vertically integrated solution for building Java services that operate at the absolute limits of hardware performance, making it an ideal choice for the most demanding use cases in finance, ad-tech, gaming, and real-time analytics.