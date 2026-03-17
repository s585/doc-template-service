package ru.sberbank.sbercrm.doctemplate.template.constant.source;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValueSourceJsonKinds {
    public static final String DIRECT = "DIRECT";
    public static final String REFERENCE = "REFERENCE";
    public static final String CONSTANT = "CONSTANT";
}
