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
package net.ripe.rpki.validator3.adapter.jpa;

import net.ripe.rpki.validator3.domain.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.Optional;

import static net.ripe.rpki.validator3.adapter.jpa.querydsl.QSetting.setting;


@Component
@Transactional(Transactional.TxType.REQUIRED)
public class JPASettings extends JPARepository<Setting> implements Settings {
    private static final String PRECONFIGURED_TAL_SETTINGS_KEY = "internal.preconfigured.tals.loaded";
    private static final String INITIAL_VALIDATION_RUN_COMPLETED = "internal.initial.validation.run.completed";

    @Autowired
    private EntityManager entityManager;

    protected JPASettings() {
        super(setting);
    }

    private Optional<String> get(String key) {
        return Optional.ofNullable(select().where(setting.key.eq(key)).fetchFirst()).map(Setting::getValue);
    }

    private void put(String key, String value) {
        Setting existing = select().where(setting.key.eq(key)).fetchFirst();
        if (existing == null) {
            entityManager.persist(new Setting(key, value));
        } else {
            existing.setValue(value);
        }
    }

    @Override
    public void markPreconfiguredTalsLoaded() {
        put(PRECONFIGURED_TAL_SETTINGS_KEY, "true");
    }

    @Override
    public boolean isPreconfiguredTalsLoaded() {
        return get(PRECONFIGURED_TAL_SETTINGS_KEY).orElse("false").equals("true");
    }

    @Override
    public void markInitialValidationRunCompleted() {
        put(INITIAL_VALIDATION_RUN_COMPLETED, "true");

    }

    @Override
    public boolean isInitialValidationRunCompleted() {
        return get(INITIAL_VALIDATION_RUN_COMPLETED).orElse("false").equals("true");
    }
}
