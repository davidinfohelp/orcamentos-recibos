package com.empresa.indicadores;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import java.awt.Desktop;
import java.net.URI;

@SpringBootApplication
public class App {
  private final Environment env;
  public App(Environment env){ this.env = env; }
  public static void main(String[] a){
    System.setProperty("java.awt.headless", "false");
    SpringApplication.run(App.class, a);
  }
  /** Abre o navegador sozinho quando o programa termina de iniciar. */
  @EventListener(ApplicationReadyEvent.class)
  public void abrirNavegador(){
    String url = "http://localhost:" + env.getProperty("local.server.port", "8765");
    System.out.println("\n  Programa de Indicadores pronto: " + url + "\n  (deixe esta janela aberta; feche-a para encerrar)\n");
    if ("false".equals(env.getProperty("abrir.navegador"))) return;
    try {
      if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) { Desktop.getDesktop().browse(new URI(url)); return; }
    } catch (Throwable ignored) { }   // sem tela gráfica: segue sem abrir
    try {                             // plano B no Windows
      if (System.getProperty("os.name", "").toLowerCase().contains("win")) new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
    } catch (Throwable ignored) { }
  }
}
