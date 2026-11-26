/*
 * Copyright (c) 2022-2026 Guus der Kinderen. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.goodbytes.xmpp.xep0363.slot;

import nl.goodbytes.xmpp.xep0363.SecureUniqueId;
import nl.goodbytes.xmpp.xep0363.Slot;
import nl.goodbytes.xmpp.xep0363.SlotProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;

// TODO: quick 'n dirty singleton. Is this the best choice?
// TODO: persist internal state to allow for restart survival.
public class DefaultSlotProvider implements SlotProvider
{
    private static final Logger Log = LoggerFactory.getLogger( DefaultSlotProvider.class );

    private static final long PUT_EXPIRY_NANOS = Duration.ofMinutes( 5 ).toNanos();

    private final Map<SecureUniqueId, Entry> slots = new ConcurrentHashMap<>();

    // Entries in the order they were stored. As every entry lives equally long, this is also the order in which they
    // expire, which allows expired entries to be removed without inspecting the ones that have not expired. When slots
    // are created concurrently, the order is only approximate: an expired entry can wait behind one that was stored a
    // moment later. That only delays its removal, as consume() checks expiry itself.
    private final Queue<Entry> expiryOrder = new ConcurrentLinkedQueue<>();

    // Ensures that only one thread at a time removes expired entries.
    private final AtomicBoolean purging = new AtomicBoolean( false );

    private final LongSupplier nanoTime;

    /**
     * Creates a provider that times slot expiry with {@link System#nanoTime()}.
     */
    public DefaultSlotProvider()
    {
        this( System::nanoTime );
    }

    /**
     * Creates a provider that times slot expiry with the given time source. This allows tests to control the passing
     * of time.
     *
     * @param nanoTime a time source that behaves like {@link System#nanoTime()}.
     */
    DefaultSlotProvider(@Nonnull final LongSupplier nanoTime)
    {
        this.nanoTime = nanoTime;
    }

    /**
     * Stores a slot, which can be consumed until five minutes have passed. Storing a slot again restarts its five
     * minutes. Also removes slots that have expired without being consumed.
     *
     * @param slot the slot to store.
     */
    @Override
    public void create(@Nonnull Slot slot)
    {
        final long now = nanoTime.getAsLong();
        purgeExpired( now );
        final Entry entry = new Entry( slot, now + PUT_EXPIRY_NANOS );
        slots.put( slot.getUuid(), entry );
        expiryOrder.add( entry );
    }

    /**
     * Hands out a stored slot and removes it, so that it is handed out only once. Also removes slots that have expired
     * without being consumed.
     *
     * @param uuid the identifier of the slot.
     * @return the slot, or null when no slot was stored for the identifier, or when it has expired or was consumed already.
     */
    @Override
    @Nullable
    public Slot consume(@Nonnull SecureUniqueId uuid)
    {
        final long now = nanoTime.getAsLong();
        purgeExpired( now );

        // Removing in one step guarantees that a slot is handed out at most once, even when requested concurrently.
        final Entry entry = slots.remove( uuid );
        if ( entry == null )
        {
            return null;
        }
        final Slot slot = entry.release();
        if ( entry.isExpired( now ) )
        {
            Log.debug( "Refusing to consume slot created by {} on {}: it has expired.", slot.getCreator(), slot.getCreationDate() );
            return null;
        }
        return slot;
    }

    /**
     * Returns the number of stored slots, including ones that expired but were not removed yet. Visible for testing.
     *
     * @return the number of stored slots.
     */
    int size()
    {
        return slots.size();
    }

    /**
     * Removes the slots that have expired without being consumed.
     *
     * @param now the current value of the time source.
     */
    private void purgeExpired( final long now )
    {
        // When another thread is already removing expired entries, leave it to that thread.
        if ( !purging.compareAndSet( false, true ) )
        {
            return;
        }
        try
        {
            int purged = 0;
            Entry head;
            while ( ( head = expiryOrder.peek() ) != null && head.isExpired( now ) )
            {
                // As only this thread takes entries off the queue, this removes the entry that was just inspected.
                expiryOrder.poll();

                // The map entry is removed only if it is still this one: a slot that was consumed or stored again since is not.
                if ( slots.remove( head.uuid, head ) )
                {
                    purged++;
                }
            }
            if ( purged > 0 )
            {
                Log.debug( "Purged {} expired slot(s) that were never consumed. {} slot(s) remain.", purged, slots.size() );
            }
        }
        finally
        {
            purging.set( false );
        }
    }

    /**
     * A stored slot and the moment at which it expires.
     */
    private static final class Entry
    {
        private final SecureUniqueId uuid;
        private final long expiresAtNanos;

        // Released when the slot is consumed, so that an entry that waits in the expiry queue does not keep it in memory.
        private Slot slot;

        /**
         * Creates an entry.
         *
         * @param slot the stored slot.
         * @param expiresAtNanos the value of the time source at which the slot expires.
         */
        private Entry(@Nonnull final Slot slot, final long expiresAtNanos)
        {
            this.uuid = slot.getUuid();
            this.slot = slot;
            this.expiresAtNanos = expiresAtNanos;
        }

        /**
         * Returns the slot and lets go of it. Only the thread that removed this entry from the map may call this, and
         * only once.
         *
         * @return the stored slot.
         */
        @Nonnull
        private Slot release()
        {
            final Slot result = slot;
            slot = null;
            return result;
        }

        /**
         * Checks if the slot has expired.
         *
         * @param now the current value of the time source.
         * @return true when the slot has expired.
         */
        private boolean isExpired( final long now )
        {
            // Compare by subtraction, as values from System.nanoTime() may overflow.
            return now - expiresAtNanos >= 0;
        }
    }
}
