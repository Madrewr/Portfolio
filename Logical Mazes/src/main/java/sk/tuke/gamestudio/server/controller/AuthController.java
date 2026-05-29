package sk.tuke.gamestudio.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sk.tuke.gamestudio.entity.PlayerAccount;
import sk.tuke.gamestudio.service.AuthException;
import sk.tuke.gamestudio.service.AuthService;

import javax.servlet.http.HttpSession;

@Controller
public class AuthController {

    public static final String SESSION_PLAYER = "loggedPlayer";

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        // Auth service riesi databazu, controller riesi iba requesty a session.
        this.authService = authService;
    }

    @GetMapping("/login")
    public String loginForm(Model model) {
        // Jedna sablona auth.html sa pouziva pre prihlasenie aj registraciu.
        model.addAttribute("mode", "login");
        return "auth";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        // Pri registracii sa zmeni iba nadpis, text tlacidla a action formulara.
        model.addAttribute("mode", "register");
        return "auth";
    }

    @PostMapping("/login")
    public String login(@RequestParam("username") String username,
                        @RequestParam("password") String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        try {
            PlayerAccount account = authService.login(username, password);
            loginToSession(session, account);
            redirectAttributes.addFlashAttribute("message", "Prihlasenie prebehlo uspesne.");
            return "redirect:/logicalmazes";
        } catch (AuthException e) {
            redirectAttributes.addFlashAttribute("authMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("lastUsername", username);
            return "redirect:/login";
        }
    }

    @PostMapping("/register")
    public String register(@RequestParam("username") String username,
                           @RequestParam("password") String password,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        try {
            PlayerAccount account = authService.register(username, password);
            loginToSession(session, account);
            redirectAttributes.addFlashAttribute("message", "Registracia hotova. Si prihlaseny.");
            return "redirect:/logicalmazes";
        } catch (AuthException e) {
            redirectAttributes.addFlashAttribute("authMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("lastUsername", username);
            return "redirect:/register";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        // Odhlasenie len zmaze meno hraca zo session.
        session.removeAttribute(SESSION_PLAYER);
        redirectAttributes.addFlashAttribute("message", "Bol si odhlaseny.");
        return "redirect:/";
    }

    private void loginToSession(HttpSession session, PlayerAccount account) {
        // Do session staci ulozit username, cele heslo ani hash tam nepotrebujem.
        session.setAttribute(SESSION_PLAYER, account.getUsername());
    }
}
