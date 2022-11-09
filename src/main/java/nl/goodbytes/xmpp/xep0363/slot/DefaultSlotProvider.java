/*
 * Copyright (c) 2022 Guus der Kinderen. All rights reserved.
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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import nl.goodbytes.xmpp.xep0363.SecureUniqueId;
import nl.goodbytes.xmpp.xep0363.Slot;
import nl.goodbytes.xmpp.xep0363.SlotProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

// TODO: quick 'n dirty singleton. Is this the best choice?
// TODO: persist internal state to allow for restart survival.
public class DefaultSlotProvider implements SlotProvider
{
    private final Cache<SecureUniqueId, Slot> slots;
    private long putExpiryValue = 5;
    private TimeUnit putExpiryUnit = TimeUnit.MINUTES;

    public DefaultSlotProvider()
    {
        slots = CacheBuilder.newBuilder()
            .expireAfterWrite( putExpiryValue, putExpiryUnit )
            .build();
    }

    @Override
    public void create(@Nonnull Slot slot)
    {
        slots.put( slot.getUuid(), slot );
    }

    @Override
    @Nullable
    public Slot consume(@Nonnull SecureUniqueId uuid)
    {
        final Slot slot = slots.getIfPresent( uuid );
        if ( slot != null )
        {
            slots.invalidate( uuid );
        }
        return slot;
    }
}
