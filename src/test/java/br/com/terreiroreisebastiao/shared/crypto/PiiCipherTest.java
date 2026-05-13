package br.com.terreiroreisebastiao.shared.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class PiiCipherTest {

    private static final String CHAVE_AES_256_BASE64 = Base64.getEncoder()
            .encodeToString("0123456789ABCDEF0123456789ABCDEF".getBytes(StandardCharsets.UTF_8));
    private static final String CHAVE_AES_256_ALTERNATIVA_BASE64 = Base64.getEncoder()
            .encodeToString("FEDCBA9876543210FEDCBA9876543210".getBytes(StandardCharsets.UTF_8));

    @Test
    void deveCriptografarEDescriptografarComChaveBase64De32Bytes() {
        PiiCipher piiCipher = new PiiCipher(CHAVE_AES_256_BASE64, "", "salt-dev");

        String textoPlano = "Fulano de Tal";
        String textoCifrado = piiCipher.criptografar(textoPlano);

        assertThat(textoCifrado)
                .isNotBlank()
                .startsWith("kv:1:")
                .isNotEqualTo(textoPlano);
        assertThat(piiCipher.descriptografar(textoCifrado)).isEqualTo(textoPlano);
    }

    @Test
    void deveGerarHashDeterministicoParaLookup() {
        PiiCipher piiCipher = new PiiCipher(CHAVE_AES_256_BASE64, "", "salt-dev");

        String hashOriginal = piiCipher.gerarHashParaBusca(" Teste@Email.com ");
        String hashNormalizado = piiCipher.gerarHashParaBusca("teste@email.com");

        assertThat(hashOriginal)
                .hasSize(64)
                .isEqualTo(hashNormalizado);
    }

    @Test
    void deveDescriptografarComChaveLegadaQuandoConfigurada() {
        PiiCipher cipherLegado = new PiiCipher(CHAVE_AES_256_BASE64, "", "salt-dev");
        String textoCifrado = cipherLegado.criptografar("Fulano de Tal");

        PiiCipher piiCipher = new PiiCipher(CHAVE_AES_256_ALTERNATIVA_BASE64, CHAVE_AES_256_BASE64, "salt-dev");

        assertThat(piiCipher.descriptografar(textoCifrado)).isEqualTo("Fulano de Tal");
    }
}