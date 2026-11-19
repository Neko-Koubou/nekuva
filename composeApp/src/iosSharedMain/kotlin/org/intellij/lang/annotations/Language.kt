package org.intellij.lang.annotations

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.ANNOTATION_CLASS,
)
@Retention(AnnotationRetention.BINARY)
annotation class Language(
    val value: String,
    val prefix: String = "",
    val suffix: String = "",
)
