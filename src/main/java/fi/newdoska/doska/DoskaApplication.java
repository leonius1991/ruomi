package fi.newdoska.doska;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DoskaApplication {

	public static void main(String[] args) {
		SpringApplication.run(DoskaApplication.class, args);
	}

}
