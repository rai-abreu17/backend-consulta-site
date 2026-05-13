package br.com.terreiroreisebastiao.notification.service;

import br.com.terreiroreisebastiao.booking.domain.Booking;
import br.com.terreiroreisebastiao.catalog.domain.Modalidade;
import br.com.terreiroreisebastiao.notification.domain.EmailOutboxEvent;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class EmailService {

    private static final Locale LOCALE_PT_BR = Locale.of("pt", "BR");
    private static final ZoneId ZONE_ID_FORTALEZA = ZoneId.of("America/Fortaleza");
    private static final DateTimeFormatter FORMATTER_DATA = DateTimeFormatter.ofPattern("dd 'de' MMMM 'às' HH:mm", LOCALE_PT_BR);

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${notification.email.from:no-reply@terreiro.local}")
    private String remetente;

    @Value("${notification.terreiro.endereco:Morada do Terreiro a configurar}")
    private String enderecoTerreiro;

    @Value("${notification.terreiro.maps-url:https://maps.google.com/?q=Terreiro+Rei+Sebastiao}")
    private String mapsUrlTerreiro;

    @Value("${notification.terreiro.link-online:https://meet.google.com/terreiro-rei-sebastiao}")
    private String linkAtendimentoOnline;

    public EmailService(JavaMailSender javaMailSender,
                        SpringTemplateEngine templateEngine) {
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
    }

    public void enviar(EmailOutboxEvent evento) {
        Booking booking = evento.getAgendamento();
        String destinatario = booking.getConsulente().getEmailDec();
        if (!StringUtils.hasText(destinatario)) {
            throw new IllegalStateException("O booking não possui e-mail válido para notificação.");
        }

        EmailPayload payload = resolverPayload(evento.getTipoEvento(), booking);
        Context context = criarContexto(booking);
        String html = templateEngine.process(payload.template(), context);

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );
            helper.setFrom(remetente);
            helper.setTo(destinatario);
            helper.setSubject(payload.assunto());
            helper.setText(html, true);
            javaMailSender.send(mimeMessage);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao enviar e-mail transacional.", ex);
        }
    }

    public String resolverTemplate(String tipoEvento) {
        return resolverPayload(tipoEvento, null).template();
    }

    private Context criarContexto(Booking booking) {
        Context context = new Context(LOCALE_PT_BR);
        String modalidade = booking.getModalidade().name();
        String modalidadeTexto = booking.getModalidade() == Modalidade.ONLINE ? "Online" : "Presencial";
        String localizacaoTitulo = booking.getModalidade() == Modalidade.ONLINE ? "Link do atendimento" : "Morada do atendimento";
        String localizacaoValor = booking.getModalidade() == Modalidade.ONLINE ? linkAtendimentoOnline : enderecoTerreiro;
        String localizacaoUrl = booking.getModalidade() == Modalidade.ONLINE ? linkAtendimentoOnline : mapsUrlTerreiro;

        context.setVariable("nome_consulente", booking.getConsulente().getNomeCompleto());
        context.setVariable("nome_servico", booking.getServico().getNome());
        context.setVariable("data_hora_formatada", booking.getInicioEm().atZoneSameInstant(ZONE_ID_FORTALEZA).format(FORMATTER_DATA));
        context.setVariable("modalidade", modalidade);
        context.setVariable("modalidade_texto", modalidadeTexto);
        context.setVariable("localizacao_titulo", localizacaoTitulo);
        context.setVariable("localizacao_valor", localizacaoValor);
        context.setVariable("localizacao_url", localizacaoUrl);
        context.setVariable("valor_formatado", formatarMoeda(booking.getPrecoCents()));
        return context;
    }

    private EmailPayload resolverPayload(String tipoEvento, Booking booking) {
        String nomeServico = booking != null ? booking.getServico().getNome() : "Consulta";
        String tipoNormalizado = tipoEvento == null ? "" : tipoEvento.trim().toUpperCase(LOCALE_PT_BR);
        return switch (tipoNormalizado) {
            case NotificationService.EVENTO_CONFIRMED -> new EmailPayload(
                    "email/confirmed",
                    "Agendamento Confirmado: " + nomeServico
            );
            case NotificationService.EVENTO_REMINDER_24H -> new EmailPayload(
                    "email/reminder_24h",
                    "Lembrete do seu atendimento em 24 horas: " + nomeServico
            );
            default -> throw new IllegalArgumentException("Tipo de evento de e-mail não suportado: " + tipoEvento);
        };
    }

    private String formatarMoeda(Long valorCents) {
        if (valorCents == null) {
            return "R$ 0,00";
        }
        BigDecimal valor = BigDecimal.valueOf(valorCents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return NumberFormat.getCurrencyInstance(LOCALE_PT_BR).format(valor);
    }

    private record EmailPayload(String template, String assunto) {
    }
}