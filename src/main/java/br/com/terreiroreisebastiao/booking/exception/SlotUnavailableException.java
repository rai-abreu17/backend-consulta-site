package br.com.terreiroreisebastiao.booking.exception;

import br.com.terreiroreisebastiao.shared.error.ApiException;
import br.com.terreiroreisebastiao.shared.error.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Exceção de domínio lançada quando o slot já está em reserva ou indisponível.
 */
public class SlotUnavailableException extends ApiException {

    public SlotUnavailableException(String detalhe) {
        super(ErrorCode.SLOT_ALREADY_TAKEN, HttpStatus.CONFLICT, detalhe);
    }

    public SlotUnavailableException(String detalhe, Throwable causa) {
        super(ErrorCode.SLOT_ALREADY_TAKEN, HttpStatus.CONFLICT, detalhe);
        initCause(causa);
    }
}