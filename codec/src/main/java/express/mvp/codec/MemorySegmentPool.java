package express.mvp.codec;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Zero-copy, GC-free MemorySegment pool for MVP.Express.
 * 
 * Uses a single long-lived Arena and pools pre-allocated segments
 * to eliminate GC pressure and achieve true zero-copy performance.
 * 
 * Thread-safe and lock-free implementation using ConcurrentLinkedQueue.
 */
public class MemorySegmentPool {
    
    private static final int DEFAULT_SEGMENT_SIZE = 8192; // 8KB segments
    private static final int DEFAULT_POOL_SIZE = 1000;
    
    private final Arena arena;
    private final ConcurrentLinkedQueue<MemorySegment> availableSegments;
    private final int segmentSize;
    private final AtomicInteger allocatedCount = new AtomicInteger(0);
    private final AtomicInteger pooledCount = new AtomicInteger(0);
    
    public MemorySegmentPool() {
        this(DEFAULT_SEGMENT_SIZE, DEFAULT_POOL_SIZE);
    }
    
    public MemorySegmentPool(int segmentSize, int initialPoolSize) {
        this.segmentSize = segmentSize;
        this.arena = Arena.ofShared(); // Single long-lived arena
        this.availableSegments = new ConcurrentLinkedQueue<>();
        
        // Pre-allocate segments to avoid allocation during hot path
        for (int i = 0; i < initialPoolSize; i++) {
            MemorySegment segment = arena.allocate(segmentSize);
            availableSegments.offer(segment);
            pooledCount.incrementAndGet();
        }
    }
    
    /**
     * Acquires a MemorySegment from the pool.
     * Returns a pre-allocated segment or creates a new one if pool is empty.
     * 
     * @return A MemorySegment ready for use
     */
    public MemorySegment acquire() {
        MemorySegment segment = availableSegments.poll();
        if (segment != null) {
            pooledCount.decrementAndGet();
            allocatedCount.incrementAndGet();
            return segment;
        }
        
        // Pool is empty, allocate new segment from shared arena
        segment = arena.allocate(segmentSize);
        allocatedCount.incrementAndGet();
        return segment;
    }
    
    /**
     * Acquires a MemorySegment with specific size.
     * For sizes larger than pool segment size, allocates directly from arena.
     * 
     * @param size Required size in bytes
     * @return A MemorySegment of at least the requested size
     */
    public MemorySegment acquire(int size) {
        if (size <= segmentSize) {
            return acquire();
        }
        
        // Large allocation - allocate directly from arena
        allocatedCount.incrementAndGet();
        return arena.allocate(size);
    }
    
    /**
     * Returns a MemorySegment to the pool for reuse.
     * Only segments of the standard pool size are returned to pool.
     * 
     * @param segment The segment to return
     */
    public void release(MemorySegment segment) {
        if (segment.byteSize() == segmentSize) {
            // Clear the segment before returning to pool
            segment.fill((byte) 0);
            availableSegments.offer(segment);
            pooledCount.incrementAndGet();
        }
        // Large segments are not pooled, just let them be GC'd with arena
        allocatedCount.decrementAndGet();
    }
    
    /**
     * Gets the number of segments currently allocated from the pool.
     */
    public int getAllocatedCount() {
        return allocatedCount.get();
    }
    
    /**
     * Gets the number of segments currently available in the pool.
     */
    public int getAvailableCount() {
        return pooledCount.get();
    }
    
    /**
     * Gets the standard segment size for this pool.
     */
    public int getSegmentSize() {
        return segmentSize;
    }
    
    /**
     * Closes the pool and releases all resources.
     * After calling this method, the pool cannot be used.
     */
    public void close() {
        arena.close();
        availableSegments.clear();
    }
    
    /**
     * Creates a slice of the given segment without copying data.
     * This is a true zero-copy operation.
     * 
     * @param segment Source segment
     * @param offset Offset in bytes
     * @param length Length in bytes
     * @return A slice of the original segment
     */
    public static MemorySegment slice(MemorySegment segment, long offset, long length) {
        return segment.asSlice(offset, length);
    }
}