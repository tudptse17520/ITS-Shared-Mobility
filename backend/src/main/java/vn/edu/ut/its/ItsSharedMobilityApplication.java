package vn.edu.ut.its;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ItsSharedMobilityApplication {

	public static void main(String[] args) {
		SpringApplication.run(ItsSharedMobilityApplication.class, args);
	}

}
