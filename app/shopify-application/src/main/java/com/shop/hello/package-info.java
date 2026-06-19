/**
 * Phase 0 vertical "hello tenant" slice — NOT a real bounded context. It exists only to prove
 * the spine end to end: resolve tenant -> route to shard -> read a trivial tenant row -> publish
 * a domain event through the outbox -> the job-engine drains it and a handler logs it. It is
 * deleted/replaced once real domain modules land in Phase 1+.
 */
package com.shop.hello;
