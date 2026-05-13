package br.com.terreiroreisebastiao.shared.crypto;

/**
 * Exceção de domínio para falhas de criptografia e hashing de PII.
 *
 * <p>Mapeada para {@code INTERNAL_ERROR} (500) no {@code GlobalExceptionHandler}.
 * Nunca deve vazar detalhes de criptografia para o frontend — apenas mensagens
 * padronizadas são expostas ao cliente.</p>
 */
public class CryptoException extends RuntimeException {

    public CryptoException(String mensagem) {
        super(mensagem);
    }

    public CryptoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
