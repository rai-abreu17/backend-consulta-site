package br.com.terreiroreisebastiao.shared.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * Conversor JPA que criptografa/descriptografa automaticamente campos PII
 * ao persistir e carregar entidades do banco de dados.
 *
 * <p>Campos anotados com {@code @Convert(converter = PiiAttributeConverter.class)}
 * passam automaticamente pelo {@link PiiCipher} sem que a camada de negócio
 * precise se preocupar com a criptografia.</p>
 *
 * <h3>Uso na entidade:</h3>
 * <pre>{@code
 * @Convert(converter = PiiAttributeConverter.class)
 * @Column(name = "full_name_enc", nullable = false)
 * private String nomeCompleto;
 * }</pre>
 *
 * @see PiiCipher
 */
@Converter(autoApply = false)
@Component
public class PiiAttributeConverter implements AttributeConverter<String, String> {

    private final PiiCipher piiCipher;

    public PiiAttributeConverter(PiiCipher piiCipher) {
        this.piiCipher = piiCipher;
    }

    /**
     * Criptografa o valor do atributo antes de persistir no banco.
     *
     * @param atributo valor em texto puro da entidade em memória
     * @return ciphertext AES-GCM-256 em base64, prefixado com versão da chave
     */
    @Override
    public String convertToDatabaseColumn(String atributo) {
        return piiCipher.criptografar(atributo);
    }

    /**
     * Descriptografa o valor do banco ao carregar a entidade em memória.
     *
     * @param dadoBanco ciphertext armazenado no banco
     * @return texto puro original
     */
    @Override
    public String convertToEntityAttribute(String dadoBanco) {
        return piiCipher.descriptografar(dadoBanco);
    }
}
