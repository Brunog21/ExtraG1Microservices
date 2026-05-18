package br.edu.atitus.currencyservice.controllers;

import br.edu.atitus.currencyservice.dtos.CurrencyDTO;
import br.edu.atitus.currencyservice.entities.CurrencyEntity;
import br.edu.atitus.currencyservice.repositories.CurrencyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("currency")
public class CurrencyController {

    @Value("${server.port}")
    private String port;

    @Value("${convert.sleep:0}")
    private int sleep;

    private final CurrencyRepository repository;

    public CurrencyController(CurrencyRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/convert")
    public ResponseEntity<CurrencyDTO> getConvert(
            @RequestParam String source,
            @RequestParam String target) throws Exception {

        Thread.sleep(sleep);

        source = source.toUpperCase();
        target = target.toUpperCase();

        CurrencyEntity currency = repository.findBySourceCurrencyAndTargetCurrency(source, target)
                .orElseThrow(() -> new Exception("Currency not found"));

        String environment = "Currency-service running on port: " + port;

        CurrencyDTO dto = new CurrencyDTO(currency.getSourceCurrency(),
                currency.getTargetCurrency(),
                currency.getConversionRate(),
                environment);

        return ResponseEntity.ok(dto);
        //return ResponseEntity.badRequest().body(dto);
        //return ResponseEntity.internalServerError().body(dto);
    }
}
