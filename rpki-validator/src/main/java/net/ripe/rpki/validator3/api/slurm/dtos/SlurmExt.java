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
package net.ripe.rpki.validator3.api.slurm.dtos;

import fj.P;
import lombok.Data;
import net.ripe.ipresource.Asn;
import net.ripe.ipresource.IpRange;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * The same as Slurm but also stores ids to ease the use of REST API.
 */
@Data
public class SlurmExt {

    private Map<Long, Slurm.SlurmPrefixFilter> prefixFilters = new HashMap<>();

    private Map<Long, Slurm.SlurmBgpSecFilter> bgpsecFilters = new HashMap<>();

    private Map<Long, Slurm.SlurmPrefixAssertion> prefixAssertions = new HashMap<>();

    private Map<Long, Slurm.SlurmBgpSecAssertion> bgpsecAssertions = new HashMap<>();

    public Slurm toSlurm() {
        Slurm slurm = new Slurm();
        slurm.setLocallyAddedAssertions(new Slurm.SlurmLocallyAddedAssertions(
                extract(prefixAssertions, slurmPrefixAssertionComparator),
                extract(bgpsecAssertions, slurmBgpSecAssertionComparator)));
        slurm.setValidationOutputFilters(new Slurm.SlurmOutputFilters(
                extract(prefixFilters, slurmPrefixFilterComparator),
                extract(bgpsecFilters, slurmBgpSecFilterComparator)));
        return slurm;
    }

    private static <T> List<T> extract(Map<Long, T> s, Comparator<T> comparator) {
        return s.values().stream().sorted(comparator).collect(Collectors.toList());
    }

    private static <T> Map<Long, T> addIds(List<T> s, AtomicLong idSeq, Comparator<T> comparator) {
        return s.stream()
                .sorted(comparator)
                .map(v -> Pair.of(idSeq.getAndIncrement(), v))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    private static <T extends Comparable<T>> int compareWithNulls(T asn1, T asn2) {
        if (asn1 == null) {
            return asn2 == null ? 0 : 1;
        }
        return asn2 == null ? -1 : asn1.compareTo(asn2);
    }

    private static Comparator<Slurm.SlurmPrefixFilter> slurmPrefixFilterComparator =
        ((Comparator<Slurm.SlurmPrefixFilter>) (o1, o2) -> compareWithNulls(o1.getAsn(), o2.getAsn()))
            .thenComparing((o1, o2) -> compareWithNulls(o1.getPrefix(), o2.getPrefix()));

    private static Comparator<Slurm.SlurmBgpSecFilter> slurmBgpSecFilterComparator =
        ((Comparator<Slurm.SlurmBgpSecFilter>) (o1, o2) -> compareWithNulls(o1.getAsn(), o2.getAsn()))
            .thenComparing((o1, o2) -> compareWithNulls(o1.getSki(), o2.getSki()));

    private static Comparator<Slurm.SlurmPrefixAssertion> slurmPrefixAssertionComparator =
        ((Comparator<Slurm.SlurmPrefixAssertion>) (o1, o2) -> compareWithNulls(o1.getAsn(), o2.getAsn()))
            .thenComparing((o1, o2) -> compareWithNulls(o1.getPrefix(), o2.getPrefix()))
            .thenComparing((o1, o2) -> compareWithNulls(o1.getMaxPrefixLength(), o2.getMaxPrefixLength()));

    private static Comparator<Slurm.SlurmBgpSecAssertion> slurmBgpSecAssertionComparator =
        ((Comparator<Slurm.SlurmBgpSecAssertion>) (o1, o2) -> compareWithNulls(o1.getAsn(), o2.getAsn()))
            .thenComparing((o1, o2) -> compareWithNulls(o1.getPublicKey(), o2.getPublicKey()))
            .thenComparing((o1, o2) -> compareWithNulls(o1.getSki(), o2.getSki()));

    public static SlurmExt fromSlurm(Slurm slurm, AtomicLong idSeq) {
        final SlurmExt slurmExt = new SlurmExt();

        // always sort SLURM entries so that they have stable IDs
        slurmExt.setPrefixFilters(addIds(slurm.getValidationOutputFilters().getPrefixFilters(), idSeq, slurmPrefixFilterComparator));
        slurmExt.setBgpsecFilters(addIds(slurm.getValidationOutputFilters().getBgpsecFilters(), idSeq, slurmBgpSecFilterComparator));
        slurmExt.setPrefixAssertions(addIds(slurm.getLocallyAddedAssertions().getPrefixAssertions(), idSeq, slurmPrefixAssertionComparator));
        slurmExt.setBgpsecAssertions(addIds(slurm.getLocallyAddedAssertions().getBgpsecAssertions(), idSeq, slurmBgpSecAssertionComparator));
        return slurmExt;
    }

    public SlurmExt copy() {
        // we never change the elements in place,
        // so it's fine to shallow copy the lists
        final SlurmExt slurmExt = new SlurmExt();
        slurmExt.setPrefixFilters(new HashMap<>(prefixFilters));
        slurmExt.setBgpsecFilters(new HashMap<>(bgpsecFilters));
        slurmExt.setPrefixAssertions(new HashMap<>(prefixAssertions));
        slurmExt.setBgpsecAssertions(new HashMap<>(bgpsecAssertions));
        return slurmExt;
    }
}
