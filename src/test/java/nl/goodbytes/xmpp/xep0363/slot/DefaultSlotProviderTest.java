/*
 * Copyright (c) 2026 Guus der Kinderen. All rights reserved.
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

import nl.goodbytes.xmpp.xep0363.SecureUUID;
import nl.goodbytes.xmpp.xep0363.Slot;
import org.junit.Before;
import org.junit.Test;
import org.xmpp.packet.JID;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Verifies how {@link DefaultSlotProvider} hands out slots, and when they expire.
 */
public class DefaultSlotProviderTest
{
    private FakeNanoTime time;
    private DefaultSlotProvider provider;

    /**
     * Creates a provider that uses a fake time source, so that tests can control the passing of time.
     */
    @Before
    public void setUp()
    {
        time = new FakeNanoTime( 1_000_000_000L );
        provider = new DefaultSlotProvider( time );
    }

    /**
     * Creates a slot for use in a test.
     *
     * @return a new slot.
     */
    private static Slot newSlot()
    {
        return new Slot( new JID( "unit-test", "example.org", "test" ), "unittest", 41 );
    }

    /**
     * Verifies that a slot that was created can be consumed.
     */
    @Test
    public void whenSlotIsCreated_ThenItCanBeConsumed()
    {
        // Setup test fixture.
        final Slot slot = newSlot();
        provider.create( slot );

        // Execute system under test.
        final Slot result = provider.consume( slot.getUuid() );

        // Verify result.
        assertSame( "A slot that was just created should be handed out when it is consumed.", slot, result );
    }

    /**
     * Verifies that a slot can be consumed only once.
     */
    @Test
    public void whenSlotIsConsumed_ThenItCannotBeConsumedAgain()
    {
        // Setup test fixture.
        final Slot slot = newSlot();
        provider.create( slot );
        provider.consume( slot.getUuid() );

        // Execute system under test.
        final Slot result = provider.consume( slot.getUuid() );

        // Verify result.
        assertNull( "A slot should be handed out only once, but it was handed out a second time.", result );
    }

    /**
     * Verifies that no slot is handed out for an identifier that no slot was created for.
     */
    @Test
    public void whenSlotIsUnknown_ThenConsumeReturnsNull()
    {
        // Setup test fixture.
        provider.create( newSlot() );

        // Execute system under test.
        final Slot result = provider.consume( SecureUUID.generate() );

        // Verify result.
        assertNull( "Consuming an identifier for which no slot was created should not hand out a slot.", result );
    }

    /**
     * Verifies that consuming one slot does not affect other slots.
     */
    @Test
    public void whenOneSlotIsConsumed_ThenOtherSlotsRemainAvailable()
    {
        // Setup test fixture.
        final Slot first = newSlot();
        final Slot second = newSlot();
        provider.create( first );
        provider.create( second );
        provider.consume( first.getUuid() );

        // Execute system under test.
        final Slot result = provider.consume( second.getUuid() );

        // Verify result.
        assertSame( "Consuming one slot should not affect another slot that was not yet consumed.", second, result );
    }

    /**
     * Verifies that a slot is still available just before it expires.
     */
    @Test
    public void whenJustUnderFiveMinutesHavePassed_ThenSlotCanBeConsumed()
    {
        // Setup test fixture.
        final Slot slot = newSlot();
        provider.create( slot );
        time.advance( Duration.ofMinutes( 5 ).minusMillis( 1 ) );

        // Execute system under test.
        final Slot result = provider.consume( slot.getUuid() );

        // Verify result.
        assertSame( "A slot should remain available until five minutes after it was created.", slot, result );
    }

    /**
     * Verifies that a slot has expired at exactly five minutes after it was created.
     */
    @Test
    public void whenExactlyFiveMinutesHavePassed_ThenSlotHasExpired()
    {
        // Setup test fixture.
        final Slot slot = newSlot();
        provider.create( slot );
        time.advance( Duration.ofMinutes( 5 ) );

        // Execute system under test.
        final Slot result = provider.consume( slot.getUuid() );

        // Verify result.
        assertNull( "A slot should be expired at exactly five minutes after it was created.", result );
    }

    /**
     * Verifies that a slot has expired well after its five minutes are up.
     */
    @Test
    public void whenMoreThanFiveMinutesHavePassed_ThenSlotHasExpired()
    {
        // Setup test fixture.
        final Slot slot = newSlot();
        provider.create( slot );
        time.advance( Duration.ofHours( 1 ) );

        // Execute system under test.
        final Slot result = provider.consume( slot.getUuid() );

        // Verify result.
        assertNull( "A slot should be expired when more than five minutes have passed since it was created.", result );
    }

    /**
     * Verifies that each slot expires on its own schedule, so the expiry of an older slot does not take newer ones with it.
     */
    @Test
    public void whenSlotHasExpired_ThenOtherSlotsCreatedLaterRemainAvailable()
    {
        // Setup test fixture.
        final Slot early = newSlot();
        provider.create( early );
        time.advance( Duration.ofMinutes( 3 ) );
        final Slot late = newSlot();
        provider.create( late );
        time.advance( Duration.ofMinutes( 3 ) );

        // Execute system under test.
        final Slot earlyResult = provider.consume( early.getUuid() );
        final Slot lateResult = provider.consume( late.getUuid() );

        // Verify result.
        assertNull( "The slot created six minutes ago should have expired.", earlyResult );
        assertSame( "The slot created three minutes ago should not have expired along with the older one.", late, lateResult );
    }

    /**
     * Verifies that creating a slot again restarts its five minutes.
     */
    @Test
    public void whenSlotIsCreatedAgain_ThenExpiryRestarts()
    {
        // Setup test fixture.
        final Slot slot = newSlot();
        provider.create( slot );
        time.advance( Duration.ofMinutes( 4 ) );
        provider.create( slot );
        time.advance( Duration.ofMinutes( 4 ) );

        // Execute system under test.
        final Slot result = provider.consume( slot.getUuid() );

        // Verify result.
        assertSame( "Creating a slot again should restart its five minutes, but the slot expired counting from the first creation.", slot, result );
    }

    /**
     * Verifies that a slot is not expired too early when the time source overflows during its lifetime.
     */
    @Test
    public void whenTimeSourceOverflowsBeforeExpiry_ThenSlotCanBeConsumed()
    {
        // Setup test fixture.
        time = new FakeNanoTime( Long.MAX_VALUE - Duration.ofMinutes( 1 ).toNanos() );
        provider = new DefaultSlotProvider( time );
        final Slot slot = newSlot();
        provider.create( slot );
        time.advance( Duration.ofSeconds( 30 ) );

        // Execute system under test.
        final Slot result = provider.consume( slot.getUuid() );

        // Verify result.
        assertSame( "A slot should remain available for five minutes, even when the time source is about to overflow.", slot, result );
    }

    /**
     * Verifies that a slot still expires after five minutes when the time source overflows during its lifetime.
     */
    @Test
    public void whenTimeSourceOverflowsAndFiveMinutesHavePassed_ThenSlotHasExpired()
    {
        // Setup test fixture.
        time = new FakeNanoTime( Long.MAX_VALUE - Duration.ofMinutes( 1 ).toNanos() );
        provider = new DefaultSlotProvider( time );
        final Slot slot = newSlot();
        provider.create( slot );
        time.advance( Duration.ofMinutes( 5 ) );

        // Execute system under test.
        final Slot result = provider.consume( slot.getUuid() );

        // Verify result.
        assertNull( "A slot should expire after five minutes, even when the time source overflowed in the meantime.", result );
    }

    /**
     * Verifies that expired slots that are never consumed are removed, so they do not accumulate in memory.
     */
    @Test
    public void whenExpiredSlotsAreNeverConsumed_ThenTheyAreEventuallyRemoved()
    {
        // Setup test fixture.
        provider.create( newSlot() );
        provider.create( newSlot() );
        time.advance( Duration.ofMinutes( 5 ) );

        // Execute system under test.
        provider.create( newSlot() );

        // Verify result.
        assertEquals( "Creating a slot should remove the expired slots that were never consumed, leaving only the new one.", 1, provider.size() );
    }

    /**
     * Verifies that removing expired slots leaves the slots that have not yet expired alone.
     */
    @Test
    public void whenExpiredSlotsAreRemoved_ThenUnexpiredSlotsAreKept()
    {
        // Setup test fixture.
        provider.create( newSlot() );
        time.advance( Duration.ofMinutes( 3 ) );
        provider.create( newSlot() );
        time.advance( Duration.ofMinutes( 3 ) );

        // Execute system under test.
        provider.create( newSlot() );

        // Verify result.
        assertEquals( "Only the slot created six minutes ago should have been removed, leaving the one created three minutes ago and the new one.", 2, provider.size() );
    }

    /**
     * Verifies that expired slots are also removed when a slot is consumed, not only when one is created.
     */
    @Test
    public void whenSlotIsConsumed_ThenExpiredSlotsAreRemoved()
    {
        // Setup test fixture.
        provider.create( newSlot() );
        provider.create( newSlot() );
        time.advance( Duration.ofMinutes( 5 ) );

        // Execute system under test.
        provider.consume( SecureUUID.generate() );

        // Verify result.
        assertEquals( "Consuming a slot should remove the expired slots that were never consumed.", 0, provider.size() );
    }

    /**
     * Verifies that a slot is handed out only once, even when many threads try to consume it at the same moment.
     */
    @Test
    public void whenSlotIsConsumedConcurrently_ThenItIsHandedOutOnlyOnce() throws Exception
    {
        // Setup test fixture.
        final int runs = 100;
        final int threads = 16;
        final ExecutorService executor = Executors.newFixedThreadPool( threads );
        final List<Integer> runsWithWrongCount = new ArrayList<>();

        try
        {
            // Execute system under test.
            for ( int run = 0; run < runs; run++ )
            {
                final Slot slot = newSlot();
                provider.create( slot );

                final CountDownLatch start = new CountDownLatch( 1 );
                final List<Future<Slot>> results = new ArrayList<>();
                for ( int i = 0; i < threads; i++ )
                {
                    results.add( executor.submit( () -> {
                        start.await();
                        return provider.consume( slot.getUuid() );
                    } ) );
                }
                start.countDown();

                int handedOut = 0;
                for ( final Future<Slot> result : results )
                {
                    if ( result.get( 10, TimeUnit.SECONDS ) != null )
                    {
                        handedOut++;
                    }
                }
                if ( handedOut != 1 )
                {
                    runsWithWrongCount.add( handedOut );
                }
            }
        }
        finally
        {
            executor.shutdownNow();
        }

        // Verify result.
        assertTrue( "When " + threads + " threads consume the same slot at once, exactly one should get it. In " + runsWithWrongCount.size() + " of " + runs + " runs, the slot was handed out this many times instead: " + runsWithWrongCount, runsWithWrongCount.isEmpty() );
    }

    /**
     * A time source that behaves like {@link System#nanoTime()}, but only moves when told to.
     */
    private static class FakeNanoTime implements LongSupplier
    {
        private volatile long now;

        /**
         * Creates a time source.
         *
         * @param start the value that the time source returns until it is advanced.
         */
        FakeNanoTime( final long start )
        {
            now = start;
        }

        /**
         * Moves the time source forward. Not safe to call from several threads at once.
         *
         * @param duration the amount of time to move forward by.
         */
        void advance( final Duration duration )
        {
            now += duration.toNanos();
        }

        @Override
        public long getAsLong()
        {
            return now;
        }
    }
}
