# `transport/`

This module provides low-level TCP-based transport using Java 21 and Project Panama.

- Uses `SocketChannel`, `MemorySegment`, and `VarHandle`
- Handles message framing, I/O, backpressure and reconnect
- Optimized for virtual threads

Depends on `codec` for binary serialization.
