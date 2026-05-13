package br.com.terreiroreisebastiao.shared.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Componente de criptografia e hashing determinístico para proteção de PII (LGPD).
 *
 * <p>Utiliza AES-GCM-256 para criptografia autenticada em repouso e HMAC-SHA256
 * para geração de hash determinístico de lookup.</p>
 *
 * <h3>Responsabilidades:</h3>
 * <ul>
 *   <li>{@link #criptografar(String)} — Criptografa texto puro (nome, email, telefone)</li>
 *   <li>{@link #descriptografar(String)} — Descriptografa ciphertext armazenado no banco</li>
 *   <li>{@link #gerarHashParaBusca(String)} — Gera hash determinístico HMAC-SHA256 para lookup de email</li>
 * </ul>
 *
 * <h3>Versionamento de chave:</h3>
 * <p>O ciphertext é prefixado com {@code kv:1:} para permitir rotação futura de chave
 * sem quebrar dados já criptografados.</p>
 *
 * @see PiiAttributeConverter
 */
@Component
public class PiiCipher {

    private static final String PREFIXO_VERSAO = "kv:1:";
    private static final String ALGORITMO_AES = "AES";
    private static final String TRANSFORMACAO_AES_GCM = "AES/GCM/NoPadding";
    private static final String ALGORITMO_HMAC = "HmacSHA256";
    private static final int TAMANHO_CHAVE_AES_256_BYTES = 32;
    private static final int TAMANHO_IV_BYTES = 12;
    private static final int TAMANHO_TAG_BITS = 128;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKeySpec chaveAes;
    private final SecretKeySpec chaveAesLegado;
    private final byte[] saltHash;

    /**
     * Inicializa o componente com as chaves de ambiente.
     *
     * @param chaveBase64   chave mestra AES-GCM-256 em Base64, provida por {@code PII_SECRET_KEY}
     * @param saltHashTexto salt estático para HMAC-SHA256
     */
    public PiiCipher(
            @Value("${pii.key}") String chaveBase64,
            @Value("${pii.legacy-key:}") String chaveLegadoBase64,
            @Value("${pii.hash-salt}") String saltHashTexto
    ) {
        this.chaveAes = criarChave(chaveBase64, "PII_SECRET_KEY");
        this.chaveAesLegado = (chaveLegadoBase64 == null || chaveLegadoBase64.isBlank())
                ? null
                : criarChave(chaveLegadoBase64, "PII_LEGACY_KEY");
        this.saltHash = saltHashTexto.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Criptografa um texto puro usando AES-GCM-256.
     *
     * <p>O ciphertext resultante é codificado em Base64 e prefixado com a versão
     * da chave ({@code kv:1:}) para suportar rotação futura.</p>
     *
     * @param textoPuro texto a ser criptografado (nome, email ou telefone)
     * @return ciphertext no formato {@code kv:1:<base64>}, ou {@code null} se a entrada for nula/vazia
     * @throws CryptoException se a criptografia falhar
     */
    public String criptografar(String textoPuro) {
        if (textoPuro == null || textoPuro.isBlank()) {
            return null;
        }

        try {
            byte[] iv = new byte[TAMANHO_IV_BYTES];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMACAO_AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, chaveAes, new GCMParameterSpec(TAMANHO_TAG_BITS, iv));

            byte[] cifrado = cipher.doFinal(textoPuro.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(iv.length + cifrado.length)
                    .put(iv)
                    .put(cifrado)
                    .array();

            String base64 = Base64.getEncoder().encodeToString(payload);
            return PREFIXO_VERSAO + base64;
        } catch (GeneralSecurityException excecao) {
            throw new CryptoException("Falha ao criptografar dado PII.", excecao);
        }
    }

    /**
     * Descriptografa um ciphertext previamente criptografado por {@link #criptografar(String)}.
     *
     * @param textoCifrado ciphertext no formato {@code kv:1:<base64>}
     * @return texto puro original, ou {@code null} se a entrada for nula/vazia
     * @throws CryptoException se o prefixo de versão for desconhecido ou a descriptografia falhar
     */
    public String descriptografar(String textoCifrado) {
        if (textoCifrado == null || textoCifrado.isBlank()) {
            return null;
        }

        if (!textoCifrado.startsWith(PREFIXO_VERSAO)) {
            throw new CryptoException(
                    "Prefixo de versão desconhecido no ciphertext. "
                            + "Esperado: '" + PREFIXO_VERSAO + "'."
            );
        }

        try {
            String base64 = textoCifrado.substring(PREFIXO_VERSAO.length());
            byte[] payload = Base64.getDecoder().decode(base64);
            if (payload.length <= TAMANHO_IV_BYTES) {
                throw new CryptoException("Ciphertext PII inválido: payload menor que o IV esperado.");
            }

            try {
                return descriptografarComChave(payload, chaveAes);
            } catch (GeneralSecurityException excecaoPrincipal) {
                if (chaveAesLegado != null) {
                    try {
                        return descriptografarComChave(payload, chaveAesLegado);
                    } catch (GeneralSecurityException ignored) {
                        // Mantem a excecao principal para indicar que o dado nao pode ser lido pela chave ativa.
                    }
                }

                throw new CryptoException("Falha ao descriptografar dado PII.", excecaoPrincipal);
            }
        } catch (IllegalArgumentException excecao) {
            throw new CryptoException("Ciphertext PII inválido: Base64 malformado.", excecao);
        }
    }

    /**
     * Gera um hash determinístico HMAC-SHA256 para busca de email sem decifrar a tabela.
     *
     * <p>O email é sanitizado ({@code trim + toLowerCase}) antes do hashing.
     * O resultado é uma string hexadecimal de 64 caracteres, persistida na coluna
     * {@code email_lookup_hash} da tabela {@code customer}.</p>
     *
     * @param email email puro fornecido pelo consulente
     * @return hash hexadecimal de 64 caracteres
     * @throws CryptoException se o cálculo do HMAC falhar
     */
    public String gerarHashParaBusca(String email) {
        if (email == null || email.isBlank()) {
            throw new CryptoException("Email não pode ser nulo ou vazio para gerar hash de busca.");
        }

        try {
            String emailSanitizado = email.trim().toLowerCase();
            Mac mac = Mac.getInstance(ALGORITMO_HMAC);
            mac.init(new SecretKeySpec(saltHash, ALGORITMO_HMAC));
            byte[] hashBytes = mac.doFinal(emailSanitizado.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception excecao) {
            throw new CryptoException("Falha ao gerar hash HMAC-SHA256 para lookup.", excecao);
        }
    }

    private SecretKeySpec criarChave(String chaveBase64, String nomeVariavel) {
        byte[] chaveBytes;
        try {
            chaveBytes = Base64.getDecoder().decode(chaveBase64);
        } catch (IllegalArgumentException excecao) {
            throw new CryptoException(nomeVariavel + " inválida: valor Base64 malformado.", excecao);
        }

        if (chaveBytes.length != TAMANHO_CHAVE_AES_256_BYTES) {
            throw new CryptoException(
                    nomeVariavel + " inválida: a chave AES-GCM-256 deve ter exatamente 32 bytes após o Base64."
            );
        }

        return new SecretKeySpec(chaveBytes, ALGORITMO_AES);
    }

    private String descriptografarComChave(byte[] payload, SecretKeySpec chave) throws GeneralSecurityException {
        byte[] iv = Arrays.copyOfRange(payload, 0, TAMANHO_IV_BYTES);
        byte[] cifrado = Arrays.copyOfRange(payload, TAMANHO_IV_BYTES, payload.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMACAO_AES_GCM);
        cipher.init(Cipher.DECRYPT_MODE, chave, new GCMParameterSpec(TAMANHO_TAG_BITS, iv));

        byte[] decifrado = cipher.doFinal(cifrado);
        return new String(decifrado, StandardCharsets.UTF_8);
    }
}
