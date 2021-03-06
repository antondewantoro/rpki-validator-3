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
package net.ripe.rpki.validator3.api.bgp;

import io.swagger.annotations.*;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.ripe.ipresource.Asn;
import net.ripe.ipresource.IpRange;
import net.ripe.rpki.validator3.api.*;
import net.ripe.rpki.validator3.api.ApiResponse;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static net.ripe.rpki.validator3.api.ModelPropertyDescriptions.*;

@PublicApiCall
@RestController
@RequestMapping(path = "/api/bgp", produces = {ValidatorApi.API_MIME_TYPE, "application/json"})
@Slf4j
@Api(
        tags ="BGP preview"
)
public class BgpPreviewController {

    @Autowired
    private BgpPreviewService bgpPreviewService;

    @GetMapping(path = "/")
    public ResponseEntity<ApiResponse<Stream<BgpPreview>>> list(
            @RequestParam(name = "startFrom", defaultValue = "0") long startFrom,
            @RequestParam(name = "pageSize", defaultValue = "20") long pageSize,
            @RequestParam(name = "search", defaultValue = "", required = false) String searchString,
            @ApiParam(allowableValues = SORT_BY_ALLOWABLE_VALUES)
            @RequestParam(name = "sortBy", defaultValue = "prefix") String sortBy,
            @ApiParam(allowableValues = SORT_DIRECTION_ALLOWABLE_VALUES)
            @RequestParam(name = "sortDirection", defaultValue = "asc") String sortDirection
    ) {
        final SearchTerm searchTerm = StringUtils.isNotBlank(searchString) ? new SearchTerm(searchString) : null;
        final Sorting sorting = Sorting.parse(sortBy, sortDirection);
        final Paging paging = Paging.of(startFrom, pageSize);

        BgpPreviewService.BgpPreviewResult bgpPreviewResult = bgpPreviewService.find(searchTerm, sorting, paging);

        return ResponseEntity.ok(ApiResponse.<Stream<BgpPreview>>builder()
                .data(bgpPreviewResult.getData().map(entry -> BgpPreview.of(
                        entry.getOrigin().toString(),
                        entry.getPrefix().toString(),
                        entry.getValidity().name()
                )))
                .metadata(Metadata.of(bgpPreviewResult.getTotalCount(), bgpPreviewResult.getLastModified()))
                .build());
    }

    @GetMapping(path = "/validity")
    public ResponseEntity<ApiResponse<BgpPreviewService.BgpValidityWithFilteredResource>> validity(
            @RequestParam(name = "prefix") String prefix,
            @RequestParam(name = "asn") String asn
    ) {
        final BgpPreviewService.BgpValidityWithFilteredResource bgp = bgpPreviewService.validity(
                arg(() -> Asn.parse(asn)),
                arg(() -> IpRange.parse(prefix))
        );
        return ResponseEntity.ok(ApiResponse.<BgpPreviewService.BgpValidityWithFilteredResource>builder()
                .data(bgp)
                .metadata(Metadata.of(bgp.getValidatingRoas().size()))
                .build());
    }

    private static <T> T arg(Supplier<T> s) {
        try  {
            return s.get();
        } catch (Exception e) {
            throw new HttpMessageNotReadableException(e.getMessage());
        }
    }

    @Value(staticConstructor = "of")
    public static class BgpPreview {
        @ApiModelProperty(value = ASN_PREFIXED_PROPERTY, example = ASN_PREFIXED_EXAMPLE)
        private String asn;
        private String prefix;
        @ApiModelProperty(allowableValues = VALIDITY_ALLOWABLE_VALUES)
        private String validity;
    }
}
