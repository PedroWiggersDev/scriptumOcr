package br.com.scriptum.configuracao;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class TestExtensaoLogExecucao
        implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

    @Override
    public void beforeTestExecution(@NonNull ExtensionContext context) {
        System.out.printf("[%s] Iniciando teste: %s%n", tipoDoTeste(context),
                context.getDisplayName());
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        String resultado = context.getExecutionException().isPresent()
                ? "FALHOU"
                : "CONCLUÍDO";
        System.out.printf("[%s] Teste %s: %s%n", tipoDoTeste(context), resultado,
                context.getDisplayName());
    }

    private static String tipoDoTeste(ExtensionContext context) {
        return context.getTags().contains("integration") ? "INTEGRAÇÃO" : "UNITÁRIO";
    }
}
