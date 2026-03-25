package ru.sberbank.sbercrm.doctemplate.template.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TemplateApiConstants {

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ExpressionJsonTypes {
        public static final String OPERATION = "operation";
        public static final String PRIMITIVE = "primitive";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ValueSourceJsonKinds {
        public static final String DIRECT = "DIRECT";
        public static final String REFERENCE = "REFERENCE";
        public static final String CONSTANT = "CONSTANT";
    }
}
