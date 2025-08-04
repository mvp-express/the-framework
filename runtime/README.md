# `runtime/`

This module defines the core API for MVP.Express, including:

- `MvpeServer`, `MvpeClient`, and builder classes
- Service dispatching, registry, and proxy logic
- Interceptor interface and default implementations (retry, timeout, etc.)

This is the heart of the runtime system and has no network logic of its own.
