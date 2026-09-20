package br.com.rpe.cartao.adapters.out.http;

import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class ProdutoAuthInterceptor implements ClientHttpRequestInterceptor {

  private final EmissorTokenServico emissorTokenServico;

  public ProdutoAuthInterceptor(EmissorTokenServico emissorTokenServico) {
    this.emissorTokenServico = emissorTokenServico;
  }

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    request.getHeaders().setBearerAuth(emissorTokenServico.gerar());
    return execution.execute(request, body);
  }
}
