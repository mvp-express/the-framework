package express.mvp.codec;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for MemorySegmentPool functionality.
 */
public class MemorySegmentPoolTest {
    
    private MemorySegmentPool pool;
    
    @BeforeEach
    void setUp() {
        pool = new MemorySegmentPool();
    }
    
    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.close();
        }
    }
    
    @Test
    void testDefaultConstructor() {
        assertEquals(8192, pool.getSegmentSize());
        assertEquals(1000, pool.getAvailableCount()); // Default pool size
        assertEquals(0, pool.getAllocatedCount());
    }
    
    @Test
    void testCustomConstructor() {
        pool.close(); // Close default pool
        pool = new MemorySegmentPool(4096, 50);
        
        assertEquals(4096, pool.getSegmentSize());
        assertEquals(50, pool.getAvailableCount());
        assertEquals(0, pool.getAllocatedCount());
    }
    
    @Test
    void testBasicAcquireRelease() {
        int initialAvailable = pool.getAvailableCount();
        
        MemorySegment segment = pool.acquire();
        assertNotNull(segment);
        assertEquals(pool.getSegmentSize(), segment.byteSize());
        assertEquals(initialAvailable - 1, pool.getAvailableCount());
        assertEquals(1, pool.getAllocatedCount());
        
        pool.release(segment);
        assertEquals(initialAvailable, pool.getAvailableCount());
        assertEquals(0, pool.getAllocatedCount());
    }
    
    @Test
    void testAcquireWithSpecificSize() {
        // Test acquiring segment with size smaller than pool segment size
        MemorySegment smallSegment = pool.acquire(1024);
        assertNotNull(smallSegment);
        assertEquals(pool.getSegmentSize(), smallSegment.byteSize()); // Should return pool-sized segment
        
        // Test acquiring segment with size equal to pool segment size
        MemorySegment equalSegment = pool.acquire(pool.getSegmentSize());
        assertNotNull(equalSegment);
        assertEquals(pool.getSegmentSize(), equalSegment.byteSize());
        
        // Test acquiring segment with size larger than pool segment size
        int largeSize = pool.getSegmentSize() * 2;
        MemorySegment largeSegment = pool.acquire(largeSize);
        assertNotNull(largeSegment);
        assertEquals(largeSize, largeSegment.byteSize());
        
        pool.release(smallSegment);
        pool.release(equalSegment);
        pool.release(largeSegment);
    }
    
    @Test
    void testPoolExhaustion() {
        int initialAvailable = pool.getAvailableCount();
        List<MemorySegment> segments = new ArrayList<>();
        
        // Exhaust the pool
        for (int i = 0; i < initialAvailable; i++) {
            segments.add(pool.acquire());
        }
        
        assertEquals(0, pool.getAvailableCount());
        assertEquals(initialAvailable, pool.getAllocatedCount());
        
        // Acquire one more - should allocate new segment
        MemorySegment extraSegment = pool.acquire();
        assertNotNull(extraSegment);
        assertEquals(0, pool.getAvailableCount());
        assertEquals(initialAvailable + 1, pool.getAllocatedCount());
        
        // Release all segments
        for (MemorySegment segment : segments) {
            pool.release(segment);
        }
        pool.release(extraSegment);
        
        assertEquals(initialAvailable + 1, pool.getAvailableCount()); // Pool grows
        assertEquals(0, pool.getAllocatedCount());
    }
    
    @Test
    void testSegmentClearing() {
        MemorySegment segment = pool.acquire();
        
        // Write some data to the segment
        segment.set(java.lang.foreign.ValueLayout.JAVA_BYTE, 0, (byte) 42);
        segment.set(java.lang.foreign.ValueLayout.JAVA_BYTE, 100, (byte) 99);
        
        // Verify data is written
        assertEquals(42, segment.get(java.lang.foreign.ValueLayout.JAVA_BYTE, 0));
        assertEquals(99, segment.get(java.lang.foreign.ValueLayout.JAVA_BYTE, 100));
        
        pool.release(segment);
        
        // Acquire the same segment again
        MemorySegment clearedSegment = pool.acquire();
        
        // Verify segment is cleared
        assertEquals(0, clearedSegment.get(java.lang.foreign.ValueLayout.JAVA_BYTE, 0));
        assertEquals(0, clearedSegment.get(java.lang.foreign.ValueLayout.JAVA_BYTE, 100));
        
        pool.release(clearedSegment);
    }
    
    @Test
    void testLargeSegmentNotPooled() {
        int initialAvailable = pool.getAvailableCount();
        int largeSize = pool.getSegmentSize() * 3;
        
        MemorySegment largeSegment = pool.acquire(largeSize);
        assertEquals(largeSize, largeSegment.byteSize());
        assertEquals(initialAvailable, pool.getAvailableCount()); // Pool count unchanged
        assertEquals(1, pool.getAllocatedCount());
        
        pool.release(largeSegment);
        assertEquals(initialAvailable, pool.getAvailableCount()); // Large segment not returned to pool
        assertEquals(0, pool.getAllocatedCount());
    }
    
    @Test
    void testSliceOperation() {
        MemorySegment segment = pool.acquire();
        
        // Create a slice
        MemorySegment slice = MemorySegmentPool.slice(segment, 100, 200);
        assertNotNull(slice);
        assertEquals(200, slice.byteSize());
        
        // Write to slice and verify it affects original segment
        slice.set(java.lang.foreign.ValueLayout.JAVA_BYTE, 0, (byte) 123);
        assertEquals(123, segment.get(java.lang.foreign.ValueLayout.JAVA_BYTE, 100));
        
        pool.release(segment);
    }
    
    @Test
    void testConcurrentAccess() throws InterruptedException {
        int numThreads = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    List<MemorySegment> threadSegments = new ArrayList<>();
                    
                    // Acquire segments
                    for (int j = 0; j < operationsPerThread; j++) {
                        threadSegments.add(pool.acquire());
                    }
                    
                    // Write some data to verify segments are valid
                    for (int j = 0; j < threadSegments.size(); j++) {
                        MemorySegment segment = threadSegments.get(j);
                        segment.set(java.lang.foreign.ValueLayout.JAVA_INT, 0, j);
                    }
                    
                    // Verify data
                    for (int j = 0; j < threadSegments.size(); j++) {
                        MemorySegment segment = threadSegments.get(j);
                        assertEquals(j, segment.get(java.lang.foreign.ValueLayout.JAVA_INT, 0));
                    }
                    
                    // Release segments
                    for (MemorySegment segment : threadSegments) {
                        pool.release(segment);
                    }
                    
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();
        
        // Verify pool is in consistent state
        assertEquals(0, pool.getAllocatedCount());
        assertTrue(pool.getAvailableCount() >= 1000); // Should have at least initial pool size
    }
    
    @Test
    void testMixedSizeAllocations() throws InterruptedException {
        int numThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    List<MemorySegment> segments = new ArrayList<>();
                    
                    // Mix of different allocation sizes
                    segments.add(pool.acquire(1024));           // Small
                    segments.add(pool.acquire());               // Standard
                    segments.add(pool.acquire(pool.getSegmentSize())); // Standard
                    segments.add(pool.acquire(pool.getSegmentSize() * 2)); // Large
                    segments.add(pool.acquire(pool.getSegmentSize() * 4)); // Very large
                    
                    // Verify all segments are valid
                    for (MemorySegment segment : segments) {
                        assertNotNull(segment);
                        assertTrue(segment.byteSize() > 0);
                        // Write and read to verify segment is accessible
                        segment.set(java.lang.foreign.ValueLayout.JAVA_INT, 0, threadId);
                        assertEquals(threadId, segment.get(java.lang.foreign.ValueLayout.JAVA_INT, 0));
                    }
                    
                    // Release all segments
                    for (MemorySegment segment : segments) {
                        pool.release(segment);
                    }
                    
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();
        
        // Verify final state
        assertEquals(0, pool.getAllocatedCount());
    }
    
    @Test
    void testPoolGrowth() {
        int initialAvailable = pool.getAvailableCount();
        List<MemorySegment> segments = new ArrayList<>();
        
        // Acquire more segments than initial pool size
        int extraSegments = 100;
        for (int i = 0; i < initialAvailable + extraSegments; i++) {
            segments.add(pool.acquire());
        }
        
        assertEquals(0, pool.getAvailableCount());
        assertEquals(initialAvailable + extraSegments, pool.getAllocatedCount());
        
        // Release all segments - pool should grow
        for (MemorySegment segment : segments) {
            pool.release(segment);
        }
        
        assertEquals(initialAvailable + extraSegments, pool.getAvailableCount());
        assertEquals(0, pool.getAllocatedCount());
    }
    
    @Test
    void testClosePool() {
        MemorySegment segment = pool.acquire();
        assertNotNull(segment);
        
        pool.close();
        
        // After closing, pool should be unusable
        assertEquals(0, pool.getAvailableCount());
        
        // Attempting to use closed pool should throw exception
        assertThrows(Exception.class, () -> pool.acquire());
    }
    
    @Test
    void testZeroCopySlicing() {
        MemorySegment original = pool.acquire();
        
        // Fill original with test data
        for (int i = 0; i < 100; i++) {
            original.set(java.lang.foreign.ValueLayout.JAVA_BYTE, i, (byte) (i % 256));
        }
        
        // Create slice
        MemorySegment slice = MemorySegmentPool.slice(original, 10, 50);
        
        // Verify slice shares memory with original
        assertEquals(50, slice.byteSize());
        for (int i = 0; i < 50; i++) {
            assertEquals((10 + i) % 256, slice.get(java.lang.foreign.ValueLayout.JAVA_BYTE, i));
        }
        
        // Modify slice and verify original is affected
        slice.set(java.lang.foreign.ValueLayout.JAVA_BYTE, 0, (byte) 255);
        assertEquals(255, original.get(java.lang.foreign.ValueLayout.JAVA_BYTE, 10));
        
        pool.release(original);
    }
    
    @Test
    void testSegmentReuse() {
        // Acquire and release a segment multiple times
        MemorySegment segment1 = pool.acquire();
        long address1 = segment1.address();
        pool.release(segment1);
        
        MemorySegment segment2 = pool.acquire();
        long address2 = segment2.address();
        pool.release(segment2);
        
        // Due to pooling, we might get the same segment back
        // This tests that the pooling mechanism works
        assertNotNull(segment1);
        assertNotNull(segment2);
        assertEquals(pool.getSegmentSize(), segment1.byteSize());
        assertEquals(pool.getSegmentSize(), segment2.byteSize());
    }
}