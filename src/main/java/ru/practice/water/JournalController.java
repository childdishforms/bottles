package ru.practice.water;

import jakarta.servlet.http.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class JournalController {
    private static final String DATABASE_ERROR = "База данных недоступна. Проверьте параметры подключения и наличие таблиц.";
    private final JournalStore store;
    public JournalController(JournalStore store) { this.store = store; }

    @GetMapping("/")
    String home(HttpSession session, Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        if (session.getAttribute("token") == null) session.setAttribute("token", UUID.randomUUID().toString());
        model.addAttribute("token", session.getAttribute("token"));
        if (session.getAttribute("user") == null) return "login";
        model.addAttribute("today", LocalDate.now());
        try {
            model.addAttribute("products", store.products());
            model.addAttribute("batches", store.batches());
        } catch (SQLException ex) {
            model.addAttribute("error", DATABASE_ERROR);
        }
        return "journal";
    }

    private void verify(HttpSession session, String token, boolean requireUser) {
        if (token == null || !token.equals(session.getAttribute("token")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (requireUser && session.getAttribute("user") == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }

    @PostMapping("/login")
    String login(@RequestParam String login, @RequestParam char[] password, @RequestParam String token,
                 HttpServletRequest request, RedirectAttributes flash) {
        var session = request.getSession();
        try {
            verify(session, token, false);
            if (login.length() <= 80 && password.length <= 256 && store.authenticate(login.strip(), password)) {
                request.changeSessionId();
                session.setAttribute("user", login.strip());
                session.setAttribute("token", UUID.randomUUID().toString());
            } else {
                flash.addFlashAttribute("error", "Неверный логин или пароль.");
            }
        } catch (SQLException ex) {
            flash.addFlashAttribute("error", DATABASE_ERROR);
        } finally {
            Arrays.fill(password, '\0');
        }
        return "redirect:/";
    }

    @PostMapping("/logout")
    String logout(HttpSession session, @RequestParam String token) {
        verify(session, token, true);
        session.invalidate();
        return "redirect:/";
    }

    @PostMapping("/batches")
    String create(HttpSession session, @RequestParam String token, @RequestParam String number,
                  @RequestParam LocalDate day, @RequestParam long product, @RequestParam int filled,
                  @RequestParam int rejected, RedirectAttributes flash) throws SQLException {
        verify(session, token, true);
        store.create(number, day, product, filled, rejected);
        return saved(flash, "Партия добавлена в журнал.");
    }

    @PostMapping("/batches/{id}/inspection")
    String inspect(HttpSession session, @RequestParam String token, @PathVariable long id,
                   @RequestParam boolean approved, @RequestParam(defaultValue="") String comment,
                   RedirectAttributes flash) throws SQLException {
        verify(session, token, true);
        store.inspect(id, approved, comment);
        return saved(flash, "Результат контроля сохранён.");
    }

    @PostMapping("/batches/{id}/receipt")
    String receive(HttpSession session, @RequestParam String token, @PathVariable long id,
                   RedirectAttributes flash) throws SQLException {
        verify(session, token, true);
        store.receive(id);
        return saved(flash, "Партия принята на склад.");
    }

    private String saved(RedirectAttributes flash, String text) {
        flash.addFlashAttribute("message", text);
        return "redirect:/";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    String validation(IllegalArgumentException ex, RedirectAttributes flash) {
        flash.addFlashAttribute("error", ex.getMessage());
        return "redirect:/";
    }

    @ExceptionHandler(SQLException.class)
    String unavailable(RedirectAttributes flash) {
        flash.addFlashAttribute("error", DATABASE_ERROR);
        return "redirect:/";
    }
}
