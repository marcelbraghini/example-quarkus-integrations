package br.com.marcelbraghini.usecases.endereco;

import br.com.marcelbraghini.entities.exceptions.EnderecoErpException;
import br.com.marcelbraghini.infrastructure.correios.atendecliente.AtendeCliente;
import br.com.marcelbraghini.infrastructure.correios.atendecliente.EnderecoERP;
import io.quarkiverse.cxf.annotation.CXFClient;
import org.jboss.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.message.Message;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static java.lang.String.format;

@ApplicationScoped
public class EnderecoUsecase {

    @Inject
    @CXFClient("correios-soap")
    AtendeCliente atendeCliente;

    private static final Logger logger = Logger.getLogger(EnderecoUsecase.class);

    public EnderecoERP getEnderecoERP(final String cep) {
        try {
            Client client = null;
            try {
                client = ClientProxy.getClient(atendeCliente);
            } catch (IllegalArgumentException iae) {
                client = null;
            }
            if (client != null) {
                client.getInInterceptors().add(new RawSoapInInterceptor());
            }

            return atendeCliente.consultaCEP(cep);
        } catch (Exception e) {
            throw new EnderecoErpException(e, cep);
        }
    }

    static class RawSoapInInterceptor extends AbstractPhaseInterceptor<Message> {
        RawSoapInInterceptor() {
            super(Phase.RECEIVE);
        }

        @Override
        public void handleMessage(Message message) throws Fault {
            try {
                InputStream is = message.getContent(InputStream.class);
                if (is == null) {
                    return;
                }
                byte[] bytes = is.readAllBytes();
                String xml = new String(bytes, StandardCharsets.UTF_8);

                logger.info(format("[EnderecoUsecase:handleMessage] XML response: %s", xml));

                message.setContent(InputStream.class, new ByteArrayInputStream(bytes));
            } catch (Exception ex) {
                throw new Fault(ex);
            }
        }
    }
}
