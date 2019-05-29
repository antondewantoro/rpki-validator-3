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
package net.ripe.rpki.validator3.storage.stores;

import net.ripe.rpki.commons.crypto.CertificateRepositoryObject;
import net.ripe.rpki.commons.crypto.cms.manifest.ManifestCms;
import net.ripe.rpki.commons.validation.ValidationResult;
import net.ripe.rpki.validator3.storage.data.Key;
import net.ripe.rpki.validator3.storage.data.RpkiObject;
import net.ripe.rpki.validator3.storage.lmdb.LmdbTx;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Stream;

public interface RpkiObjects extends GenericStore<RpkiObject> {
    Optional<RpkiObject> get(LmdbTx.Read tx, Key key);

    void put(LmdbTx.Write tx, RpkiObject rpkiObject);

    void put(LmdbTx.Write tx, RpkiObject rpkiObject, String location);

    void delete(LmdbTx.Write tx, RpkiObject o);

    void markReachable(LmdbTx.Write tx, Key pk, Instant i);

    void addLocation(LmdbTx.Write tx, Key pk, String location);

    SortedSet<String> getLocations(LmdbTx.Read tx, Key pk);

    void deleteLocation(LmdbTx.Write tx, Key key, String uri);

    <T extends CertificateRepositoryObject> Optional<T> findCertificateRepositoryObject(
            LmdbTx.Read tx, Key sha256, Class<T> clazz, ValidationResult validationResult);

    Optional<RpkiObject> findBySha256(LmdbTx.Read tx, byte[] sha256);

    Map<String, RpkiObject> findObjectsInManifest(LmdbTx.Read tx, ManifestCms manifestCms);

    Optional<RpkiObject> findLatestMftByAKI(LmdbTx.Read tx, byte[] authorityKeyIdentifier);

    long deleteUnreachableObjects(LmdbTx.Write tx, Instant unreachableSince);

    Stream<byte[]> streamObjects(LmdbTx.Read tx, RpkiObject.Type type);

    Set<Key> getPkByType(LmdbTx.Read tx, RpkiObject.Type type);

    void verify(LmdbTx.Read tx);
}