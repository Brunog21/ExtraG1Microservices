package br.edu.atitus.currencyservice.controllers;

import br.edu.atitus.currencyservice.clients.BCBClient;
import br.edu.atitus.currencyservice.clients.BCBResponse;
import br.edu.atitus.currencyservice.entities.CurrencyEntity;
import br.edu.atitus.currencyservice.repositories.CurrencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/currency-service")
public class CurrencyController {

    @Autowired
    private Environment environment;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private BCBClient bcbClient;

    @Autowired
    private CacheManager cacheManager;

    @GetMapping("/{currency}")
    public ResponseEntity<CurrencyEntity> getCurrency(@PathVariable String currency) {

        var currencyEntity = currencyRepository.findByCurrency(currency)
                .orElseThrow(() -> new RuntimeException("Currency not found"));

        String port = environment.getProperty("local.server.port");
        String dataFixaCotacao = "10-25-2023"; // Data útil fixa
        String nameCache = "bcb-currency";
        Double cotacaoBcb = null;

        // Tenta buscar do Cache
        var cacheInfo = cacheManager.getCache(nameCache).get(currency);
        if (cacheInfo != null) {
            cotacaoBcb = (Double) cacheInfo.get();
            port += " - BCB in cache";
        } else {
            // Busca da API do BCB via Feign
            BCBResponse response = bcbClient.getCotacaoBcb(currency, dataFixaCotacao);

            // Verifica se a resposta não é nula (Se for nula, é porque caiu no Fallback)
            if (response != null && response.getValue() != null && !response.getValue().isEmpty()) {
                cotacaoBcb = response.getValue().get(0).getCotacaoVenda();
                cacheManager.getCache(nameCache).put(currency, cotacaoBcb); // Salva no cache
            } else {
                port += " - BCB Fallback"; // O BCB falhou, usaremos o valor original do BD local
            }
        }

        // Se conseguiu pegar a cotação do BCB, atualiza o multiplicador.
        if (cotacaoBcb != null) {
            currencyEntity.setConversionMultiplier(cotacaoBcb);
        }

        currencyEntity.setEnvironment(port);
        return ResponseEntity.ok(currencyEntity);
    }
}