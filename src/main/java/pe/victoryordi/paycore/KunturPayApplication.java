package pe.victoryordi.paycore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class KunturPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(KunturPayApplication.class, args);
    }
}
