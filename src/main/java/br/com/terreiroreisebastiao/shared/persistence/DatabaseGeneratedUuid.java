package br.com.terreiroreisebastiao.shared.persistence;

import org.hibernate.annotations.IdGeneratorType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um identificador UUID como gerado pelo PostgreSQL no momento do INSERT.
 */
@IdGeneratorType(DatabaseGeneratedUuidGenerator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface DatabaseGeneratedUuid {
}