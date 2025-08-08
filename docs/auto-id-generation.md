# MVP.Express Auto ID Generation and Stability Validation

This document describes the production-grade strategy and implementation used by MVP.Express to:
- deterministically generate IDs for services, methods, and messages,
- validate stability across schema edits and CI runs,
- prevent accidental ID reuse through tombstones and rename-aware aliases.

Contents
- Goals
- ID Spaces and Ranges
- Lockfile Model and On-Disk Format
- Deterministic Allocation (FNV-1a + Probing)
- Assignment and Validation Flow
- Renames and Deletions
- CI and Developer Workflow
- Integration Points
- FAQs

Goals
- Deterministic: Same names → same IDs everywhere.
- Collision-safe: Unique in each ID space, with deterministic probing.
- Human-governable: Explicit IDs allowed, aliases for renames, tombstones for removals.
- CI-friendly: Non-writing CHECK mode to protect stability; WRITE mode for local updates.

ID Spaces and Ranges
- service.id: 32..64999 (avoid low/high reserved bands).
- method.id: 16..239 per service namespace.
- message.id: 32..64000 globally.
Reserved bands can be extended by project policy; allocator honors tombstones/reserved sets.

Lockfile Model
In-memory:
- services: Map<String name, Int id>
- methods: Map<String "Service.Method", Int id>
- messages: Map<String name, Int id>
- tombstoneServices: Set<Int>
- tombstoneMethods: Map<String service, Set<Int>>
- tombstoneMessages: Set<Int>
- aliasServices: Map<String old, String new>
- aliasMessages: Map<String old, String new>
- version: Int (default 1)

On-Disk Format (Properties-based, no extra dependencies)
- File name: .mvpe.ids.lock (configurable)
- Keys:
  - version=1
  - services.<name>=<id>
  - methods.<Service>.<Method>=<id>
  - messages.<name>=<id>
  - tombstones.services=5,6
  - tombstones.messages=999,1000
  - tombstones.methods.<Service>=15,16
  - aliases.services.<old>=<new>
  - aliases.messages.<old>=<new>
- Names are escaped to support separators:
  - '%' -> '%25'
  - '.' -> '%2E'

Deterministic Allocation
- Hash: FNV-1a 32-bit over canonical names:
  - Service: "Service:<ServiceName>"
  - Method: "ServiceName.MethodName"
  - Message: "Message:<MessageName>"
- Mapping: min + (hash % (max - min + 1))
- Probing: Append "#k", k=0..N (N=4096 for global, 1024 for methods)
- Skip:
  - IDs already in use,
  - tombstoned IDs,
  - out-of-range values (never produced by mapping).
- Result is stable given the same lockfile contents and names.

Assignment and Validation Flow
- Input: ServiceSchema (service, id?, methods[] with id?, messages[] with optional id via reflection).
- Load .mvpe.ids.lock (or start empty).
- Apply aliases to resolve renames:
  - If service/message has an alias entry (old->new), switch to canonical name.
- For each item:
  1) If explicit ID in schema:
     - Validate range and tombstones.
     - Must match lockfile if present (CHECK mode fails on mismatch; WRITE mode updates lock).
  2) If not explicit but present in lockfile:
     - Use lockfile and set into schema (methods and service).
  3) Otherwise:
     - Allocate deterministically and set into schema (WRITE mode), or fail (CHECK mode).
- Messages:
  - Stored/validated in lock. If MessageDef has id accessors (getId/setId), they are used reflectively.
- Save:
  - WRITE mode persists updates to lock.
  - CHECK mode never writes; fails on drift or missing entries.

Renames and Deletions
- Renames:
  - Add aliases.<space>.<old>=<new> to keep the ID stable for the renamed symbol.
  - The assignment step resolves aliases and enforces the inherited ID.
- Deletions:
  - Remove active entry and add its ID to the corresponding tombstones set.
  - Prevents accidental reuse of the retired ID.

CI and Developer Workflow
- Local development:
  - Run generator/assignment with WRITE mode to allocate new IDs and update .mvpe.ids.lock.
  - Commit schema changes together with the updated lockfile.
- Continuous Integration:
  - Run with CHECK mode.
  - If new symbols are introduced without corresponding lock entries or any drift occurs, the build fails with guidance to run WRITE mode locally.

Integration Points
- Core implementation (package express.mvp.codegen):
  - IdAllocator: Deterministic ID allocation (FNV-1a + probing).
  - IdsLock: In-memory lockfile model.
  - LockfileManager: Properties-based read/write to .mvpe.ids.lock.
  - IdAssignment: Orchestrates assigning/validating IDs to a ServiceSchema and updates lock.
- The orchestrator/generator should invoke:
  - IdAssignment.assignAndValidate(schema, lockPath, mode)
  - before emitting code/SBE artifacts.

FAQs
- Why Properties and not YAML?
  - Avoids adding dependencies in the core codegen path and keeps the format human-diffable and editable.
- Can I use explicit IDs?
  - Yes. They’re validated. Conflicts cause failure (CHECK) or lock updates (WRITE) per mode.
- How do I choose the mode?
  - WRITE locally to accept new allocations and tombstones; CHECK in CI to enforce stability.
- What about messages if MessageDef has no id field?
  - IDs are tracked in the lock and applied reflectively when possible. Generators can consult the lock for message IDs even if the model doesn’t store them directly.
