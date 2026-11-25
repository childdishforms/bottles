package ru.practice.water;

import java.io.Console;
import java.util.Arrays;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WaterApplication {
    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--add-user")) {
            Console console = System.console();
            if (console == null) {
                System.err.println("Запустите создание пользователя в интерактивном терминале.");
                System.exit(1);
                return;
            }
            try {
                String login = console.readLine("Логин: ").strip();
                char[] password = console.readPassword("Пароль (ввод скрыт): ");
                try {
                    new JournalStore().addUser(login, password);
                } finally {
                    Arrays.fill(password, '\0');
                }
                console.printf("Пользователь создан.%n");
            } catch (Exception ex) {
                System.err.println("Проверьте настройки БД, наличие таблиц, уникальность и длину логина (1–80) и пароль (1–256 символов).");
                System.exit(1);
            }
            return;
        }
        SpringApplication.run(WaterApplication.class, args);
    }
}
