package sk.tuke.gamestudio.server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

@Controller
public class MainController {

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        // Hlavna stranka aplikacie, odkial sa hrac dostane do hry a rebrickov.
        Object loggedPlayer = session.getAttribute(AuthController.SESSION_PLAYER);
        model.addAttribute("player", loggedPlayer == null ? "" : loggedPlayer.toString());
        model.addAttribute("loggedIn", loggedPlayer != null);
        return "main";
    }
}
