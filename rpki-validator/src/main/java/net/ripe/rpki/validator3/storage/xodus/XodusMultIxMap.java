/**
 * The BSD License
 *
 * Copyright (c) 2010-2018 RIPE NCC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   - Neither the name of the RIPE NCC nor the names of its contributors may be
 *     used to endorse or promote products derived from this software without
 *     specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.ripe.rpki.validator3.storage.xodus;

import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.StoreConfig;
import net.ripe.rpki.validator3.storage.Bytes;
import net.ripe.rpki.validator3.storage.MultIxMap;
import net.ripe.rpki.validator3.storage.Tx;
import net.ripe.rpki.validator3.storage.data.Key;
import net.ripe.rpki.validator3.storage.encoding.Coder;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class XodusMultIxMap<T extends Serializable> extends XodusIxBase<T> implements MultIxMap<T> {

    public XodusMultIxMap(final Xodus xodus,
                          final String name,
                          final Coder<T> coder) {
        super(xodus, name, coder);
    }

    protected StoreConfig getStoreConfig() {
        return StoreConfig.WITH_DUPLICATES;
    }

    @Override
    public List<T> get(Tx.Read tx, Key primaryKey) {
        verifyKey(primaryKey);
        final List<T> result = new ArrayList<>();
        try (Cursor cursor = getMainDb().openCursor(castTxn(tx))) {
            ByteIterable startKey = cursor.getSearchKey(primaryKey.toByteIterable());
            if (startKey != null) {
                result.add(getValue(primaryKey, Bytes.toBytes(cursor.getValue())));
                while (cursor.getNextDup()) {
                    result.add(getValue(primaryKey, Bytes.toBytes(cursor.getValue())));
                }
            }
        }
        return result;
    }

    @Override
    public int count(Tx.Read tx, Key primaryKey) {
        verifyKey(primaryKey);
        int s = 0;
        try (Cursor cursor = getMainDb().openCursor(castTxn(tx))) {
            ByteIterable startKey = cursor.getSearchKey(primaryKey.toByteIterable());
            if (startKey != null) {
                s++;
                while (cursor.getNextDup()) {
                    s++;
                }
            }
        }
        return s;
    }

    @Override
    public void put(Tx.Write tx, Key primaryKey, T value) {
        checkKeyAndValue(primaryKey, value);
        getMainDb().put(castTxn(tx), primaryKey.toByteIterable(), valueWithChecksum(value));
    }

    @Override
    public void delete(Tx.Write tx, Key primaryKey) {
        getMainDb().delete(castTxn(tx), primaryKey.toByteIterable());
    }

    @Override
    public void delete(Tx.Write tx, Key primaryKey, T value) {
        verifyKey(primaryKey);
        try (Cursor c = getMainDb().openCursor(castTxn(tx))) {
            if (c.getSearchBoth(primaryKey.toByteIterable(), valueWithChecksum(value))) {
                c.deleteCurrent();
            }
        }
    }

    @Override
    public void deleteBatch(Tx.Write tx, List<Pair<Key, T>> toDelete) {
        try (Cursor c = getMainDb().openCursor(castTxn(tx))) {
            toDelete.forEach(p -> {
                if (c.getSearchBoth(p.getKey().toByteIterable(), valueWithChecksum(p.getValue()))) {
                    c.deleteCurrent();
                }
            });
        }
    }

    @Override
    public T toValue(byte[] bb) {
        return getValue(null, bb);
    }

    @Override
    public boolean exists(Tx.Read tx, Key pk, T value) {
        try (Cursor c = getMainDb().openCursor(castTxn(tx))) {
            return c.getSearchBoth(pk.toByteIterable(), valueWithChecksum(value));
        }
    }
}
