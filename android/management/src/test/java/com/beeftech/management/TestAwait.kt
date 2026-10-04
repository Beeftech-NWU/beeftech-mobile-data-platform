package com.beeftech.management

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/*
 * The view models call their onAdded/onCreated/... callback after they update their state, on
 * whichever thread the HTTP engine answered on. A test that waits for the state and then checks a
 * plain flag can run before the callback has. Complete one of these in the callback and wait on it:
 * the state is in by then, and the wait fails with a timeout instead of an assertion if it never fires.
 */
suspend fun CompletableDeferred<Unit>.awaitFired() =
    withContext(Dispatchers.Default) { withTimeout(5_000) { await() } }
